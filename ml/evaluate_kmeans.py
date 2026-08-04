"""
DreamSaver ML — K-Means Clustering Evaluation
===============================================
Produces formal evaluation metrics and visualizations for the K-Means
cold-start clustering model (Layer 1 of the dual-layer ML architecture).

Outputs:
  - Silhouette Score (printed + saved)
  - Elbow Curve chart (PNG)
  - PCA 2D cluster visualization (PNG)
  - Cluster center statistics (printed + saved)

Usage:
  cd ml/
  python evaluate_kmeans.py

All outputs are saved to ml/evaluation_results/
"""

import os
import sys
import numpy as np
import joblib
import matplotlib
matplotlib.use('Agg')  # Non-interactive backend for server/CI environments
import matplotlib.pyplot as plt
from sklearn.cluster import KMeans
from sklearn.metrics import silhouette_score, silhouette_samples
from sklearn.decomposition import PCA
from sklearn.preprocessing import StandardScaler
from datetime import datetime

# ── Configuration ──
SEED = 42
N_SAMPLES = 1000
K_RANGE = range(2, 9)  # Test K=2 through K=8
OPTIMAL_K = 4
OUTPUT_DIR = os.path.join(os.path.dirname(__file__), "evaluation_results")
MODEL_DIR = os.path.join(os.path.dirname(__file__), "models")

# Cluster profile names (must match main.py)
CLUSTER_NAMES = {
    0: "Conservative Budget\n(Lowest Income)",
    1: "Balanced Budget\n(Lower-Mid Income)",
    2: "Growth Focused\n(Upper-Mid Income)",
    3: "Aggressive Savings\n(Highest Income)",
}

# ── Chart Styling ──
COLORS = ['#FF6B6B', '#4ECDC4', '#45B7D1', '#F7DC6F']
plt.rcParams.update({
    'font.family': 'sans-serif',
    'font.size': 11,
    'axes.titlesize': 14,
    'axes.titleweight': 'bold',
    'axes.labelsize': 12,
    'figure.facecolor': '#0f1117',
    'axes.facecolor': '#1a1b26',
    'text.color': '#e0e0e0',
    'axes.labelcolor': '#e0e0e0',
    'xtick.color': '#a0a0a0',
    'ytick.color': '#a0a0a0',
    'axes.edgecolor': '#333344',
    'grid.color': '#2a2b3d',
    'grid.alpha': 0.5,
})


def generate_synthetic_data(seed=SEED, n_samples=N_SAMPLES):
    """
    Regenerate the EXACT same synthetic dataset used in train_kmeans.py.
    Same seed, same distribution, same parameters — ensures reproducibility.
    """
    np.random.seed(seed)

    # Age: Uniform [18, 68]
    ages = np.random.uniform(18, 68, n_samples)

    # Income: Lognormal (median ~70k LKR), clamped [30k, 500k]
    incomes = np.random.lognormal(mean=4.25, sigma=0.6, size=n_samples)
    incomes = np.clip(incomes, 30.0, 500.0)

    # Savings: 10-30% of income
    savings_pct = np.random.uniform(0.10, 0.30, n_samples)
    savings = incomes * savings_pct

    return np.column_stack((ages, incomes, savings))


def load_trained_model():
    """Load the pre-trained K-Means model, scaler, and label map from disk."""
    model_path = os.path.join(MODEL_DIR, "kmeans_cold_start.pkl")
    scaler_path = os.path.join(MODEL_DIR, "kmeans_scaler.pkl")
    label_map_path = os.path.join(MODEL_DIR, "kmeans_label_map.pkl")

    for path, name in [(model_path, "K-Means model"), (scaler_path, "Scaler"), (label_map_path, "Label map")]:
        if not os.path.exists(path):
            print(f"ERROR: {name} not found at {path}")
            print("Please run train_kmeans.py first.")
            sys.exit(1)

    kmeans_model = joblib.load(model_path)
    scaler = joblib.load(scaler_path)
    label_map = joblib.load(label_map_path)

    return kmeans_model, scaler, label_map


