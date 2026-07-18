# ============================================================
# DreamSaver ML - Expense Forecasting with Chronos
# ============================================================
# Run this notebook on Google Colab (free GPU runtime)
#
# STEP 1: Go to https://colab.research.google.com
# STEP 2: Upload this script or paste into a new notebook
# STEP 3: Go to Runtime → Change runtime type → GPU (T4)
# STEP 4: Run all cells
# ============================================================

# %% [Cell 1] Install required libraries
# !pip install chronos-forecasting transformers torch pandas matplotlib huggingface_hub -q

# %% [Cell 2] Upload your dataset
# from google.colab import files
# uploaded = files.upload()  # Upload monthly_expenses.csv

# %% [Cell 3] Load and visualise the data
import pandas as pd
import matplotlib.pyplot as plt
import torch
import numpy as np

df = pd.read_csv("monthly_expenses.csv")
print(f"Months of data: {len(df)}")
print(df.head())

# Plot the expense history
fig, axes = plt.subplots(2, 2, figsize=(14, 8))

axes[0, 0].plot(df['month'], df['total_expense'], 'r-o', linewidth=2)
axes[0, 0].set_title('Total Expenses Over Time')
axes[0, 0].tick_params(axis='x', rotation=45)

axes[0, 1].plot(df['month'], df['total_income'], 'g-o', linewidth=2)
axes[0, 1].set_title('Total Income Over Time')
axes[0, 1].tick_params(axis='x', rotation=45)

axes[1, 0].plot(df['month'], df['total_bills'], 'orange', marker='o', linewidth=2)
axes[1, 0].set_title('Monthly Bills Over Time')
axes[1, 0].tick_params(axis='x', rotation=45)

# Stacked bar chart of categories
categories = ['food', 'transport', 'utilities', 'housing', 'health', 'shopping', 'entertainment', 'education']
bottom = np.zeros(len(df))
colors = ['#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4', '#FFEAA7', '#DDA0DD', '#98D8C8', '#F7DC6F']
for cat, color in zip(categories, colors):
    if cat in df.columns:
        axes[1, 1].bar(range(len(df)), df[cat], bottom=bottom, label=cat, color=color)
        bottom += df[cat].values
axes[1, 1].set_title('Expense Breakdown by Category')
axes[1, 1].legend(fontsize=7)

plt.tight_layout()
plt.savefig('expense_history.png', dpi=150)
plt.show()
print("Saved chart as expense_history.png")

# %% [Cell 4] Load Chronos Pretrained Model
from chronos import ChronosPipeline

pipeline = ChronosPipeline.from_pretrained(
    "amazon/chronos-t5-small",
    device_map="cuda" if torch.cuda.is_available() else "cpu",
    torch_dtype=torch.float32,
)
print(f"Chronos loaded on: {'GPU' if torch.cuda.is_available() else 'CPU'}")

# %% [Cell 5] Predict next month's total expenses
context = torch.tensor(df["total_expense"].values, dtype=torch.float32)

# Predict next 3 months
forecast = pipeline.predict(
    context=context,
    prediction_length=3,
    num_samples=100,  # Monte Carlo samples for uncertainty
)

# Extract predictions
forecast_np = forecast.numpy()
pred_mean = np.median(forecast_np, axis=1)[0]  # Median of samples
pred_low  = np.percentile(forecast_np, 10, axis=1)[0]  # 10th percentile
pred_high = np.percentile(forecast_np, 90, axis=1)[0]  # 90th percentile

print("\n" + "="*60)
print("CHRONOS EXPENSE FORECAST")
print("="*60)
for i in range(3):
    print(f"Month +{i+1}: LKR {pred_mean[i]:,.0f}  "
          f"(Range: LKR {pred_low[i]:,.0f} - LKR {pred_high[i]:,.0f})")
print("="*60)

# %% [Cell 6] Predict next month's bills
context_bills = torch.tensor(df["total_bills"].values, dtype=torch.float32)
forecast_bills = pipeline.predict(context=context_bills, prediction_length=3, num_samples=100)
bills_mean = np.median(forecast_bills.numpy(), axis=1)[0]

print("\nBILLS FORECAST")
for i in range(3):
    print(f"Month +{i+1} Bills: LKR {bills_mean[i]:,.0f}")

# %% [Cell 7] Predict next month's income
context_income = torch.tensor(df["total_income"].values, dtype=torch.float32)
forecast_income = pipeline.predict(context=context_income, prediction_length=3, num_samples=100)
income_mean = np.median(forecast_income.numpy(), axis=1)[0]

print("\nINCOME FORECAST")
for i in range(3):
    print(f"Month +{i+1} Income: LKR {income_mean[i]:,.0f}")

# %% [Cell 8] Predict per-category expenses
print("\nCATEGORY-WISE FORECAST (Next Month)")
print("-" * 40)
category_predictions = {}
for cat in categories:
    if cat in df.columns:
        ctx = torch.tensor(df[cat].values, dtype=torch.float32)
        fc = pipeline.predict(context=ctx, prediction_length=1, num_samples=50)
        pred = float(np.median(fc.numpy(), axis=1)[0][0])
        category_predictions[cat] = pred
        print(f"  {cat:15s}: LKR {pred:>10,.0f}")

# %% [Cell 9] Visualise forecast vs actual
fig, ax = plt.subplots(figsize=(12, 5))

months = list(range(len(df)))
forecast_months = [len(df) + i for i in range(3)]

ax.plot(months, df['total_expense'], 'b-o', linewidth=2, label='Actual Expenses')
ax.plot(forecast_months, pred_mean, 'r-s', linewidth=2, markersize=10, label='Predicted Expenses')
ax.fill_between(forecast_months, pred_low, pred_high, alpha=0.2, color='red', label='Prediction Range (80%)')
ax.axvline(x=len(df)-0.5, color='gray', linestyle='--', alpha=0.5)
ax.text(len(df)-0.3, max(df['total_expense'])*1.05, '← Actual | Forecast →', fontsize=10)

ax.set_xlabel('Month Index')
ax.set_ylabel('Amount (LKR)')
ax.set_title('DreamSaver - Expense Forecast with Chronos T5')
ax.legend()
plt.tight_layout()
plt.savefig('forecast_chart.png', dpi=150)
plt.show()
print("Saved forecast chart as forecast_chart.png")

# %% [Cell 10] Save predictions as JSON for the API server
import json

predictions = {
    "next_month_expense": float(pred_mean[0]),
    "next_month_expense_low": float(pred_low[0]),
    "next_month_expense_high": float(pred_high[0]),
    "next_month_bills": float(bills_mean[0]),
    "next_month_income": float(income_mean[0]),
    "next_month_net_cash_flow": float(income_mean[0] - pred_mean[0]),
    "next_3_months_expense": [float(x) for x in pred_mean],
    "next_3_months_bills": [float(x) for x in bills_mean],
    "next_3_months_income": [float(x) for x in income_mean],
    "category_predictions": {k: round(v, 2) for k, v in category_predictions.items()},
    "model": "amazon/chronos-t5-small",
    "data_months": len(df),
}

with open("forecast_predictions.json", "w") as f:
    json.dump(predictions, f, indent=2)

print("\n✅ Predictions saved to forecast_predictions.json")
print(f"\nSummary:")
print(f"  Next month predicted expense: LKR {pred_mean[0]:,.0f}")
print(f"  Next month predicted bills:   LKR {bills_mean[0]:,.0f}")
print(f"  Next month predicted income:  LKR {income_mean[0]:,.0f}")
print(f"  Next month net cash flow:     LKR {income_mean[0] - pred_mean[0]:,.0f}")
