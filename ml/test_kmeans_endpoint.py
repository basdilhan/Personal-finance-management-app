import os

# Ensure API key is set for testing
os.environ["ML_SERVICE_API_KEY"] = "test_key"

from fastapi.testclient import TestClient
from main import app, cluster_profiles
import main
main.INTERNAL_API_KEY = "test_key"

client = TestClient(app)

print("=" * 70)
print("  K-Means Cold Start Endpoint Test (5-Feature Model)")
print("=" * 70)
print("  Features: age, income, savings_goal, spending_style, risk_tolerance")
print()

profiles = [
    # Basic profiles across income ranges
    {"age": 22, "income_bracket": "35000", "savings_goal": 5000.0,
     "spending_style": 4, "risk_tolerance": 4,
     "desc": "Young spender, high risk tolerance"},
    {"age": 22, "income_bracket": "35000", "savings_goal": 5000.0,
     "spending_style": 1, "risk_tolerance": 2,
     "desc": "Young frugal, low risk tolerance"},
    {"age": 45, "income_bracket": "250000", "savings_goal": 50000.0,
     "spending_style": 3, "risk_tolerance": 2,
     "desc": "Mid-career high income, moderate spender, conservative"},
    {"age": 45, "income_bracket": "250000", "savings_goal": 50000.0,
     "spending_style": 5, "risk_tolerance": 5,
     "desc": "Mid-career high income, big spender, aggressive risk"},
    {"age": 35, "income_bracket": "80000", "savings_goal": 15000.0,
     "spending_style": 3, "risk_tolerance": 3,
     "desc": "Average Sri Lankan professional, moderate everything"},
    {"age": 60, "income_bracket": "60000", "savings_goal": 8000.0,
     "spending_style": 2, "risk_tolerance": 1,
     "desc": "Senior, low income, careful & very conservative"},
]

print("--- Testing: Same income but DIFFERENT spending/risk ---")
print("  (Profiles 1 vs 2 show how behavioural features change the cluster)\n")

for i, p in enumerate(profiles):
    req_data = {
        "user_id": "test_user",
        "age": p["age"],
        "income_bracket": p["income_bracket"],
        "savings_goal": p["savings_goal"],
        "spending_style": p["spending_style"],
        "risk_tolerance": p["risk_tolerance"],
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
                
        print(f"Profile {i+1}: {p['desc'].upper()}")
        print(f"  Age: {p['age']}, Income: {p['income_bracket']}, Savings: {p['savings_goal']}")
        print(f"  Spending Style: {p['spending_style']}, Risk Tolerance: {p['risk_tolerance']}")
        print(f"  -> Assigned Cluster ID: {cluster_id}")
        print(f"  -> Cluster Name: {data['assigned_cluster']}")
        print(f"  -> Recommended Budget: {data['recommended_monthly_budget']} LKR")
        print(f"  -> Recommended Savings: {data['recommended_savings_goal']} LKR")
        print(f"  -> Features Used: {data.get('features_used', 'N/A')}")
        print()
    else:
        print(f"Error testing profile {p['desc']}: {response.text}")

# Test backward compatibility — missing spending_style/risk_tolerance should default to 3
print("--- Testing: Backward Compatibility (missing new fields) ---\n")
req_data = {
    "user_id": "test_user",
    "age": 30,
    "income_bracket": "75000",
    "savings_goal": 12000.0,
    # spending_style and risk_tolerance intentionally omitted
}

response = client.post("/api/ml/cold_start", json=req_data, headers={"x-api-key": "test_key"})
if response.status_code == 200:
    data = response.json()
    print(f"  Backward compatibility test: PASSED (status 200)")
    print(f"  Response: {data}")
else:
    print(f"  Backward compatibility test: FAILED (status {response.status_code})")
    print(f"  Response: {response.text}")

print("\n" + "=" * 70)
print("  All tests completed.")
print("=" * 70)
