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
    summary_ids = model.generate(inputs, max_length=max_length, min_length=min_length, num_beams=num_beams, length_penalty=2.0, early_stopping=True)
    
    # Decode the summary
    summary = tokenizer.decode(summary_ids[0], skip_special_tokens=True)
    
    return summary

# Load GloVe model
def load_glove_model(glove_file):
    with open(glove_file, 'r', encoding='utf-8') as f:
        model = {}
        for line in f:
            split_line = line.split()
            word = split_line[0]
            embedding = np.array([float(val) for val in split_line[1:]])
            model[word] = embedding
    return model

# Function to calculate cosine similarity
def cosine_similarity(vec1, vec2):
    dot_product = np.dot(vec1, vec2)
    norm1 = np.linalg.norm(vec1)
    norm2 = np.linalg.norm(vec2)
    return dot_product / (norm1 * norm2)

# Function to get related words using GloVe embeddings
def get_related_words_glove(word, model, top_n=5):
    if word not in model:
        return f"The word '{word}' is not in the vocabulary."
    
    word_embedding = model[word]
    similarities = {}
    
    for other_word, other_embedding in model.items():
        if other_word != word:
            similarity = cosine_similarity(word_embedding, other_embedding)
            similarities[other_word] = similarity
    
    sorted_similarities = sorted(similarities.items(), key=lambda item: item[1], reverse=True)
    
    return sorted_similarities[:top_n]

# load documents from terminal
docs = sys.argv[1]
doc_list = docs.split(" | ")

# generate summary of provided text
summary_list = []
for text in doc_list:
    summary = summarize_text(text, 10, 10)[11:]
    summary_list.append(summary)

# only trigger if related words are necessary
if sys.argv[2] == "false":
    print(";".join(summary_list))
elif sys.argv[2] == "true":
    # Load the GloVe model
    glove_file = '../../../../../glove/glove.6B.300d.txt'
    glove_model = load_glove_model(glove_file)

    related_word_list = []
    for summary in summary_list:
        # load tokens from generated summary
        tokens = word_tokenize(summary.lower())
        stop_words = set(stopwords.words('english'))
        filtered_tokens = [token for token in tokens if token not in stop_words and token.isalnum()]

        # Get related words for each token
        related_words_dict = {}
        for token in filtered_tokens:
            related_words = get_related_words_glove(token, glove_model)
            related_words_dict[token] = related_words

        related_words_only = {}
        for token, related_words in related_words_dict.items():
            related_words_only[token] = []
            for word, similarity in related_words:
                related_words_only[token].append(word)
        related_word_list.append(related_words_only)
    print(";".join(related_word_list))