import torch
import pandas as pd
from datasets import load_dataset
from transformers import BartTokenizer, BartForConditionalGeneration, Trainer, TrainingArguments
from pathlib import Path
import sys

def summarize_phrase(ft_model, phrase, min_len=4, max_len=10):
    # Tokenize the input phrase
    inputs = tokenizer(phrase, max_length=150, truncation=True, return_tensors="pt")

    summary_ids = ft_model.generate(
        input_ids=inputs["input_ids"],
        attention_mask=inputs["attention_mask"],
        max_length=max_len,
        min_length=min_len,
        num_beams=4,
        length_penalty=2.0,
        early_stopping=True
    )

    # Decode the summary
    summary = tokenizer.decode(summary_ids[0], skip_special_tokens=True)
    return summary

model_dir = Path("D:/Pop_Documents/workspace/github/pMage_AI_model/fine-tuned-bart/")
ft_model = BartForConditionalGeneration.from_pretrained(model_dir)
tokenizer = BartTokenizer.from_pretrained(model_dir)

input_collection = sys.argv[1]
input_phrases = input_collection.split(" | ")

extra_info = []
for input_phrase in input_phrases:
    summary = summarize_phrase(ft_model, input_phrase, 10, 10)
    extra_info.append[summary]
print(";".join(extra_info))