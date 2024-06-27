# %%
import pymongo
import random
import pandas as pd
import related_words

# %%
project_name_path = './projectName.txt'
project_name_list = []
with open(project_name_path, 'r') as file:
    content = file.read()
    project_name_list = content.split("\n")

app_path = './app.txt'
app_list = []
with open(app_path, 'r') as file:
    content = file.read()
    app_list = content.split("\n")

user_path = './user.txt'
user_list = []
with open(user_path, 'r') as file:
    content = file.read()
    user_list = content.split("\n")

print(project_name_list, app_list, user_list)

# %%
rules = {}
rules["visual"] = ["tableau", "power bi"]
rules["monitor"] = ["tableau", "power bi"]
rules["software"] = ["gitlab", "github"]
rules["cloud"] = ["gitlab", "github", "cisco"]
rules["implement"] = ["gitlab", "github"]
rules["train"] = ["openclassroom"]
rules["hub"] = ["openclassroom", "cisco"]

# %%


# %%
tokens = list(rules.keys())
rules_extend = related_words.extract_related_words(tokens)
print(rules_extend)

# %%


# %%
hist = []

# instead of random, we should have some rules !
for _ in range(10000):
    rand_hist = {}
    rand_hist["processName"] = random.choice(project_name_list)
    triggered = False
    for keyword in rules.keys():
        if keyword in rand_hist["processName"].lower():
            triggered = True
            rand_hist["app"] = random.choice(rules[keyword])
            break
        for related_keyword in rules_extend[keyword]:
            if related_keyword in rand_hist["processName"].lower():
                triggered = True
                rand_hist["app"] = random.choice(rules[keyword])
                break
        if triggered:
            break
            
    if not triggered:
        rand_hist["app"] = random.choice(app_list)
    rand_hist["userName"] = random.choice(user_list)
    hist.append(rand_hist)

# %%
df = pd.DataFrame.from_dict(hist) 
df.to_csv("hist.csv", index=False)

# %%
import nltk
from nltk.corpus import stopwords
from nltk.tokenize import word_tokenize
from nltk.stem import WordNetLemmatizer
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.model_selection import train_test_split
from sklearn.naive_bayes import MultinomialNB
from sklearn.metrics import accuracy_score
import joblib

# Download necessary NLTK data
nltk.download('punkt')
nltk.download('stopwords')

# %%
# Example labeled reviews and their categories (labels)
reviews = df["processName"].to_list()
labels = df["app"].to_list()

# %%
stop_words = set(stopwords.words('english'))
lemmatizer = WordNetLemmatizer()

def preprocess(review):
    tokens = word_tokenize(review.lower())
    tokens = [lemmatizer.lemmatize(token) for token in tokens if token.isalnum() and token not in stop_words]
    return ' '.join(tokens)

processed_reviews = [preprocess(review) for review in reviews]

# %%
# Convert text data to TF-IDF features
vectorizer = TfidfVectorizer()
X = vectorizer.fit_transform(processed_reviews)

# %%
# Split the data into training and test sets
X_train, X_test, y_train, y_test = train_test_split(X, labels, test_size=0.2, random_state=42)

# Train a Naive Bayes classifier
model = MultinomialNB()
model.fit(X_train, y_train)

# Evaluate the model
y_pred = model.predict(X_test)
accuracy = accuracy_score(y_test, y_pred)
print(f"Model Accuracy: {accuracy * 100:.2f}%")

# %%
# Save the model and vectorizer
joblib.dump(model, 'model.joblib')
joblib.dump(vectorizer, 'vectorizer.joblib')