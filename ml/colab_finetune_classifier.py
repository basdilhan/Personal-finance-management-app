# ============================================================
# DreamSaver ML - Transaction Classifier Fine-Tuning
# ============================================================
# Run this notebook on Google Colab (free GPU runtime)
#
# STEP 1: Go to https://colab.research.google.com
# STEP 2: Click File → Upload Notebook → Upload this .ipynb or
#         paste this script into a new Colab notebook
# STEP 3: Go to Runtime → Change runtime type → GPU (T4)
# STEP 4: Run all cells
# ============================================================

# %% [Cell 1] Install required libraries
# !pip install transformers datasets torch accelerate huggingface_hub -q

# %% [Cell 2] Upload your dataset
# from google.colab import files
# uploaded = files.upload()  # Upload srilanka_transactions.csv

# %% [Cell 3] Load and prepare dataset
import pandas as pd
from datasets import Dataset, DatasetDict
from sklearn.model_selection import train_test_split

df = pd.read_csv("srilanka_transactions.csv")
print(f"Total samples: {len(df)}")
print(f"Categories: {df['label'].nunique()}")
print(df['label'].value_counts())

# Create label mapping
labels = sorted(df['label'].unique().tolist())
label2id = {label: i for i, label in enumerate(labels)}
id2label = {i: label for label, i in label2id.items()}
num_labels = len(labels)

print(f"\nLabel mapping: {label2id}")

# Convert labels to integers
df['label_id'] = df['label'].map(label2id)

# Split into train/test
train_df, test_df = train_test_split(df, test_size=0.2, stratify=df['label'], random_state=42)

train_dataset = Dataset.from_pandas(train_df[['description', 'label_id']].rename(columns={'label_id': 'label'}))
test_dataset = Dataset.from_pandas(test_df[['description', 'label_id']].rename(columns={'label_id': 'label'}))

dataset = DatasetDict({'train': train_dataset, 'test': test_dataset})
print(f"\nTrain: {len(train_dataset)}, Test: {len(test_dataset)}")

# %% [Cell 4] Load pretrained model and tokenizer
from transformers import AutoTokenizer, AutoModelForSequenceClassification

model_name = "distilbert-base-uncased"

tokenizer = AutoTokenizer.from_pretrained(model_name)
model = AutoModelForSequenceClassification.from_pretrained(
    model_name,
    num_labels=num_labels,
    id2label=id2label,
    label2id=label2id,
)

print(f"Model loaded: {model_name}")
print(f"Number of categories: {num_labels}")

# %% [Cell 5] Tokenize the dataset
def tokenize_function(examples):
    return tokenizer(
        examples["description"],
        padding="max_length",
        truncation=True,
        max_length=64,  # Transaction descriptions are short
    )

tokenized_dataset = dataset.map(tokenize_function, batched=True)
print("Dataset tokenized!")

# %% [Cell 6] Set up training
from transformers import TrainingArguments, Trainer
import numpy as np

def compute_metrics(eval_pred):
    logits, labels = eval_pred
    predictions = np.argmax(logits, axis=-1)
    accuracy = (predictions == labels).mean()
    return {"accuracy": accuracy}

training_args = TrainingArguments(
    output_dir="./results",
    num_train_epochs=15,           # More epochs for small dataset
    per_device_train_batch_size=16,
    per_device_eval_batch_size=16,
    warmup_steps=50,
    weight_decay=0.01,
    learning_rate=2e-5,
    logging_dir="./logs",
    logging_steps=10,
    eval_strategy="epoch",
    save_strategy="epoch",
    load_best_model_at_end=True,
    metric_for_best_model="accuracy",
    report_to="none",
)

trainer = Trainer(
    model=model,
    args=training_args,
    train_dataset=tokenized_dataset["train"],
    eval_dataset=tokenized_dataset["test"],
    compute_metrics=compute_metrics,
)

# %% [Cell 7] Train!
print("Starting fine-tuning...")
trainer.train()

# %% [Cell 8] Evaluate
results = trainer.evaluate()
print(f"\n{'='*50}")
print(f"FINAL TEST ACCURACY: {results['eval_accuracy']:.2%}")
print(f"{'='*50}")

# %% [Cell 9] Test with real Sri Lankan transactions
from transformers import pipeline

classifier = pipeline(
    "text-classification",
    model=trainer.model,
    tokenizer=tokenizer,
)

test_transactions = [
    "KFC",
    "Keells Super",
    "Lanka IOC Fuel",
    "CEB Electricity",
    "Dialog Mobile",
    "Nawaloka Hospital",
    "Netflix",
    "Daraz.lk",
    "Monthly salary",
    "Uber Lanka",
    "Landlord Rent",
    "British Council",
    "AIA Insurance",
    "Bank loan EMI",
    "Temple donation",
]

print("\n--- Testing with Sri Lankan Transactions ---\n")
for tx in test_transactions:
    result = classifier(tx)[0]
    label = id2label[int(result['label'].split('_')[-1])] if 'LABEL' in result['label'] else result['label']
    print(f"  {tx:30s} → {label:15s} ({result['score']:.1%})")

# %% [Cell 10] Push to Hugging Face Hub
# IMPORTANT: Replace 'YOUR_USERNAME' with your actual HF username
# First, login: run this in a cell:
#   from huggingface_hub import notebook_login
#   notebook_login()

HF_USERNAME = "YOUR_USERNAME"  # <-- CHANGE THIS
MODEL_NAME = "srilanka-transaction-classifier"

trainer.model.push_to_hub(f"{HF_USERNAME}/{MODEL_NAME}")
tokenizer.push_to_hub(f"{HF_USERNAME}/{MODEL_NAME}")

# Save label mapping as a JSON file
import json
label_config = {"label2id": label2id, "id2label": id2label, "labels": labels}
with open("label_config.json", "w") as f:
    json.dump(label_config, f, indent=2)

print(f"\n✅ Model pushed to: https://huggingface.co/{HF_USERNAME}/{MODEL_NAME}")
print(f"✅ Label config saved to label_config.json")
print(f"\nYou can now use this model via the HF Inference API!")
