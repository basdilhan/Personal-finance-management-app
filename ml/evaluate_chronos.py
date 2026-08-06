"""
DreamSaver ML - Chronos-T5 Forecasting Evaluation (Backtesting)
================================================================
Produces formal evaluation metrics for the Chronos-T5-tiny time-series
forecasting model (Layer 2 of the dual-layer ML architecture).

Strategy:
  1. Generate 50 synthetic 12-month user spending timelines
  2. For each user, feed N months -> predict month N+1
  3. Compare prediction vs known actual
  4. Calculate MAE, MAPE, RMSE
  5. Also benchmark against the mathematical fallback (avg x 1.05)

Outputs:
  - MAE, MAPE, RMSE by history length (printed + saved)
  - Accuracy vs. History Length bar chart (PNG)
  - Predicted vs. Actual scatter plot (PNG)

Usage:
  cd ml/
  python evaluate_chronos.py

All outputs are saved to ml/evaluation_results/
"""

import os
import sys
import numpy as np
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from datetime import datetime

# ── Configuration ──
SEED = 42
N_USERS = 50
N_MONTHS = 12
HISTORY_LENGTHS = [3, 5, 7, 9, 11]  # Feed N months, predict month N+1
OUTPUT_DIR = os.path.join(os.path.dirname(__file__), "evaluation_results")

# ── Chart Styling ──
COLORS_CHRONOS = '#4ECDC4'
COLORS_FALLBACK = '#FF6B6B'
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


def generate_synthetic_timelines(n_users=N_USERS, n_months=N_MONTHS, seed=SEED):
    """
    Generate realistic monthly spending timelines for synthetic users.

    Each user has:
      - A base spending level (lognormal, 60k-100k LKR range)
      - Seasonal variation (±10%, peaks around month 12 = December)
      - Linear trend (slight upward drift ~1.5% per month)
      - Random noise (±5% Gaussian)

    Returns:
      np.ndarray of shape (n_users, n_months) with monthly spending in LKR
    """
    np.random.seed(seed)
    timelines = np.zeros((n_users, n_months))

    for i in range(n_users):
        # Base monthly spending: lognormal centered around 70k LKR
        base = np.random.lognormal(mean=11.15, sigma=0.25)  # e^11.15 ≈ 70k
        base = np.clip(base, 40000, 150000)

        for m in range(n_months):
            # Seasonal component (sinusoidal, peak in December = month 12)
            seasonal = 1.0 + 0.10 * np.sin(2 * np.pi * (m - 3) / 12)

            # Trend component (1.5% monthly growth — inflation/lifestyle)
            trend = 1.0 + 0.015 * m

            # Random noise (±5%)
            noise = 1.0 + np.random.normal(0, 0.05)

            timelines[i, m] = base * seasonal * trend * noise

    return np.round(timelines, 2)


def predict_with_chronos(history, chronos_pipeline):
    """
    Run Chronos-T5 prediction (same logic as production main.py lines 113-116).
    Returns predicted value or None if model unavailable.
    """
    import torch
    torch.manual_seed(42)
    context_tensor = torch.tensor(history, dtype=torch.float32)
    forecast = chronos_pipeline.predict(context_tensor, prediction_length=1)
    predicted = float(forecast[0].median().item())
    return predicted


def predict_with_fallback(history):
    """
    Mathematical fallback (same logic as production main.py lines 108-111).
    avg × 1.05 (5% buffer)
    """
    non_zero = [x for x in history if x > 0]
    if len(non_zero) == 0:
        return 15000.0
    return (sum(non_zero) / len(non_zero)) * 1.05


def calculate_metrics(predictions, actuals):
    """Calculate MAE, MAPE, and RMSE from parallel arrays."""
    predictions = np.array(predictions)
    actuals = np.array(actuals)

    errors = np.abs(predictions - actuals)
    mae = np.mean(errors)

    # MAPE: avoid division by zero
    valid_mask = actuals > 0
    mape = np.mean(errors[valid_mask] / actuals[valid_mask]) * 100

    rmse = np.sqrt(np.mean((predictions - actuals) ** 2))

    return mae, mape, rmse


