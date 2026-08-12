import pandas as pd
import numpy as np
from sklearn.cluster import KMeans
from sklearn.preprocessing import StandardScaler
import joblib
import os

# Ensure models directory exists
os.makedirs("models", exist_ok=True)

print("Generating Sri Lankan synthetic dataset (5 features)...")
np.random.seed(42)

N_SAMPLES = 1000

# ── Feature 1: Age — Uniform [18, 68] ──
ages = np.random.uniform(18, 68, N_SAMPLES)

# ── Feature 2: Income — Lognormal, median ~70k LKR, clamped [30k, 500k] ──
incomes = np.random.lognormal(mean=4.25, sigma=0.6, size=N_SAMPLES)
incomes = np.clip(incomes, 30.0, 500.0)

# ── Feature 3: Savings Goal — 10-30% of income ──
savings_percentages = np.random.uniform(0.10, 0.30, N_SAMPLES)
savings = incomes * savings_percentages

# ── Feature 4: Spending Style (1-5) ──
# 1=Very Frugal, 2=Careful, 3=Moderate, 4=Generous, 5=Spender
# Correlated with income percentile: higher income → higher spending tendency
income_percentile = np.argsort(np.argsort(incomes)) / N_SAMPLES  # 0-1 rank-based
spending_style = np.zeros(N_SAMPLES, dtype=int)
for i in range(N_SAMPLES):
    p = income_percentile[i]  # 0 = lowest income, 1 = highest
    # Weighted probabilities shift toward higher spending as income increases
    weights = [
        0.35 - 0.25 * p,   # P(1) = Very Frugal: 35% at low income → 10% at high
        0.30 - 0.10 * p,   # P(2) = Careful:     30% → 20%
        0.20,               # P(3) = Moderate:    constant 20%
        0.10 + 0.15 * p,   # P(4) = Generous:    10% → 25%
        0.05 + 0.20 * p,   # P(5) = Spender:     5%  → 25%
    ]
    weights = np.array(weights)
    weights = weights / weights.sum()
    spending_style[i] = np.random.choice([1, 2, 3, 4, 5], p=weights)

# ── Feature 5: Risk Tolerance (1-5) ──
# 1=Very Conservative, 2=Conservative, 3=Moderate, 4=Aggressive, 5=Very Aggressive
# Correlated with age percentile: younger → higher risk appetite
age_percentile = np.argsort(np.argsort(ages)) / N_SAMPLES  # 0 = youngest, 1 = oldest
risk_tolerance = np.zeros(N_SAMPLES, dtype=int)
for i in range(N_SAMPLES):
    p = age_percentile[i]  # 0 = youngest, 1 = oldest
    # Younger people tend toward higher risk tolerance
    weights = [
        0.05 + 0.25 * p,   # P(1) = Very Conservative: 5%  → 30% as age increases
        0.10 + 0.15 * p,   # P(2) = Conservative:      10% → 25%
        0.20,               # P(3) = Moderate:          constant 20%
        0.30 - 0.10 * p,   # P(4) = Aggressive:        30% → 20%
        0.35 - 0.30 * p,   # P(5) = Very Aggressive:   35% → 5%
    ]
    weights = np.array(weights)
    weights = weights / weights.sum()
    risk_tolerance[i] = np.random.choice([1, 2, 3, 4, 5], p=weights)

# Combine all 5 features into synthetic data array
synthetic_data = np.column_stack((ages, incomes, savings, spending_style, risk_tolerance))

print(f"\n--- Dataset Summary ({N_SAMPLES} samples, 5 features) ---")
print(f"{'Feature':<20} {'Mean':>8} {'Median':>8} {'Min':>8} {'Max':>8}")
print("-" * 56)
feature_names = ["Age", "Income (k LKR)", "Savings (k LKR)", "Spending Style", "Risk Tolerance"]
for i, name in enumerate(feature_names):
    col = synthetic_data[:, i]
    print(f"{name:<20} {np.mean(col):>8.2f} {np.median(col):>8.2f} {np.min(col):>8.2f} {np.max(col):>8.2f}")
print("-" * 56)

print("\nFitting StandardScaler to 5-feature synthetic data...")
scaler = StandardScaler()
scaled_synthetic_data = scaler.fit_transform(synthetic_data)

print("Training K-Means Model (K=4) on scaled 5-feature data...")
kmeans_model = KMeans(n_clusters=4, random_state=42, n_init=10)
kmeans_model.fit(scaled_synthetic_data)

# Deterministic Cluster Mapping (Sort by Income ascending — index 1)
income_centers = kmeans_model.cluster_centers_[:, 1]
sorted_cluster_ids = np.argsort(income_centers)
label_map = {int(original): int(new) for new, original in enumerate(sorted_cluster_ids)}

# Save the trained model, scaler, and label map to disk
model_path = "models/kmeans_cold_start.pkl"
scaler_path = "models/kmeans_scaler.pkl"
label_map_path = "models/kmeans_label_map.pkl"

joblib.dump(kmeans_model, model_path)
joblib.dump(scaler, scaler_path)
joblib.dump(label_map, label_map_path)

print(f"\nSuccessfully saved trained model to {model_path}")
print(f"Successfully saved scaler to {scaler_path}")
print(f"Successfully saved label_map to {label_map_path}")

print("\nLabel Remapping (Original ID -> New Deterministic ID):")
print(label_map)

# Display the cluster centers to verify the new ordering
real_world_centers = scaler.inverse_transform(kmeans_model.cluster_centers_)

print(f"\nFinal Ordered Cluster Centers (Unscaled):")
print(f"{'Cluster':<10} {'Age':>6} {'Income(k)':>10} {'Savings(k)':>11} {'SpendStyle':>11} {'RiskTol':>8}")
print("-" * 60)
for new_id in range(4):
    original_id = sorted_cluster_ids[new_id]
    c = real_world_centers[original_id]
    print(f"{new_id:<10} {c[0]:>6.1f} {c[1]:>10.1f} {c[2]:>11.1f} {c[3]:>11.1f} {c[4]:>8.1f}")
print("-" * 60)
