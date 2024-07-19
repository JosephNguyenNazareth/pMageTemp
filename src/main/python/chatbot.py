import torch
from transformers import BartForConditionalGeneration, BartTokenizer

# Load BART model and tokenizer
model_name = 'facebook/bart-large'
tokenizer = BartTokenizer.from_pretrained(model_name)
model = BartForConditionalGeneration.from_pretrained(model_name)

def chatbot_bart(input_text):
    # Tokenize input text
    inputs = tokenizer(input_text, return_tensors='pt')

    # Generate response
    with torch.no_grad():
        outputs = model.generate(**inputs)

    # Decode and return response
    response = tokenizer.decode(outputs[0], skip_special_tokens=True)
    return response

while True:
    user_input = input("You: ")
    if user_input.lower() == 'exit':
        break
    
    response = chatbot_bart(user_input)
    print("Bot:", response)