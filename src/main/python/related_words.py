from nltk.tokenize import word_tokenize
from nltk.corpus import stopwords
import gensim
import numpy as np

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

# Load the GloVe model
glove_file = '../../../../../glove/glove.6B.300d.txt'
glove_model = load_glove_model(glove_file)

def extract_related_words(tokens):
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

    return related_words_only