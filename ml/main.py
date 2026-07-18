import os
from fastapi import FastAPI, Depends, HTTPException, Header
from pydantic import BaseModel
from typing import List
import pandas as pd
import numpy as np
from sklearn.cluster import KMeans
from transformers import pipeline
from chronos import ChronosPipeline
import torch

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

# 1. NLP Categorization Model (Fine-Tuned)
try:
    # IMPORTANT: Once you run `colab_finetune_classifier.py` and push to HuggingFace, 
    # replace "cross-encoder/nli-distilroberta-base" with your model name!
    # Load the user's custom fine-tuned model for Sri Lankan transactions
    classifier = pipeline("text-classification", model="samudu123/srilanka-transaction-classifier")
    print("Loaded Custom NLP Categorization model.")
except Exception as e:
    print(f"Failed to load NLP model: {e}")
    classifier = None

# 2. Time-Series Forecasting Model (Chronos T5-Mini)
try:
    chronos_pipeline = ChronosPipeline.from_pretrained(
        "amazon/chronos-t5-mini",
        device_map="cpu",  # Use CPU for compatibility
        torch_dtype=torch.float32,
    )
    print("Loaded Chronos Time-Series model.")
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
    # For the chatbot, we'll keep it simple for now, mocking a smart LLM response.
    # In production, this would use a generative model like LLaMA-2 or OpenAI API.
    return {
        "reply": f"Based on your query '{req.message}', and your context: {req.context}. I recommend reviewing your Food budget and transferring $50 to savings.",
        "model_used": "mock-llm-v1"
    }

@app.post("/api/ml/forecast", dependencies=[Depends(verify_api_key)])
async def forecast_endpoint(req: ForecastRequest):
    if chronos_pipeline is None or len(req.historical_data) < 2:
        return {"predicted_next_month_expense": 0.0, "confidence_score": 0.0}
    
    # Run Chronos forecasting
    context_tensor = torch.tensor(req.historical_data)
    forecast = chronos_pipeline.predict(context_tensor, prediction_length=1)
    predicted_value = float(forecast[0].median().item())
    
    return {
        "predicted_next_month_expense": max(0.0, predicted_value),
        "confidence_score": 0.85
    }

@app.post("/api/ml/categorize", dependencies=[Depends(verify_api_key)])
async def auto_categorize(req: CategorizeRequest):
    if classifier is None:
        return {"category": "Other", "confidence": 0.0}
    
    # Run the custom fine-tuned text classification model
    result = classifier(req.description)[0]
    
    best_category = result["label"]
    
    # If the model outputs LABEL_X, we can clean it, but normally id2label handles it
    if "LABEL" in best_category:
        best_category = best_category.split('_')[-1]
        
    confidence = result["score"]
    
    return {
        "category": best_category.capitalize(),
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

