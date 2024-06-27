import joblib
from nltk.corpus import stopwords
from nltk.tokenize import word_tokenize
from nltk.stem import WordNetLemmatizer
import sys

# Load the model and vectorizer
loaded_model = joblib.load('model.joblib')
loaded_vectorizer = joblib.load('vectorizer.joblib')

stop_words = set(stopwords.words('english'))
lemmatizer = WordNetLemmatizer()

def preprocess(review):
    tokens = word_tokenize(review.lower())
    tokens = [lemmatizer.lemmatize(token) for token in tokens if token.isalnum() and token not in stop_words]
    return ' '.join(tokens)

def suggest_product(review):
    processed_review = preprocess(review)
    review_tfidf = loaded_vectorizer.transform([processed_review])
    prediction = loaded_model.predict(review_tfidf)[0]
    return prediction

new_process_name = sys.argv[1]
print(suggest_product(new_process_name))