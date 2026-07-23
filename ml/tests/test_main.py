import pytest
from fastapi.testclient import TestClient
from main import app

client = TestClient(app)
API_KEY = "dev_secret_key_123"
HEADERS = {"x-api-key": API_KEY}

def test_health_check():
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "healthy", "model": "dreamsaver-ai"}

def test_missing_api_key():
    response = client.post("/api/ml/forecast", json={
        "user_id": "test",
        "historical_data": [100.0]
    })
    assert response.status_code == 403

def test_forecast_fallback_empty_data():
    response = client.post("/api/ml/forecast", headers=HEADERS, json={
        "user_id": "user1",
        "historical_data": []
    })
    assert response.status_code == 200
    data = response.json()
    assert "predicted_next_month_expense" in data
    assert data["predicted_next_month_expense"] == 15000.0

def test_forecast_fallback_insufficient_data():
    # Only 1 data point should trigger mathematical fallback
    response = client.post("/api/ml/forecast", headers=HEADERS, json={
        "user_id": "user1",
        "historical_data": [1000.0]
    })
    assert response.status_code == 200
    data = response.json()
    assert "predicted_next_month_expense" in data
    assert data["predicted_next_month_expense"] == 1050.0  # 1000 * 1.05

def test_forecast_zeros_only():
    response = client.post("/api/ml/forecast", headers=HEADERS, json={
        "user_id": "user1",
        "historical_data": [0.0, 0.0, 0.0]
    })
    assert response.status_code == 200
    data = response.json()
    assert data["predicted_next_month_expense"] == 15000.0

def test_chat_endpoint():
    response = client.post("/api/ml/chat", headers=HEADERS, json={
        "user_id": "user1",
        "message": "Hello",
        "context": "Context"
    })
    assert response.status_code == 200
    assert "reply" in response.json()

def test_cold_start_fallback_invalid_types():
    response = client.post("/api/ml/cold_start", headers=HEADERS, json={
        "user_id": "user1",
        "age": 25,
        "income_bracket": "NotANumber",
        "savings_goal": 5000.0
    })
    assert response.status_code == 200
    data = response.json()
    assert "assigned_cluster" in data
    # When income is not a number, fallback is to 50k and uses cluster 0 logic, 
    # but actual_income will throw a ValueError in the real code if not caught carefully!
    # Wait, the current code has a bug: `actual_income = float(req.income_bracket)` happens outside the try-except!
    # Let's see if this test catches it.
