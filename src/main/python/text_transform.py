from transformers import BartForConditionalGeneration, BartTokenizer
import gensim
import numpy as np
import nltk
from nltk.tokenize import word_tokenize
from nltk.corpus import stopwords
import sys

# Load the pre-trained BART model and tokenizer
model_name = "facebook/bart-large-cnn"
model = BartForConditionalGeneration.from_pretrained(model_name)
tokenizer = BartTokenizer.from_pretrained(model_name)

# Function to summarize text
def summarize_text(text, max_length=150, min_length=40, num_beams=4):
    # Tokenize the input text
    inputs = tokenizer.encode("summarize: " + text, return_tensors="pt", max_length=512, truncation=True)
    
    # Generate summary
    summary_ids = model.generate(inputs, max_length=max_length, min_length=min_length, num_beams=num_beams, length_penalty=2.0, early_stopping=False)
    summary = tokenizer.decode(summary_ids[0], skip_special_tokens=True)
    
    return summary

# load documents from terminal
docs = sys.argv[1]
doc_list = docs.split(" | ")

# generate summary of provided text
summary_list = []
for text in doc_list:
    summary = summarize_text(text, 10, 10)[11:]
    summary_list.append(summary)
print(";".join(summary_list))