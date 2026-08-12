import os
# Limit threads to reduce memory usage (Fixes OOM Status 137 on Render 512MB)
os.environ["OMP_NUM_THREADS"] = "1"
os.environ["MKL_NUM_THREADS"] = "1"
os.environ["OPENBLAS_NUM_THREADS"] = "1"

from fastapi import FastAPI, Depends, HTTPException, Header
from pydantic import BaseModel
from typing import List
import pandas as pd
import numpy as np
from sklearn.cluster import KMeans
from transformers import pipeline
import torch
torch.set_num_threads(1)
try:
    from chronos import ChronosPipeline
except ImportError:
    ChronosPipeline = None
    print("WARNING: chronos package not installed. Forecasting will use fallback.")

# --- Security Setup ---
INTERNAL_API_KEY = os.getenv("ML_SERVICE_API_KEY")
if not INTERNAL_API_KEY:
    raise RuntimeError("Missing ML_SERVICE_API_KEY")

def verify_api_key(x_api_key: str = Header(None)):
    if x_api_key != INTERNAL_API_KEY:
        raise HTTPException(status_code=403, detail="Invalid API Key. Direct access forbidden.")
    return x_api_key

app = FastAPI(
    title="DreamSaver AI Pipeline",
    description="Internal ML service for Expense Categorization and Clustering"
)

@app.get("/health")
async def health_check():
    return {"status": "healthy", "model": "dreamsaver-ai"}

# --- Global Model Initialization ---
import joblib

# 1. NLP Categorization Model (Offloaded to API to save RAM)
# We will use HuggingFace Inference API in the endpoint instead of loading it into RAM.

# 2. Time-Series Forecasting Model (Chronos T5-Tiny to save RAM)
try:
    if ChronosPipeline is not None:
        chronos_pipeline = ChronosPipeline.from_pretrained(
            "amazon/chronos-t5-tiny", # Switched to tiny version (fits in 512MB)
            device_map="cpu",
            torch_dtype=torch.float32,
        )
        print("Loaded Chronos Time-Series model (Tiny).")
    else:
        chronos_pipeline = None
        print("Chronos not available, forecasting will use simple average fallback.")
except Exception as e:
    print(f"Failed to load Chronos model: {e}")
    chronos_pipeline = None

# 3. K-Means Clustering for Cold Start
try:
    kmeans_model = joblib.load("models/kmeans_cold_start.pkl")
    scaler = joblib.load("models/kmeans_scaler.pkl")
    label_map = joblib.load("models/kmeans_label_map.pkl")
    # Budgets are defined as percentages of their total income
    cluster_profiles = {
        0: {"name": "Conservative Budget (Lowest Income)", "budget_pct": 0.85, "savings_pct": 0.15},
        1: {"name": "Balanced Budget (Lower-Mid Income)", "budget_pct": 0.75, "savings_pct": 0.25},
        2: {"name": "Growth Focused (Upper-Mid Income)", "budget_pct": 0.65, "savings_pct": 0.35},
        3: {"name": "Aggressive Savings (Highest Income)", "budget_pct": 0.50, "savings_pct": 0.50},
    }
    print("Loaded pre-trained K-Means Cold Start model from disk.")
except Exception as e:
    print(f"Failed to load K-Means model, scaler, or label_map: {e}. Please run train_kmeans.py first.")
    kmeans_model = None
    scaler = None
    label_map = None


# --- Data Models ---
class ForecastRequest(BaseModel):
    user_id: str
    historical_data: List[float] # List of past monthly expense totals

class ColdStartRequest(BaseModel):
    user_id: str
    age: int
    income_bracket: str # e.g. "50000"
    savings_goal: float
    spending_style: int = 3  # 1=Very Frugal, 2=Careful, 3=Moderate, 4=Generous, 5=Spender
    risk_tolerance: int = 3  # 1=Very Conservative, 2=Conservative, 3=Moderate, 4=Aggressive, 5=Very Aggressive

# --- Endpoints ---

@app.post("/api/ml/forecast", dependencies=[Depends(verify_api_key)])
async def forecast_endpoint(req: ForecastRequest):
    import datetime
    from fastapi.responses import JSONResponse
    try:
        non_zero_data = [x for x in req.historical_data if x > 0]
        
        if len(non_zero_data) == 0:
            # Mathematical baseline for new users
            return {"predicted_next_month_expense": 15000.0, "confidence_score": 0.3}
        
        if chronos_pipeline is None or len(non_zero_data) < 2:
            # Mathematical fallback: average of available month data + 5% buffer
            avg_expense = sum(non_zero_data) / len(non_zero_data)
            forecast_val = round(avg_expense * 1.05, 2)
            return {"predicted_next_month_expense": forecast_val, "confidence_score": 0.5}
        
        context_tensor = torch.tensor(non_zero_data)
        torch.manual_seed(42) # Force probabilistic model to generate deterministic results for the same input
        forecast = chronos_pipeline.predict(context_tensor, prediction_length=1)
        predicted_value = float(forecast[0].median().item())
        
        if predicted_value < 1.0 and len(non_zero_data) > 0:
            avg_expense = sum(non_zero_data) / len(non_zero_data)
            predicted_value = avg_expense * 1.05
        
        return {
            "predicted_next_month_expense": max(0.0, round(predicted_value, 2)),
            "confidence_score": 0.85
        }
    except Exception as e:
        timestamp = datetime.datetime.now().isoformat()
        print(f"[{timestamp}] Error in forecast_endpoint: {str(e)}")
        return JSONResponse(status_code=500, content={"message": "Internal model error during forecasting", "error": str(e)})


@app.post("/api/ml/cold_start", dependencies=[Depends(verify_api_key)])
async def cold_start_profile(req: ColdStartRequest):
    try:
        income_k = float(req.income_bracket) / 1000.0
        savings_k = float(req.savings_goal) / 1000.0
        actual_income = float(req.income_bracket)
    except ValueError:
        income_k = 50.0
        savings_k = 5.0
        actual_income = 50000.0

    # Clamp behavioural features to valid 1-5 range
    spending_style = max(1, min(5, req.spending_style))
    risk_tolerance = max(1, min(5, req.risk_tolerance))

    if kmeans_model is not None and scaler is not None and label_map is not None:
        # 5-feature vector: [age, income_k, savings_k, spending_style, risk_tolerance]
        features = np.array([[req.age, income_k, savings_k, spending_style, risk_tolerance]])
        scaled_features = scaler.transform(features)
        raw_cluster_id = int(kmeans_model.predict(scaled_features)[0])
        cluster_id = label_map[raw_cluster_id]
    else:
        cluster_id = 0 # Fallback if model not loaded

    profile = cluster_profiles.get(cluster_id, cluster_profiles[0])

    return {
        "assigned_cluster": profile["name"],
        "recommended_monthly_budget": round(actual_income * profile["budget_pct"], 2),
        "recommended_savings_goal": round(actual_income * profile["savings_pct"], 2),
        "features_used": ["age", "income", "savings_goal", "spending_style", "risk_tolerance"]
    }

