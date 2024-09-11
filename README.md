# DocVisionAI

DocVisionAI is a web platform designed to streamline radiologist workflows using advanced AI and machine learning models. The platform features multiple ML models for detecting various conditions in medical images, such as pneumonia and lung cancer, with heatmaps generated to assist in diagnosis.

In addition to these capabilities, DocVisionAI integrates a specialized medical large language model (LLM), Palmyra-Med-70B-32K, hosted by NVIDIA NIM. This model provides highly accurate, contextually relevant responses to radiologists' queries, including summarizing patient histories and analyzing medical records.

The platform utilizes Firestore and Firebase Storage for secure data management and Firebase Auth with Google Auth for seamless user authentication. DocVisionAI combines advanced AI technologies with robust cloud infrastructure to enhance diagnostic accuracy and efficiency.


## Demo

You can access the demo of DocVisionAI here: [Demo Link](https:)

Use the following credentials to log in and explore the features:
- **Username**: demo_user
- **Password**: Demo1234!

This demo account is pre-configured with sample patient data and reports, allowing you to fully experience the AI analysis, report generation, and image heatmaps.
## Architecture Overview

Frontend (Bootstrap Studio)
- Built with Bootstrap Studio: Utilized for quick design and responsive, user-friendly interface creation.
- Custom API Integration: JavaScript code enables communication with the Java Spring Boot backend for data and AI interactions.
- Responsive Design: Ensures accessibility and optimal experience across all device types.

Backend (Java Spring Boot)
- Java Spring Boot Backend: Manages all functions for saving and retrieving data and images in Firestore and Firebase Storage.
- Image Processing: Receives and preprocesses image uploads, then dispatches them to the TensorFlow model's Flask API for analysis.
- Data Management: Ensures seamless communication between the frontend and backend, handling data packaging and preparation for machine learning tasks.


Pneumonia Detection Model
- TensorFlow Model: The model is developed using TensorFlow and is hosted on a Flask API for efficient deployment.
- Image Analysis: Upon receiving an image, the model returns a prediction, confidence score, and a heatmap highlighting the areas used for the decision.
- Integration: The Flask API seamlessly integrates with the backend to ensure secure and timely image analysis results.


Medical LLM Integration
- Palmyra-Med-70B-32K LLM: A specialized large language model developed by Writer and optimized by NVIDIA, designed specifically for biomedical applications with leading performance on medical benchmarks.
- Custom Python Functions: Implemented to query patient data from Firestore, enabling radiologists to ask the LLM specific medical questions about reports, assisting in report writing and summarization.
- Contextual Accuracy: The model provides precise, contextually relevant responses tailored to the healthcare domain, enhancing the radiologist's ability to make informed decisions.



 



## Pneumonia Detection Model Training

The pneumonia detection model for DocVisionAI was trained using a Kaggle dataset containing chest X-ray images that were classified as either normal or pneumonia. The dataset was pre-organized into training, validation, and test sets, with images reviewed by medical professionals for accuracy.

To train the model:

- The dataset was split into an 80/20 ratio for training and testing.
- A **TensorFlow** model was developed using the **InceptionV3** architecture for transfer learning. This allowed for efficient feature extraction from the images.
- **Data Augmentation** techniques, such as rotation, zoom, and horizontal flipping, were applied to the training images to enhance model robustness.
- The model was trained to classify X-ray images as either normal or pneumonia, with a binary classification output.
- **Early stopping** and **model checkpointing** were used during training to avoid overfitting and to save the best-performing model.

The final model was saved and integrated into the platform's backend, where it processes incoming X-ray images and returns a pneumonia prediction along with a heatmap generated using Grad-CAM to highlight the areas that contributed most to the model's decision.
## Deployment Overview

DocVisionAI is deployed using a combination of **Google Cloud** and **Firebase** services to ensure scalability, security, and seamless integration of machine learning models and user interfaces. Below is the detailed deployment setup for the backend and frontend components.

### Backend Deployment (Google Cloud)

The backend, developed with **Java Spring Boot**, and various machine learning APIs are hosted on **Google Cloud App Engine**, offering the flexibility of auto-scaling, secure access, and efficient traffic management during high usage.

- **Google Cloud App Engine (Java Spring Boot)**:  
   - The Java Spring Boot backend is deployed to Google Cloud App Engine, ensuring scalability, load balancing, and traffic management.  
   - It integrates with **Firestore** for securely storing patient data and **Firebase Storage** for medical images, ensuring quick data retrieval and storage.

- **Flask API for ML Models**:  
   - The pneumonia detection model, built using TensorFlow, is hosted on a Flask API deployed via Google Cloud. This API receives medical images, processes them, and returns predictions, confidence scores, and heatmaps for diagnosis.

- **Medical LLM (Palmyra-Med-70B-32K)**:  
   - A Flask API, deployed on Google Cloud App Engine, handles the **Palmyra-Med-70B-32K** model for querying patient data from Firestore, assisting radiologists by answering specific questions and generating summaries for medical reports.

### Frontend Deployment (Firebase Hosting)

- **Firebase Hosting**:  
   The frontend, built using **Bootstrap Studio**, is deployed on Firebase Hosting to ensure secure and fast delivery of static assets, integrated seamlessly with backend services.

- **Frontend-Backend Communication**:  
   The frontend interacts with the backend APIs through RESTful requests, enabling functionalities like patient search, report generation, and image uploads.




## API Documentation

You can view the full API documentation [here](https://documenter.getpostman.com/view/38128309/2sAXjQ1V7y).
## License

[MIT](https://choosealicense.com/licenses/mit/)