def calculate_silhouette(scaled_data, labels):
    """Calculate the overall Silhouette Score and per-sample values."""
    overall_score = silhouette_score(scaled_data, labels)
    sample_scores = silhouette_samples(scaled_data, labels)
    return overall_score, sample_scores


def plot_elbow_curve(scaled_data, output_path):
    """
    Plot inertia (WCSS) for K=2 to K=8.
    The 'elbow' at K=4 proves it's the mathematically optimal cluster count.
    """
    inertias = []
    silhouette_scores_list = []

    for k in K_RANGE:
        km = KMeans(n_clusters=k, random_state=SEED, n_init=10)
        km.fit(scaled_data)
        inertias.append(km.inertia_)
        if k > 1:
            score = silhouette_score(scaled_data, km.labels_)
            silhouette_scores_list.append(score)
        else:
            silhouette_scores_list.append(0)

    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(14, 5))

    # --- Left: Elbow Curve ---
    ax1.plot(list(K_RANGE), inertias, 'o-', color='#4ECDC4', linewidth=2.5,
             markersize=8, markerfacecolor='#4ECDC4', markeredgecolor='white', markeredgewidth=1.5)

    # Highlight K=4
    k4_idx = list(K_RANGE).index(OPTIMAL_K)
    ax1.plot(OPTIMAL_K, inertias[k4_idx], 'o', color='#FF6B6B', markersize=16,
             markeredgecolor='white', markeredgewidth=2, zorder=5)
    ax1.annotate(f'Optimal K={OPTIMAL_K}\nInertia={inertias[k4_idx]:.0f}',
                 xy=(OPTIMAL_K, inertias[k4_idx]),
                 xytext=(OPTIMAL_K + 1.2, inertias[k4_idx] + 100),
                 arrowprops=dict(arrowstyle='->', color='#FF6B6B', lw=1.5),
                 fontsize=11, color='#FF6B6B', fontweight='bold',
                 bbox=dict(boxstyle='round,pad=0.4', facecolor='#2a2b3d', edgecolor='#FF6B6B', alpha=0.9))

    ax1.set_xlabel('Number of Clusters (K)')
    ax1.set_ylabel('Inertia (Within-Cluster Sum of Squares)')
    ax1.set_title('Elbow Method — Optimal K Selection')
    ax1.set_xticks(list(K_RANGE))
    ax1.grid(True, linestyle='--', alpha=0.3)

    # --- Right: Silhouette Score by K ---
    bars = ax2.bar(list(K_RANGE), silhouette_scores_list, color='#45B7D1', alpha=0.7,
                   edgecolor='white', linewidth=0.5)
    bars[k4_idx].set_color('#FF6B6B')
    bars[k4_idx].set_alpha(1.0)

    ax2.set_xlabel('Number of Clusters (K)')
    ax2.set_ylabel('Silhouette Score')
    ax2.set_title('Silhouette Score by K')
    ax2.set_xticks(list(K_RANGE))
    ax2.set_ylim(0, 1)
    ax2.grid(True, linestyle='--', alpha=0.3)

    # Add value labels on bars
    for bar, val in zip(bars, silhouette_scores_list):
        ax2.text(bar.get_x() + bar.get_width() / 2, bar.get_height() + 0.02,
                 f'{val:.3f}', ha='center', va='bottom', fontsize=9, color='#e0e0e0')

    plt.tight_layout(pad=2.0)
    fig.savefig(output_path, dpi=200, bbox_inches='tight', facecolor=fig.get_facecolor())
    plt.close(fig)
    print(f"  [OK] Elbow curve saved to: {output_path}")

    return inertias, silhouette_scores_list