def plot_accuracy_vs_history(chronos_results, fallback_results, output_path):
    """Bar chart comparing MAPE of Chronos vs Fallback across history lengths."""
    fig, ax = plt.subplots(figsize=(10, 6))

    x = np.arange(len(HISTORY_LENGTHS))
    width = 0.35

    chronos_mapes = [chronos_results[h]['mape'] for h in HISTORY_LENGTHS] if chronos_results else [0] * len(HISTORY_LENGTHS)
    fallback_mapes = [fallback_results[h]['mape'] for h in HISTORY_LENGTHS]

    bars1 = ax.bar(x - width/2, fallback_mapes, width, label='Math Fallback (avg × 1.05)',
                   color=COLORS_FALLBACK, alpha=0.8, edgecolor='white', linewidth=0.5)

    if chronos_results:
        bars2 = ax.bar(x + width/2, chronos_mapes, width, label='Chronos-T5-Tiny',
                       color=COLORS_CHRONOS, alpha=0.8, edgecolor='white', linewidth=0.5)

        # Value labels for Chronos
        for bar, val in zip(bars2, chronos_mapes):
            ax.text(bar.get_x() + bar.get_width()/2, bar.get_height() + 0.3,
                    f'{val:.1f}%', ha='center', va='bottom', fontsize=9, color=COLORS_CHRONOS, fontweight='bold')

    # Value labels for fallback
    for bar, val in zip(bars1, fallback_mapes):
        ax.text(bar.get_x() + bar.get_width()/2, bar.get_height() + 0.3,
                f'{val:.1f}%', ha='center', va='bottom', fontsize=9, color=COLORS_FALLBACK, fontweight='bold')

    ax.set_xlabel('Number of Historical Months Used')
    ax.set_ylabel('MAPE (Mean Absolute Percentage Error) %')
    ax.set_title('Prediction Accuracy vs. Historical Data Length')
    ax.set_xticks(x)
    ax.set_xticklabels([f'{h} months' for h in HISTORY_LENGTHS])
    ax.legend(loc='upper right', framealpha=0.8, facecolor='#1a1b26', edgecolor='#333344')
    ax.grid(True, axis='y', linestyle='--', alpha=0.3)

    # Add "good accuracy" reference line
    ax.axhline(y=20, color='#F7DC6F', linestyle='--', alpha=0.5, linewidth=1)
    ax.text(len(HISTORY_LENGTHS) - 0.8, 20.5, '< 20% = Good', fontsize=9, color='#F7DC6F', alpha=0.7)

    plt.tight_layout()
    fig.savefig(output_path, dpi=200, bbox_inches='tight', facecolor=fig.get_facecolor())
    plt.close(fig)
    print(f"  [OK] Accuracy vs history chart saved to: {output_path}")


def plot_predicted_vs_actual(all_predictions, all_actuals, model_name, output_path):
    """Scatter plot of predicted vs actual values with y=x diagonal reference line."""
    fig, ax = plt.subplots(figsize=(8, 8))

    preds = np.array(all_predictions)
    acts = np.array(all_actuals)

    # Scatter points
    ax.scatter(acts, preds, c=COLORS_CHRONOS if 'Chronos' in model_name else COLORS_FALLBACK,
               alpha=0.5, s=40, edgecolors='white', linewidth=0.3)

    # Perfect prediction line (y=x)
    lims = [min(acts.min(), preds.min()) * 0.9, max(acts.max(), preds.max()) * 1.1]
    ax.plot(lims, lims, '--', color='#F7DC6F', linewidth=1.5, alpha=0.7, label='Perfect Prediction (y=x)')

    # +/-10% error bands
    x_range = np.linspace(lims[0], lims[1], 100)
    ax.fill_between(x_range, x_range * 0.9, x_range * 1.1,
                    alpha=0.08, color='#4ECDC4', label='+/-10% Error Band')

    ax.set_xlabel('Actual Spending (LKR)')
    ax.set_ylabel('Predicted Spending (LKR)')
    ax.set_title(f'Predicted vs. Actual - {model_name}')
    ax.set_xlim(lims)
    ax.set_ylim(lims)
    ax.set_aspect('equal')
    ax.legend(loc='upper left', framealpha=0.8, facecolor='#1a1b26', edgecolor='#333344')
    ax.grid(True, linestyle='--', alpha=0.2)

    plt.tight_layout()
    fig.savefig(output_path, dpi=200, bbox_inches='tight', facecolor=fig.get_facecolor())
    plt.close(fig)
    print(f"  [OK] Scatter plot saved to: {output_path}")


