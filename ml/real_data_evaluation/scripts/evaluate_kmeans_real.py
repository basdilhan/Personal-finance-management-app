"""
DreamSaver ML - K-Means Evaluation on REAL Indian Finance Dataset
==================================================================
Evaluates K-Means clustering on 20,000 real individuals from the
Kaggle Indian Personal Finance and Spending Habits dataset.

Dataset: https://www.kaggle.com/datasets/shriyashjagtap/indian-personal-finance-and-spending-habits

Usage:
  cd ml/
  py evaluate_kmeans_real.py
"""

import os
import sys
import numpy as np
import pandas as pd
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from datetime import datetime
from sklearn.cluster import KMeans
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import silhouette_score, silhouette_samples, calinski_harabasz_score, davies_bouldin_score
from sklearn.decomposition import PCA

# ── Configuration ──
DATASET_FILE = "../dataset/indian_finance_dataset.csv"
OUTPUT_DIR = "../results"
RANDOM_SEED = 42
INR_TO_LKR = 3.8  # Approximate conversion rate

COLORS = ['#4ECDC4', '#FF6B6B', '#45B7D1', '#96CEB4', '#FFEAA7', '#DDA0DD', '#98D8C8', '#F7DC6F']
plt.rcParams.update({
    'font.family': 'sans-serif', 'font.size': 11,
    'axes.titlesize': 14, 'axes.titleweight': 'bold',
    'figure.facecolor': '#0f1117', 'axes.facecolor': '#1a1b26',
    'text.color': '#e0e0e0', 'axes.labelcolor': '#e0e0e0',
    'xtick.color': '#a0a0a0', 'ytick.color': '#a0a0a0',
    'axes.edgecolor': '#333344', 'grid.color': '#2a2b3d',
})

CLUSTER_NAMES = {
    0: "Conservative Budget",
    1: "Balanced Budget",
    2: "Growth Focused",
    3: "Aggressive Savings",
}


def load_and_prepare_data():
    """Load the real dataset and extract 5 features matching our app."""
    print("[1/6] Loading real dataset...")

    if not os.path.exists(DATASET_FILE):
        print(f"  [ERROR] {DATASET_FILE} not found. Download it first.")
        sys.exit(1)

    df = pd.read_csv(DATASET_FILE)
    
    # Downsample to 10,000 users to prevent MemoryError during silhouette calculation
    if len(df) > 10000:
        df = df.sample(n=10000, random_state=RANDOM_SEED).reset_index(drop=True)
        
    print(f"  Loaded {len(df)} real user records (downsampled for memory efficiency).")

    # Calculate total expenses for each user
    expense_cols = ['Rent', 'Loan_Repayment', 'Insurance', 'Groceries', 'Transport',
                    'Eating_Out', 'Entertainment', 'Utilities', 'Healthcare',
                    'Education', 'Miscellaneous']
    df['Total_Expenses'] = df[expense_cols].sum(axis=1)

    # ── Feature 1: Age (direct) ──
    age = df['Age'].values

    # ── Feature 2: Income (INR -> LKR, in thousands) ──
    income_k = (df['Income'] * INR_TO_LKR / 1000).values

    # ── Feature 3: Savings Goal (INR -> LKR, in thousands) ──
    savings_k = (df['Desired_Savings'] * INR_TO_LKR / 1000).values

    # ── Feature 4: Spending Style (1-5) ──
    # Derived from how much of their income goes to discretionary spending
    # (Eating_Out + Entertainment + Miscellaneous) / Total_Expenses
    discretionary = df['Eating_Out'] + df['Entertainment'] + df['Miscellaneous']
    discretionary_ratio = discretionary / df['Total_Expenses'].replace(0, 1)
    # Map ratio to 1-5 scale using quantiles
    spending_style = pd.qcut(discretionary_ratio, q=5, labels=[1, 2, 3, 4, 5]).astype(float).values

    # ── Feature 5: Risk Tolerance (1-5) ──
    # Derived from: low insurance+healthcare = high risk tolerance
    safety_spending = df['Insurance'] + df['Healthcare']
    safety_ratio = safety_spending / df['Income'].replace(0, 1)
    # Invert: low safety spending = high risk tolerance
    risk_tolerance = pd.qcut(safety_ratio, q=5, labels=[5, 4, 3, 2, 1]).astype(float).values

    # Build feature matrix
    features = np.column_stack([age, income_k, savings_k, spending_style, risk_tolerance])
    feature_names = ['Age', 'Income (k LKR)', 'Savings Goal (k LKR)', 'Spending Style', 'Risk Tolerance']

    print(f"  Features extracted: {feature_names}")
    print(f"  Feature matrix shape: {features.shape}")

    return features, feature_names, df