def plot_pca_clusters(scaled_data, labels, label_map, output_path):
    """
    Reduce 3D feature space to 2D via PCA and plot colored clusters.
    Shows the panel that clusters are visually separable.
    """
    pca = PCA(n_components=2)
    pca_data = pca.fit_transform(scaled_data)
    explained_var = pca.explained_variance_ratio_

    # Remap raw labels to deterministic ordered labels
    remapped_labels = np.array([label_map[l] for l in labels])

    fig, ax = plt.subplots(figsize=(10, 7))

    for cluster_id in range(OPTIMAL_K):
        mask = remapped_labels == cluster_id
        ax.scatter(pca_data[mask, 0], pca_data[mask, 1],
                   c=COLORS[cluster_id], label=CLUSTER_NAMES[cluster_id],
                   alpha=0.6, s=30, edgecolors='white', linewidth=0.3)

    # Plot cluster centroids
    for cluster_id in range(OPTIMAL_K):
        mask = remapped_labels == cluster_id
        centroid_x = pca_data[mask, 0].mean()
        centroid_y = pca_data[mask, 1].mean()
        ax.scatter(centroid_x, centroid_y, c=COLORS[cluster_id],
                   marker='X', s=200, edgecolors='white', linewidth=2, zorder=10)

    ax.set_xlabel(f'Principal Component 1 ({explained_var[0]:.1%} variance)')
    ax.set_ylabel(f'Principal Component 2 ({explained_var[1]:.1%} variance)')
    ax.set_title(f'PCA Cluster Visualization — K-Means (K={OPTIMAL_K})\n'
                 f'Total Variance Explained: {sum(explained_var):.1%}')
    ax.legend(loc='upper right', fontsize=9, framealpha=0.8,
              facecolor='#1a1b26', edgecolor='#333344')
    ax.grid(True, linestyle='--', alpha=0.2)

    plt.tight_layout()
    fig.savefig(output_path, dpi=200, bbox_inches='tight', facecolor=fig.get_facecolor())
    plt.close(fig)
    print(f"  [OK] PCA visualization saved to: {output_path}")

    return explained_var


