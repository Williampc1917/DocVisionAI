from openai import OpenAI
import config

# Initialize the OpenAI client with NVIDIA's API URL and your API key
client = OpenAI(
    base_url="https://integrate.api.nvidia.com/v1",  # NVIDIA's API URL
    api_key=config.NVIDIA_API_KEY  # Replace with your actual API key
)

# Create a chat completion using the Palmyra-Med-70B-32K model
completion = client.chat.completions.create(
    model="writer/palmyra-med-70b-32k",  # Specify the model
    messages=[
        {"role": "system", "content": "You are a highly knowledgeable radiologist assistant. Your job is to provide detailed and thorough answers based on the provided medical information, focusing on radiology-related inquiries. Please include explanations for your recommendations and consider all relevant medical factors."},
        {"role": "user", "content": "Based on the following patient history, how likely is it that this patient is developing pneumonia? Please provide a detailed explanation of the signs we should monitor, the potential complications, and the recommended next steps in management. Consider the patient's history of COPD, hypertension, and type 2 diabetes, and explain how these conditions may influence the current diagnosis."}
    ],
    temperature=0.4,  # Adjusted to allow for more variety in the response
    top_p=0.7,  # Controls diversity via nucleus sampling
    max_tokens=1500,  # Increased to allow for longer responses
    stream=True  # Enables streaming responses
)

# Process and print the response chunks
for chunk in completion:
    if chunk.choices[0].delta.content is not None:
        print(chunk.choices[0].delta.content, end="")