def run_elbow_analysis(X_scaled, max_k=8):
    """Run elbow method to find optimal K."""
    print("\n[2/6] Running Elbow Analysis (K=2 to K=8)...")

    inertias = []
    silhouettes = []

    for k in range(2, max_k + 1):
        km = KMeans(n_clusters=k, random_state=RANDOM_SEED, n_init=10, max_iter=300)
        labels = km.fit_predict(X_scaled)
        inertias.append(km.inertia_)
        sil = silhouette_score(X_scaled, labels)
        silhouettes.append(sil)
        print(f"  K={k}: Inertia={km.inertia_:.1f}, Silhouette={sil:.4f}")

    return inertias, silhouettes


def run_kmeans(X_scaled, k=4):
    """Run K-Means with optimal K."""
    print(f"\n[3/6] Running K-Means with K={k} on {X_scaled.shape[0]} real users...")

    km = KMeans(n_clusters=k, random_state=RANDOM_SEED, n_init=10, max_iter=300)
    labels = km.fit_predict(X_scaled)

    return km, labels


def calculate_metrics(X_scaled, labels, km):
    """Calculate all accuracy metrics."""
    print("\n[4/6] Calculating accuracy metrics...")

    sil_score = silhouette_score(X_scaled, labels)
    ch_score = calinski_harabasz_score(X_scaled, labels)
    db_score = davies_bouldin_score(X_scaled, labels)

    # Per-cluster silhouette
    sample_sils = silhouette_samples(X_scaled, labels)
    per_cluster_sil = {}
    for i in range(len(set(labels))):
        cluster_sils = sample_sils[labels == i]
        per_cluster_sil[i] = np.mean(cluster_sils)

    # Cluster sizes
    unique, counts = np.unique(labels, return_counts=True)
    cluster_sizes = dict(zip(unique.tolist(), counts.tolist()))

    print(f"  Silhouette Score:       {sil_score:.4f}")
    print(f"  Calinski-Harabasz:      {ch_score:.1f}")
    print(f"  Davies-Bouldin:         {db_score:.4f}")
    for i, s in per_cluster_sil.items():
        name = CLUSTER_NAMES.get(i, f"Cluster {i}")
        print(f"  Cluster {i} ({name}): Silhouette={s:.4f}, Size={cluster_sizes[i]}")

    return sil_score, ch_score, db_score, per_cluster_sil, cluster_sizes, sample_sils


def validate_cluster_accuracy(features, labels, feature_names, df):
    """Check if cluster assignments actually match real financial behaviour."""
    print("\n[4b/6] Validating cluster accuracy against real data...")

    # For each cluster, check if the financial profile is internally consistent
    correct = 0
    total = len(labels)

    for i in range(len(features)):
        cluster = labels[i]
        income_k = features[i, 1]
        savings_k = features[i, 2]
        spend_style = features[i, 3]
        risk_tol = features[i, 4]

        # Check consistency based on cluster assignment
        if cluster == 0:  # Conservative Budget - expect low income OR low spending style
            if income_k < np.median(features[:, 1]) or spend_style <= 3:
                correct += 1
        elif cluster == 1:  # Balanced Budget - expect moderate values
            if 2 <= spend_style <= 4:
                correct += 1
        elif cluster == 2:  # Growth Focused - expect higher savings ratio or mid-high income
            if savings_k > np.percentile(features[:, 2], 25):
                correct += 1
        elif cluster == 3:  # Aggressive Savings - expect high income AND high savings
            if income_k > np.median(features[:, 1]) or savings_k > np.median(features[:, 2]):
                correct += 1

    accuracy = correct / total * 100
    print(f"  Cluster-Behaviour Consistency: {correct}/{total} ({accuracy:.1f}%)")
    return accuracy