def main():
    print("=" * 65)
    print("  DreamSaver ML — Chronos-T5 Forecasting Evaluation")
    print("=" * 65)
    print(f"  Timestamp: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"  Random Seed: {SEED}")
    print(f"  Synthetic Users: {N_USERS}")
    print(f"  Months per User: {N_MONTHS}")
    print(f"  History Lengths to Test: {HISTORY_LENGTHS}")
    print()

    os.makedirs(OUTPUT_DIR, exist_ok=True)

    # ── Step 1: Generate synthetic timelines ──
    print("[1/5] Generating synthetic spending timelines...")
    timelines = generate_synthetic_timelines()
    print(f"  Generated {timelines.shape[0]} users × {timelines.shape[1]} months")
    print(f"  Spending range: LKR {timelines.min():,.0f} — LKR {timelines.max():,.0f}")
    print(f"  Average monthly: LKR {timelines.mean():,.0f}")

    # ── Step 2: Try loading Chronos-T5 ──
    print("\n[2/5] Attempting to load Chronos-T5-Tiny model...")
    chronos_pipeline = None
    try:
        import torch
        torch.set_num_threads(1)
        from chronos import ChronosPipeline
        chronos_pipeline = ChronosPipeline.from_pretrained(
            "amazon/chronos-t5-tiny",
            device_map="cpu",
            torch_dtype=torch.float32,
        )
        print("  [OK] Chronos-T5-Tiny loaded successfully")
    except ImportError:
        print("  [WARN] chronos-forecasting not installed. Skipping Chronos evaluation.")
        print("     Only the mathematical fallback baseline will be evaluated.")
    except Exception as e:
        print(f"  [WARN] Failed to load Chronos: {e}")
        print("     Only the mathematical fallback baseline will be evaluated.")

    # ── Step 3: Run backtesting ──
    chronos_results = {} if chronos_pipeline else None
    fallback_results = {}

    for n_hist in HISTORY_LENGTHS:
        print(f"\n[3/5] Backtesting with {n_hist} months of history...")
        target_month_idx = n_hist  # Predict month at index n_hist (0-indexed)

        if target_month_idx >= N_MONTHS:
            print(f"  Skipping — need at least {n_hist + 1} months of data")
            continue

        chronos_preds = []
        chronos_actuals = []
        fallback_preds = []
        fallback_actuals = []

        for user_idx in range(N_USERS):
            history = timelines[user_idx, :n_hist].tolist()
            actual = timelines[user_idx, target_month_idx]

            # Fallback prediction
            fb_pred = predict_with_fallback(history)
            fallback_preds.append(fb_pred)
            fallback_actuals.append(actual)

            # Chronos prediction
            if chronos_pipeline is not None:
                try:
                    ch_pred = predict_with_chronos(history, chronos_pipeline)
                    # Apply same safeguard as production: if prediction <= 0, use fallback
                    if ch_pred <= 0:
                        ch_pred = fb_pred
                    chronos_preds.append(ch_pred)
                    chronos_actuals.append(actual)
                except Exception as e:
                    print(f"    [WARN] Chronos failed for user {user_idx}: {e}")
                    chronos_preds.append(fb_pred)
                    chronos_actuals.append(actual)

        # Calculate metrics
        fb_mae, fb_mape, fb_rmse = calculate_metrics(fallback_preds, fallback_actuals)
        fallback_results[n_hist] = {'mae': fb_mae, 'mape': fb_mape, 'rmse': fb_rmse,
                                     'predictions': fallback_preds, 'actuals': fallback_actuals}

        if chronos_pipeline is not None and len(chronos_preds) > 0:
            ch_mae, ch_mape, ch_rmse = calculate_metrics(chronos_preds, chronos_actuals)
            chronos_results[n_hist] = {'mae': ch_mae, 'mape': ch_mape, 'rmse': ch_rmse,
                                        'predictions': chronos_preds, 'actuals': chronos_actuals}
            print(f"  Chronos  -> MAE: {ch_mae:>10,.0f} LKR  |  MAPE: {ch_mape:>5.1f}%  |  RMSE: {ch_rmse:>10,.0f} LKR")

        print(f"  Fallback -> MAE: {fb_mae:>10,.0f} LKR  |  MAPE: {fb_mape:>5.1f}%  |  RMSE: {fb_rmse:>10,.0f} LKR")

    # ── Step 4: Generate charts ──
    print(f"\n[4/5] Generating evaluation charts...")

    accuracy_path = os.path.join(OUTPUT_DIR, "accuracy_vs_history.png")
    plot_accuracy_vs_history(chronos_results, fallback_results, accuracy_path)

    # Scatter plot — use all predictions from the longest history length
    best_hist = max(HISTORY_LENGTHS)
    if chronos_results and best_hist in chronos_results:
        scatter_path = os.path.join(OUTPUT_DIR, "predicted_vs_actual_chronos.png")
        plot_predicted_vs_actual(
            chronos_results[best_hist]['predictions'],
            chronos_results[best_hist]['actuals'],
            "Chronos-T5-Tiny",
            scatter_path
        )

    scatter_fb_path = os.path.join(OUTPUT_DIR, "predicted_vs_actual_fallback.png")
    plot_predicted_vs_actual(
        fallback_results[best_hist]['predictions'],
        fallback_results[best_hist]['actuals'],
        "Math Fallback (avg x 1.05)",
        scatter_fb_path
    )

    # ── Step 5: Save metrics ──
    print(f"\n[5/5] Saving evaluation metrics...")
    metrics_path = os.path.join(OUTPUT_DIR, "chronos_metrics.txt")
    with open(metrics_path, 'w') as f:
        f.write("DreamSaver ML - Chronos-T5 Forecasting Evaluation Results\n")
        f.write(f"{'=' * 60}\n")
        f.write(f"Timestamp: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
        f.write(f"Random Seed: {SEED}\n")
        f.write(f"Synthetic Users: {N_USERS}\n")
        f.write(f"Months per User: {N_MONTHS}\n")
        f.write(f"Chronos Model: {'Loaded' if chronos_pipeline else 'Not Available'}\n\n")

        # Results table
        f.write(f"{'History':<10} ")
        if chronos_results:
            f.write(f"{'Chronos MAE':>14} {'Chronos MAPE':>14} {'Chronos RMSE':>14} ")
        f.write(f"{'Fallback MAE':>14} {'Fallback MAPE':>14} {'Fallback RMSE':>14}\n")
        f.write(f"{'-' * (10 + (42 if chronos_results else 0) + 42)}\n")

        for n_hist in HISTORY_LENGTHS:
            if n_hist not in fallback_results:
                continue
            fb = fallback_results[n_hist]
            line = f"{n_hist:>3} months "
            if chronos_results and n_hist in chronos_results:
                ch = chronos_results[n_hist]
                line += f"{ch['mae']:>13,.0f} {ch['mape']:>13.1f}% {ch['rmse']:>13,.0f} "
            line += f"{fb['mae']:>13,.0f} {fb['mape']:>13.1f}% {fb['rmse']:>13,.0f}"
            f.write(line + "\n")

        f.write(f"\n\nDefense Summary:\n")
        f.write(f"{'-' * 60}\n")
        if chronos_results:
            best_chronos = chronos_results[max(chronos_results.keys())]
            f.write(f"Best Chronos MAPE (with {max(chronos_results.keys())} months): {best_chronos['mape']:.1f}%\n")
        best_fb = fallback_results[max(fallback_results.keys())]
        f.write(f"Best Fallback MAPE (with {max(fallback_results.keys())} months): {best_fb['mape']:.1f}%\n")
        if chronos_results:
            improvement = best_fb['mape'] - chronos_results[max(chronos_results.keys())]['mape']
            f.write(f"Chronos improvement over fallback: {improvement:.1f} percentage points\n")

    print(f"  [OK] Metrics saved to: {metrics_path}")

    # ── Final Summary ──
    print(f"\n{'=' * 65}")
    print(f"  EVALUATION COMPLETE")
    print(f"{'=' * 65}")
    print(f"  Model tested: {'Chronos-T5-Tiny + Math Fallback' if chronos_pipeline else 'Math Fallback Only'}")
    print()

    print(f"  {'History':<12} ", end="")
    if chronos_results:
        print(f"{'Chronos MAPE':>14} ", end="")
    print(f"{'Fallback MAPE':>15}")
    print(f"  {'-' * 45}")

    for n_hist in HISTORY_LENGTHS:
        if n_hist not in fallback_results:
            continue
        line = f"  {n_hist:>3} months    "
        if chronos_results and n_hist in chronos_results:
            line += f"{chronos_results[n_hist]['mape']:>12.1f}%  "
        line += f"{fallback_results[n_hist]['mape']:>12.1f}%"
        print(line)

    print(f"  {'-' * 45}")
    print(f"  Output directory: {OUTPUT_DIR}")
    print(f"{'=' * 65}")


if __name__ == "__main__":
    main()
