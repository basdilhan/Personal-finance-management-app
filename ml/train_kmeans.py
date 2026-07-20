import pandas as pd
import numpy as np
from sklearn.cluster import KMeans
import joblib
import os

# Ensure models directory exists
os.makedirs("models", exist_ok=True)

print("Generating Sri Lankan synthetic dataset...")
np.random.seed(42)

# Features: [age, income_k (in Thousands of LKR), savings_goal_k (in Thousands of LKR)]
# Age: 18 to 68 (range 50)
# Monthly Income: 30k LKR to 500k LKR (range 470)
# Monthly Savings Goal: 5k LKR to 100k LKR (range 95)
synthetic_data = np.random.rand(1000, 3) * [50, 470, 95] + [18, 30, 5] 

print("Training K-Means Model (K=4)...")
kmeans_model = KMeans(n_clusters=4, random_state=42)
kmeans_model.fit(synthetic_data)

# Save the trained model to disk
model_path = "models/kmeans_cold_start.pkl"
joblib.dump(kmeans_model, model_path)
print(f"Successfully saved trained model to {model_path}")

# Display the cluster centers to verify
print("\nCluster Centers [Age, Income (k LKR), Savings Goal (k LKR)]:")
print(kmeans_model.cluster_centers_)