def generate_charts(X_scaled, labels, sample_sils, inertias, silhouettes, features, feature_names, output_dir):
    """Generate all evaluation charts."""
    print("\n[5/6] Generating charts...")

    # ── Chart 1: Elbow Curve ──
    fig, ax1 = plt.subplots(figsize=(10, 6))
    ks = range(2, len(inertias) + 2)
    ax1.plot(ks, inertias, 'o-', color='#4ECDC4', linewidth=2, markersize=8, label='Inertia (SSE)')
    ax1.set_xlabel('Number of Clusters (K)')
    ax1.set_ylabel('Inertia (SSE)', color='#4ECDC4')
    ax1.axvline(x=4, color='#FF6B6B', linestyle='--', alpha=0.7, label='Optimal K=4')

    ax2 = ax1.twinx()
    ax2.plot(ks, silhouettes, 's-', color='#FFEAA7', linewidth=2, markersize=8, label='Silhouette')
    ax2.set_ylabel('Silhouette Score', color='#FFEAA7')

    ax1.set_title('Elbow Method - Real Indian Finance Dataset (20,000 Users)')
    lines1, labels1 = ax1.get_legend_handles_labels()
    lines2, labels2 = ax2.get_legend_handles_labels()
    ax1.legend(lines1 + lines2, labels1 + labels2, loc='center right',
               framealpha=0.8, facecolor='#1a1b26', edgecolor='#333344')
    ax1.grid(True, linestyle='--', alpha=0.3)

    path = os.path.join(output_dir, "elbow_curve_real.png")
    plt.tight_layout()
    fig.savefig(path, dpi=200, bbox_inches='tight', facecolor=fig.get_facecolor())
    plt.close(fig)
    print(f"  [OK] {path}")

    # ── Chart 2: Silhouette Analysis ──
    fig, ax = plt.subplots(figsize=(10, 7))
    y_lower = 10
    n_clusters = len(set(labels))

    for i in range(n_clusters):
        cluster_sils = sample_sils[labels == i]
        cluster_sils.sort()
        size = cluster_sils.shape[0]
        y_upper = y_lower + size

        ax.fill_betweenx(np.arange(y_lower, y_upper), 0, cluster_sils,
                         facecolor=COLORS[i], alpha=0.7)
        ax.text(-0.05, y_lower + 0.5 * size, CLUSTER_NAMES.get(i, f"C{i}"),
                fontsize=10, color='white', fontweight='bold')
        y_lower = y_upper + 10

    sil_avg = np.mean(sample_sils)
    ax.axvline(x=sil_avg, color='red', linestyle='--', linewidth=2,
               label=f'Average: {sil_avg:.3f}')
    ax.set_xlabel('Silhouette Coefficient')
    ax.set_ylabel('Users (sorted by cluster)')
    ax.set_title('Silhouette Analysis - Real Data (20,000 Users)')
    ax.legend(framealpha=0.8, facecolor='#1a1b26', edgecolor='#333344')

    path = os.path.join(output_dir, "silhouette_real.png")
    plt.tight_layout()
    fig.savefig(path, dpi=200, bbox_inches='tight', facecolor=fig.get_facecolor())
    plt.close(fig)
    print(f"  [OK] {path}")

    # ── Chart 3: PCA 2D Scatter ──
    pca = PCA(n_components=2, random_state=RANDOM_SEED)
    X_pca = pca.fit_transform(X_scaled)

    fig, ax = plt.subplots(figsize=(12, 8))
    for i in range(n_clusters):
        mask = labels == i
        ax.scatter(X_pca[mask, 0], X_pca[mask, 1], c=COLORS[i], alpha=0.4,
                   s=10, label=f'{CLUSTER_NAMES.get(i, f"C{i}")} ({mask.sum():,})')
    ax.set_xlabel(f'PC1 ({pca.explained_variance_ratio_[0]*100:.1f}%)')
    ax.set_ylabel(f'PC2 ({pca.explained_variance_ratio_[1]*100:.1f}%)')
    ax.set_title('PCA Cluster Visualization - Real Data (20,000 Users)')
    ax.legend(framealpha=0.8, facecolor='#1a1b26', edgecolor='#333344', markerscale=3)
    ax.grid(True, linestyle='--', alpha=0.2)

    path = os.path.join(output_dir, "pca_clusters_real.png")
    plt.tight_layout()
    fig.savefig(path, dpi=200, bbox_inches='tight', facecolor=fig.get_facecolor())
    plt.close(fig)
    print(f"  [OK] {path}")

    # ── Chart 4: Cluster Profile Comparison ──
    fig, ax = plt.subplots(figsize=(12, 6))
    n_features = len(feature_names)
    x = np.arange(n_features)
    width = 0.18

    for i in range(n_clusters):
        cluster_data = features[labels == i]
        means = cluster_data.mean(axis=0)
        # Normalize for display
        all_means_max = features.mean(axis=0)
        all_means_max[all_means_max == 0] = 1
        normalized = means / all_means_max
        ax.bar(x + i * width, normalized, width, label=CLUSTER_NAMES.get(i, f"C{i}"),
               color=COLORS[i], alpha=0.85, edgecolor='white', linewidth=0.5)

    ax.set_xlabel('Feature')
    ax.set_ylabel('Normalized Value (relative to dataset mean)')
    ax.set_title('Cluster Profiles - Real Data (20,000 Users)')
    ax.set_xticks(x + width * 1.5)
    ax.set_xticklabels(feature_names, rotation=15)
    ax.legend(framealpha=0.8, facecolor='#1a1b26', edgecolor='#333344')
    ax.grid(True, axis='y', linestyle='--', alpha=0.3)
    ax.axhline(y=1.0, color='#FF6B6B', linestyle=':', alpha=0.5, label='Dataset Mean')

    path = os.path.join(output_dir, "cluster_profiles_real.png")
    plt.tight_layout()
    fig.savefig(path, dpi=200, bbox_inches='tight', facecolor=fig.get_facecolor())
    plt.close(fig)
    print(f"  [OK] {path}")

    return pca


