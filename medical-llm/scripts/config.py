import os

class Config:
    # OpenAI API key for accessing the Palmyra-Med-70B-32K model
    NVIDIA_API_KEY = os.getenv("NVIDIA_API_KEY")

    # Firestore credentials (path to the JSON key file)
    FIRESTORE_CREDENTIALS = os.getenv("FIRESTORE_CREDENTIALS")

    # Flask-specific settings
    FLASK_ENV = os.getenv("FLASK_ENV", "production")
    FLASK_DEBUG = os.getenv("FLASK_DEBUG", "False").lower() in ['true', '1', 't']

# Create a config instance
config = Config()

