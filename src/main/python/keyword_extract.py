from keybert import KeyBERT
import sys


# load documents from terminal
docs = sys.argv[1]
doc_list = docs.split(" | ")

# prepare the BERT model
kw_model = KeyBERT()
final_keywords = []

# for each document, find the 3-word phrase with the most appealing meaning
regenerate = False
while True:
    for doc in doc_list:
        keywords = kw_model.extract_keywords(doc, keyphrase_ngram_range=(1, 2), stop_words=None)
        if keywords[0][0] not in final_keywords:
            final_keywords.append(keywords[0][0])
        else:
            final_keywords = []
            regenerate = True
            break

    if not regenerate:
        break

# return the list of keywords for the corresponding documents
print(";".join(final_keywords))