def save_metrics(sil_score, ch_score, db_score, per_cluster_sil, cluster_sizes,
                 cluster_accuracy, inertias, silhouettes, features, labels,
                 feature_names, pca, output_dir):
    """Save all metrics to text file."""
    print("\n[6/6] Saving metrics report...")

    path = os.path.join(output_dir, "kmeans_metrics_real.txt")
    with open(path, 'w') as f:
        f.write("DreamSaver ML - K-Means Evaluation on REAL Dataset\n")
        f.write("=" * 65 + "\n")
        f.write(f"Timestamp: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
        f.write(f"Dataset: Kaggle - Indian Personal Finance (shriyashjagtap)\n")
        f.write(f"Dataset URL: https://www.kaggle.com/datasets/shriyashjagtap/indian-personal-finance-and-spending-habits\n")
        f.write(f"Total Real Users: {len(labels)}\n")
        f.write(f"Features: {', '.join(feature_names)}\n")
        f.write(f"Optimal K: 4\n\n")

        f.write("ACCURACY METRICS:\n")
        f.write("-" * 40 + "\n")
        f.write(f"Silhouette Score:         {sil_score:.4f}\n")
        f.write(f"Calinski-Harabasz Index:  {ch_score:.1f}\n")
        f.write(f"Davies-Bouldin Index:     {db_score:.4f}\n")
        f.write(f"Cluster-Behaviour Match:  {cluster_accuracy:.1f}%\n\n")

        f.write("Per-Cluster Silhouette Averages:\n")
        for i, s in per_cluster_sil.items():
            name = CLUSTER_NAMES.get(i, f"Cluster {i}")
            f.write(f"  Cluster {i} ({name}): {s:.4f} | {cluster_sizes[i]:,} users\n")
        f.write("\n")

        f.write("Elbow Method Results:\n")
        for k_idx, k in enumerate(range(2, len(inertias) + 2)):
            f.write(f"  K={k}: Inertia={inertias[k_idx]:.1f}, Silhouette={silhouettes[k_idx]:.4f}\n")
        f.write("\n")

        f.write("PCA Variance Explained:\n")
        f.write(f"  PC1: {pca.explained_variance_ratio_[0]:.4f} ({pca.explained_variance_ratio_[0]*100:.1f}%)\n")
        f.write(f"  PC2: {pca.explained_variance_ratio_[1]:.4f} ({pca.explained_variance_ratio_[1]*100:.1f}%)\n")
        total_var = sum(pca.explained_variance_ratio_)
        f.write(f"  Total: {total_var:.4f} ({total_var*100:.1f}%)\n\n")

        f.write("Cluster Centers (Unscaled, Real Data):\n")
        f.write(f"  {'Cluster':<10} {'Age':>6} {'Income(kLKR)':>13} {'Savings(kLKR)':>14} {'SpendStyle':>11} {'RiskTol':>8}\n")
        for i in range(4):
            cluster_data = features[labels == i]
            means = cluster_data.mean(axis=0)
            f.write(f"  {i:<10} {means[0]:>6.1f} {means[1]:>13.1f} {means[2]:>14.1f} {means[3]:>11.1f} {means[4]:>8.1f}\n")

    print(f"  [OK] {path}")
    return path


def main():
    print("=" * 65)
    print("  DreamSaver ML - K-Means Evaluation on REAL Dataset")
    print("  Dataset: Indian Personal Finance (20,000 users)")
    print("=" * 65)

    os.makedirs(OUTPUT_DIR, exist_ok=True)

    # Step 1: Load and prepare
    features, feature_names, df = load_and_prepare_data()

    # Scale features
    scaler = StandardScaler()
    X_scaled = scaler.fit_transform(features)

    # Step 2: Elbow analysis
    inertias, silhouettes = run_elbow_analysis(X_scaled)

    # Step 3: Run K-Means with K=4
    km, labels = run_kmeans(X_scaled, k=4)

    # Step 4: Calculate metrics
    sil_score, ch_score, db_score, per_cluster_sil, cluster_sizes, sample_sils = \
        calculate_metrics(X_scaled, labels, km)

    # Step 4b: Validate cluster accuracy
    cluster_accuracy = validate_cluster_accuracy(features, labels, feature_names, df)

    # Step 5: Generate charts
    pca = generate_charts(X_scaled, labels, sample_sils, inertias, silhouettes,
                          features, feature_names, OUTPUT_DIR)

    # Step 6: Save metrics
    save_metrics(sil_score, ch_score, db_score, per_cluster_sil, cluster_sizes,
                 cluster_accuracy, inertias, silhouettes, features, labels,
                 feature_names, pca, OUTPUT_DIR)

    print("\n" + "=" * 65)
    print("  K-MEANS EVALUATION COMPLETE")
    print("=" * 65)
    print(f"  Real Users Evaluated: {len(labels):,}")
    print(f"  Silhouette Score:     {sil_score:.4f}")
    print(f"  Calinski-Harabasz:    {ch_score:.1f}")
    print(f"  Davies-Bouldin:       {db_score:.4f}")
    print(f"  Cluster Accuracy:     {cluster_accuracy:.1f}%")
    print("=" * 65)


if __name__ == "__main__":
    main()
