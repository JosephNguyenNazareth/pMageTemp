import related_words
import sys

tokens = sys.argv[1].split(",")
related_tokens = related_words.extract_related_words(tokens)["name"]
result = related_words.extract_related_words(related_tokens).values()

flatten_result = []
for bucket in result:
    flatten_result += bucket
print(flatten_result)