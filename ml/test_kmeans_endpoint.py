import os

# Ensure API key is set for testing
os.environ["ML_SERVICE_API_KEY"] = "test_key"

from fastapi.testclient import TestClient
from main import app, cluster_profiles
import main
import main
main.INTERNAL_API_KEY = "test_key"

client = TestClient(app)

profiles = [
    {"age": 22, "income_bracket": "35000", "savings_goal": 5000.0, "desc": "young, low income"},
    {"age": 45, "income_bracket": "250000", "savings_goal": 50000.0, "desc": "mid-career, high income"},
    {"age": 35, "income_bracket": "80000", "savings_goal": 15000.0, "desc": "average Sri Lankan professional"}
]

for p in profiles:
    req_data = {
        "user_id": "test_user",
        "age": p["age"],
        "income_bracket": p["income_bracket"],
        "savings_goal": p["savings_goal"]
    }
    
    response = client.post("/api/ml/cold_start", json=req_data, headers={"x-api-key": "test_key"})
    
    if response.status_code == 200:
        data = response.json()
        
        # Find which cluster ID corresponds to the assigned name
        cluster_id = -1
        for cid, profile in cluster_profiles.items():
            if profile["name"] == data["assigned_cluster"]:
                cluster_id = cid
                break
                
        print(f"Profile {p['desc'].upper()}:")
        print(f"  Age: {p['age']}, Income: {p['income_bracket']}, Savings: {p['savings_goal']}")
        print(f"  -> Assigned Cluster ID: {cluster_id}")
        print(f"  -> Cluster Name: {data['assigned_cluster']}")
        print(f"  -> Recommended Budget: {data['recommended_monthly_budget']} LKR")
        print(f"  -> Recommended Savings: {data['recommended_savings_goal']} LKR\n")
    else:
        print(f"Error testing profile {p['desc']}: {response.text}")
