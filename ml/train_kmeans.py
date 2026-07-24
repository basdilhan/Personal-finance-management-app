import pandas as pd
import numpy as np
from sklearn.cluster import KMeans
from sklearn.preprocessing import StandardScaler
import joblib
import os

# Ensure models directory exists
os.makedirs("models", exist_ok=True)

print("Generating Sri Lankan synthetic dataset...")
np.random.seed(42)

# Generate Age uniformly between 18 and 68
ages = np.random.uniform(18, 68, 1000)

# Generate Income using a log-normal distribution to reflect a right-skewed realistic distribution
# Median around ~70k. Lognormal params: mean ~ 4.25 (e^4.25 = ~70), sigma ~ 0.6
incomes = np.random.lognormal(mean=4.25, sigma=0.6, size=1000)
# Clamp incomes between 30k and 500k to prevent extreme outliers
incomes = np.clip(incomes, 30.0, 500.0)

# Generate Savings Goal roughly proportional to income (10% to 30% of income)
savings_percentages = np.random.uniform(0.10, 0.30, 1000)
savings = incomes * savings_percentages

# Combine into synthetic data array
synthetic_data = np.column_stack((ages, incomes, savings))

print("\n--- Income Distribution Summary (k LKR) ---")
print(f"Mean Income:   {np.mean(incomes):.2f}")
print(f"Median Income: {np.median(incomes):.2f}")
print(f"Min Income:    {np.min(incomes):.2f}")
print(f"Max Income:    {np.max(incomes):.2f}")
print("-------------------------------------------\n")

print("Fitting StandardScaler to synthetic data...")
scaler = StandardScaler()
scaled_synthetic_data = scaler.fit_transform(synthetic_data)

print("Training K-Means Model (K=4) on scaled data...")
kmeans_model = KMeans(n_clusters=4, random_state=42)
kmeans_model.fit(scaled_synthetic_data)

# Deterministic Cluster Mapping (Sort by Income ascending)
# Get the income column of cluster centers (in scaled space, index 1)
income_centers = kmeans_model.cluster_centers_[:, 1]
# Sort cluster IDs by income ascending
sorted_cluster_ids = np.argsort(income_centers)
# Create a mapping: original_label -> new_sorted_label
label_map = {int(original): int(new) for new, original in enumerate(sorted_cluster_ids)}

# Save the trained model, scaler, and label map to disk
model_path = "models/kmeans_cold_start.pkl"
scaler_path = "models/kmeans_scaler.pkl"
label_map_path = "models/kmeans_label_map.pkl"

joblib.dump(kmeans_model, model_path)
joblib.dump(scaler, scaler_path)
joblib.dump(label_map, label_map_path)

print(f"Successfully saved trained model to {model_path}")
print(f"Successfully saved scaler to {scaler_path}")
print(f"Successfully saved label_map to {label_map_path}")

print("\nLabel Remapping (Original ID -> New Deterministic ID):")
print(label_map)

# Display the cluster centers to verify the new ordering
real_world_centers = scaler.inverse_transform(kmeans_model.cluster_centers_)

print("\nFinal Ordered Cluster Centers [Age, Income (k LKR), Savings Goal (k LKR)] (Unscaled):")
# We iterate 0 to 3, find the original cluster ID that maps to it, and print that center
for new_id in range(4):
    original_id = sorted_cluster_ids[new_id]
    print(f"Cluster {new_id}: {real_world_centers[original_id]}")
