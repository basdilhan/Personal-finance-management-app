import os
from transformers import pipeline, AutoTokenizer, AutoModelForSequenceClassification
from chronos import ChronosPipeline
import torch

# Create models directory if it doesn't exist
os.makedirs("models", exist_ok=True)

print("Starting model downloads... This may take a few minutes depending on your internet speed.\n")

# 1. Download Transaction Classifier
print("Downloading Transaction Categorization Model (cross-encoder/nli-distilroberta-base)...")
classifier_name = "cross-encoder/nli-distilroberta-base"
classifier_path = "models/transaction_classifier"

# Download and save the tokenizer and model locally
tokenizer = AutoTokenizer.from_pretrained(classifier_name)
model = AutoModelForSequenceClassification.from_pretrained(classifier_name)

tokenizer.save_pretrained(classifier_path)
model.save_pretrained(classifier_path)
print(f"✅ Transaction Classifier saved to {classifier_path}\n")

# 2. Download Chronos Forecasting Model
print("Downloading Expense Forecasting Model (amazon/chronos-t5-small)...")
forecaster_name = "amazon/chronos-t5-small"
forecaster_path = "models/chronos_forecaster"

# Download and save Chronos pipeline locally
chronos_pipeline = ChronosPipeline.from_pretrained(
    forecaster_name,
    device_map="cpu",  # Use CPU for downloading/saving
    torch_dtype=torch.float32,
)

chronos_pipeline.model.save_pretrained(forecaster_path)
print(f"✅ Chronos Forecaster saved to {forecaster_path}\n")

print("🎉 All models downloaded successfully! You can now load them from the local 'models' folder.")
