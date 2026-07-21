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
INTERNAL_API_KEY = os.getenv("ML_SERVICE_API_KEY", "dev_secret_key_123")

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
    # Budgets are defined in LKR
    cluster_profiles = {
        0: {"name": "Starting Out (Young, Low Income)", "budget": 25000.0, "savings": 5000.0},
        1: {"name": "Established Professional (Mid Age, High Income)", "budget": 150000.0, "savings": 50000.0},
        2: {"name": "Frugal Saver (Mixed Age, Mid Income, High Goal)", "budget": 80000.0, "savings": 40000.0},
        3: {"name": "High Spender (Mixed Age, High Income, Low Goal)", "budget": 250000.0, "savings": 10000.0},
    }
    print("Loaded pre-trained K-Means Cold Start model from disk.")
except Exception as e:
    print(f"Failed to load K-Means model: {e}. Please run train_kmeans.py first.")
    kmeans_model = None


# --- Data Models ---
class ChatRequest(BaseModel):
    user_id: str
    message: str
    context: str

class ForecastRequest(BaseModel):
    user_id: str
    historical_data: List[float] # List of past monthly expense totals

class CategorizeRequest(BaseModel):
    description: str

class ColdStartRequest(BaseModel):
    user_id: str
    age: int
    income_bracket: str # e.g. "50000"
    savings_goal: float

# --- Endpoints ---

@app.post("/api/ml/chat", dependencies=[Depends(verify_api_key)])
async def chat_endpoint(req: ChatRequest):
    return {
        "reply": f"Based on your query '{req.message}', and your context: {req.context}. I recommend reviewing your Food budget and transferring $50 to savings.",
        "model_used": "mock-llm-v1"
    }

@app.post("/api/ml/forecast", dependencies=[Depends(verify_api_key)])
async def forecast_endpoint(req: ForecastRequest):
    non_zero_data = [x for x in req.historical_data if x > 0]
    
    if len(non_zero_data) == 0:
        return {"predicted_next_month_expense": 0.0, "confidence_score": 0.0}
    
    if chronos_pipeline is None or len(non_zero_data) < 2:
        avg_expense = sum(non_zero_data) / len(non_zero_data)
        return {"predicted_next_month_expense": round(avg_expense, 2), "confidence_score": 0.5}
    
    context_tensor = torch.tensor(req.historical_data)
    forecast = chronos_pipeline.predict(context_tensor, prediction_length=1)
    predicted_value = float(forecast[0].median().item())
    
    if predicted_value < 1.0 and len(non_zero_data) > 0:
        predicted_value = sum(non_zero_data) / len(non_zero_data)
    
    return {
        "predicted_next_month_expense": max(0.0, round(predicted_value, 2)),
        "confidence_score": 0.85
    }

import requests

@app.post("/api/ml/categorize", dependencies=[Depends(verify_api_key)])
async def auto_categorize(req: CategorizeRequest):
    API_URL = "https://api-inference.huggingface.co/models/samudu123/srilanka-transaction-classifier"
    
    HF_TOKEN = os.getenv("HF_API_TOKEN", "")
    headers = {}
    if HF_TOKEN:
        headers["Authorization"] = f"Bearer {HF_TOKEN}"
        
    try:
        import time
        max_retries = 3
        best_category = "other"
        confidence = 0.0
        
        for attempt in range(max_retries):
            response = requests.post(API_URL, headers=headers, json={"inputs": req.description})
            result = response.json()
            
            # Check if model is loading
            if isinstance(result, dict) and "estimated_time" in result:
                wait_time = min(result["estimated_time"], 20.0) # Wait up to 20s per attempt
                print(f"HF Model loading, waiting {wait_time}s... (Attempt {attempt+1}/{max_retries})")
                time.sleep(wait_time)
                continue
            
            # Parse successful inference API response
            if isinstance(result, list) and len(result) > 0:
                if isinstance(result[0], list):
                    best_pred = result[0][0]
                else:
                    best_pred = result[0]
                    
                best_category = best_pred.get("label", "other").lower()
                confidence = best_pred.get("score", 0.5)
            break
            
    except Exception as e:
        print("HF API Error:", e)
        best_category = "other"
        confidence = 0.0
    
    if "label" in best_category:
        best_category = best_category.split('_')[-1]
        
    category_mapping = {
        "food": "Food & Dining",
        "transport": "Transportation",
        "utilities": "Mobile & Internet",
        "health": "Healthcare",
        "education": "Education",
        "entertainment": "Entertainment",
        "shopping": "Shopping",
        "groceries": "Groceries",
        "fuel": "Fuel"
    }
    
    mapped_category = category_mapping.get(best_category, best_category.capitalize())
    
    return {
        "category": mapped_category,
        "confidence": float(confidence)
    }

@app.post("/api/ml/cold_start", dependencies=[Depends(verify_api_key)])
async def cold_start_profile(req: ColdStartRequest):
    try:
        income_k = float(req.income_bracket) / 1000.0
        savings_k = float(req.savings_goal) / 1000.0
    except ValueError:
        income_k = 50.0
        savings_k = 5.0

    features = np.array([[req.age, income_k, savings_k]])
    cluster_id = int(kmeans_model.predict(features)[0])
    profile = cluster_profiles.get(cluster_id, cluster_profiles[0])
    
    return {
        "assigned_cluster": profile["name"],
        "recommended_monthly_budget": profile["budget"],
        "recommended_savings_goal": profile["savings"]
    }

