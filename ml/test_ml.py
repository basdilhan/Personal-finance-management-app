import sys
from transformers import pipeline

classifier = pipeline("text-classification", model="samudu123/srilanka-transaction-classifier")

descriptions = [
    "bought some groceries at keells",
    "highway bus ticket",
    "dialog mobile reload",
    "hospital bill",
    "movie tickets",
    "paid school fees"
]

for desc in descriptions:
    res = classifier(desc)[0]
    print(f"'{desc}' -> {res['label']}")