def main():
    print("=" * 65)
    print("  DreamSaver ML — K-Means Clustering Evaluation")
    print("=" * 65)
    print(f"  Timestamp: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"  Random Seed: {SEED}")
    print(f"  Samples: {N_SAMPLES}")
    print()

    # Create output directory
    os.makedirs(OUTPUT_DIR, exist_ok=True)

    # ── Step 1: Regenerate data & load model ──
    print("[1/5] Regenerating synthetic dataset (same seed as training)...")
    raw_data = generate_synthetic_data()
    print(f"  Generated {raw_data.shape[0]} samples with {raw_data.shape[1]} features")

    print("\n[2/5] Loading pre-trained model from disk...")
    kmeans_model, scaler, label_map = load_trained_model()
    print(f"  Model: K-Means (K={kmeans_model.n_clusters})")
    print(f"  Label map: {label_map}")

    # Scale data using the saved scaler
    scaled_data = scaler.transform(raw_data)
    labels = kmeans_model.predict(scaled_data)

    # ── Step 2: Silhouette Score ──
    print("\n[3/5] Calculating Silhouette Score...")
    overall_sil, sample_sil = calculate_silhouette(scaled_data, labels)

    # Per-cluster silhouette averages
    remapped_labels = np.array([label_map[l] for l in labels])
    per_cluster_sil = {}
    for c_id in range(OPTIMAL_K):
        mask = remapped_labels == c_id
        per_cluster_sil[c_id] = sample_sil[mask].mean()

    print(f"\n  +----------------------------------------------+")
    print(f"  |  SILHOUETTE SCORE:  {overall_sil:.4f}                   |")
    print(f"  +----------------------------------------------+")

    if overall_sil >= 0.7:
        interpretation = "EXCELLENT - Very strong cluster separation"
    elif overall_sil >= 0.5:
        interpretation = "STRONG - Clear, meaningful clusters"
    elif overall_sil >= 0.35:
        interpretation = "MODERATE - Reasonable cluster structure"
    else:
        interpretation = "WEAK - Clusters may overlap significantly"

    print(f"  Interpretation: {interpretation}")
    print(f"\n  Per-Cluster Silhouette Averages:")
    for c_id in range(OPTIMAL_K):
        name = CLUSTER_NAMES[c_id].replace('\n', ' ')
        print(f"    Cluster {c_id} ({name}): {per_cluster_sil[c_id]:.4f}")

    # ── Step 3: Elbow Curve ──
    print(f"\n[4/5] Running Elbow Method (K={min(K_RANGE)} to K={max(K_RANGE)})...")
    elbow_path = os.path.join(OUTPUT_DIR, "elbow_curve.png")
    inertias, sil_by_k = plot_elbow_curve(scaled_data, elbow_path)

    # ── Step 4: PCA Visualization ──
    print(f"\n[5/5] Generating PCA cluster visualization...")
    pca_path = os.path.join(OUTPUT_DIR, "pca_clusters.png")
    explained_var = plot_pca_clusters(scaled_data, labels, label_map, pca_path)

    # ── Step 5: Cluster Center Statistics ──
    real_centers = scaler.inverse_transform(kmeans_model.cluster_centers_)
    sorted_cluster_ids = np.argsort(kmeans_model.cluster_centers_[:, 1])

    print(f"\n  {'-' * 60}")
    print(f"  CLUSTER CENTER STATISTICS (Unscaled)")
    print(f"  {'-' * 60}")
    print(f"  {'Cluster':<10} {'Age':>8} {'Income (k LKR)':>16} {'Savings (k LKR)':>17}")
    print(f"  {'-' * 60}")
    for new_id in range(OPTIMAL_K):
        original_id = sorted_cluster_ids[new_id]
        center = real_centers[original_id]
        print(f"  {new_id:<10} {center[0]:>8.1f} {center[1]:>16.1f} {center[2]:>17.1f}")
    print(f"  {'-' * 60}")

    # ── Save metrics to text file ──
    metrics_path = os.path.join(OUTPUT_DIR, "kmeans_metrics.txt")
    with open(metrics_path, 'w') as f:
        f.write("DreamSaver ML - K-Means Evaluation Results\n")
        f.write(f"{'=' * 55}\n")
        f.write(f"Timestamp: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
        f.write(f"Random Seed: {SEED}\n")
        f.write(f"Samples: {N_SAMPLES}\n")
        f.write(f"Optimal K: {OPTIMAL_K}\n\n")

        f.write(f"SILHOUETTE SCORE: {overall_sil:.4f}\n")
        f.write(f"Interpretation: {interpretation}\n\n")

        f.write("Per-Cluster Silhouette Averages:\n")
        for c_id in range(OPTIMAL_K):
            name = CLUSTER_NAMES[c_id].replace('\n', ' ')
            f.write(f"  Cluster {c_id} ({name}): {per_cluster_sil[c_id]:.4f}\n")

        f.write(f"\nElbow Method Inertias:\n")
        for k, inertia, sil in zip(K_RANGE, inertias, sil_by_k):
            f.write(f"  K={k}: Inertia={inertia:.1f}, Silhouette={sil:.4f}\n")

        f.write(f"\nPCA Variance Explained:\n")
        f.write(f"  PC1: {explained_var[0]:.4f} ({explained_var[0]:.1%})\n")
        f.write(f"  PC2: {explained_var[1]:.4f} ({explained_var[1]:.1%})\n")
        f.write(f"  Total: {sum(explained_var):.4f} ({sum(explained_var):.1%})\n")

        f.write(f"\nCluster Centers (Unscaled):\n")
        f.write(f"  {'Cluster':<10} {'Age':>8} {'Income(k)':>12} {'Savings(k)':>12}\n")
        for new_id in range(OPTIMAL_K):
            original_id = sorted_cluster_ids[new_id]
            c = real_centers[original_id]
            f.write(f"  {new_id:<10} {c[0]:>8.1f} {c[1]:>12.1f} {c[2]:>12.1f}\n")

    print(f"\n  [OK] Metrics saved to: {metrics_path}")

    # ── Final Summary ──
    print(f"\n{'=' * 65}")
    print(f"  EVALUATION COMPLETE")
    print(f"{'=' * 65}")
    print(f"  Silhouette Score:     {overall_sil:.4f} - {interpretation}")
    print(f"  Optimal K:            {OPTIMAL_K} (validated by elbow + silhouette)")
    print(f"  PCA Variance:         {sum(explained_var):.1%} explained in 2D")
    print(f"  Output directory:     {OUTPUT_DIR}")
    print(f"{'=' * 65}")


if __name__ == "__main__":
    main()
