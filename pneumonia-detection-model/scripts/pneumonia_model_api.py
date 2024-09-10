import base64
import os
import cv2
import numpy as np
from flask import Flask, request, jsonify, send_file
import tensorflow as tf
from keras.models import load_model
from keras.utils import img_to_array
import io
from PIL import Image
import matplotlib.pyplot as plt

os.environ["CUDA_VISIBLE_DEVICES"] = "-1"  # Disable all GPUs
tf.config.set_visible_devices([], 'GPU')

app = Flask(__name__)

model = None

def load_model_once():
    global model
    if model is None:
        model_path = os.getenv('MODEL_PATH', 'pneumonia_xray_classifier_new.h5')
        model = load_model(model_path)
        app.logger.info('Model loaded successfully')
    return model

def preprocess_image(image_bytes, img_size=(256, 256)):
    img_arr = np.frombuffer(image_bytes, np.uint8)
    img = cv2.imdecode(img_arr, cv2.IMREAD_COLOR)
    img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
    img = cv2.resize(img, img_size)
    img = img_to_array(img)  # Convert image to numpy array
    img = img / 255.0  # Rescale
    img = np.expand_dims(img, axis=0)  # Add batch dimension
    return img

def make_gradcam_heatmap(img_array, model, last_conv_layer_name, pred_index=None):
    grad_model = tf.keras.models.Model(
        [model.inputs], [model.get_layer(last_conv_layer_name).output, model.output]
    )

    with tf.GradientTape() as tape:
        last_conv_layer_output, preds = grad_model(img_array)
        if pred_index is None:
            pred_index = tf.argmax(preds[0])
        class_channel = preds[:, pred_index]

    grads = tape.gradient(class_channel, last_conv_layer_output)
    pooled_grads = tf.reduce_mean(grads, axis=(0, 1, 2))

    last_conv_layer_output = last_conv_layer_output[0]
    heatmap = last_conv_layer_output @ pooled_grads[..., tf.newaxis]
    heatmap = tf.squeeze(heatmap)

    heatmap = tf.maximum(heatmap, 0) / tf.math.reduce_max(heatmap)
    return heatmap.numpy()

def guided_backpropagation(model, img, layer_name='conv2d'):
    with tf.GradientTape() as tape:
        inputs = tf.cast(img, tf.float32)
        tape.watch(inputs)
        outputs = model(inputs)[0]

    grads = tape.gradient(outputs, inputs)[0]
    return grads.numpy()

def apply_guided_gradcam(image_bytes, model, last_conv_layer_name='mixed8'):
    img_array = preprocess_image(image_bytes)
    img = cv2.imdecode(np.frombuffer(image_bytes, np.uint8), cv2.IMREAD_COLOR)
    img_rgb = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)

    heatmap = make_gradcam_heatmap(img_array, model, last_conv_layer_name)
    guided_grads = guided_backpropagation(model, img_array)

    # Resize heatmap to match the size of the guided grads
    heatmap = cv2.resize(heatmap, (guided_grads.shape[1], guided_grads.shape[0]))
    heatmap = np.expand_dims(heatmap, axis=-1)

    guided_gradcam = guided_grads * heatmap
    guided_gradcam = np.maximum(guided_gradcam, 0)
    guided_gradcam = guided_gradcam / np.max(guided_gradcam)

    # Ensure guided_gradcam has the correct shape
    guided_gradcam = np.sum(guided_gradcam, axis=-1)
    guided_gradcam = cv2.resize(guided_gradcam, (img_rgb.shape[1], img_rgb.shape[0]))

    # Apply colormap
    colormap = plt.get_cmap('viridis')
    guided_gradcam_colored = colormap(guided_gradcam)

    # Convert to RGB and remove alpha channel
    guided_gradcam_colored = np.delete(guided_gradcam_colored, 3, axis=2)

    return img_rgb, guided_gradcam_colored

@app.route('/predict', methods=['POST'])
def predict():
    """
        Endpoint to predict pneumonia from a chest X-ray image.

        The endpoint accepts a POST request with an image file and returns the diagnosis,
        confidence level, and a Guided Grad-CAM heatmap in base64 format.

        Request:
            - Method: POST
            - Form-data:
                - image: The chest X-ray image file to be analyzed.

        Returns:
            - 200 OK:
                {
                    'result': "Pneumonia detected with 85.50% confidence",
                    'diagnosis': "Pneumonia detected" or "Normal",
                    'confidence': 0.855,  # Confidence score of the prediction
                    'heatmap': "<base64_encoded_heatmap_image>"  # Base64 encoded heatmap image
                }
            - 400 Bad Request:
                {
                    'error': 'No image provided'
                }
            - 500 Internal Server Error:
                {
                    'error': 'Internal Server Error'
                }

        Raises:
            Exception: If an error occurs during the prediction process.
        """
    try:
        app.logger.info('Received request: %s', request)
        if 'image' not in request.files:
            app.logger.error('No image provided in request')
            return jsonify({'error': 'No image provided'}), 400

        img = request.files['image'].read()
        img_array = preprocess_image(img)
        model = load_model_once()
        prediction = model.predict(img_array)[0][0]

        if prediction > 0.5:
            diagnosis = "Pneumonia detected"
            confidence = prediction
        else:
            diagnosis = "Normal"
            confidence = 1 - prediction

        confidence = float(confidence)  # Convert float32 to native Python float

        # Generate Guided Grad-CAM heatmap
        original_img, guided_gradcam = apply_guided_gradcam(img, model)

        # Create a response image with the heatmap
        pil_img = Image.fromarray((guided_gradcam * 255).astype(np.uint8))
        buf = io.BytesIO()
        pil_img.save(buf, format='JPEG')
        buf.seek(0)
        heatmap_base64 = base64.b64encode(buf.getvalue()).decode('utf-8')

        return jsonify({
            'result': f"{diagnosis} with {confidence * 100:.2f}% confidence",
            'diagnosis': diagnosis,
            'confidence': confidence,
            'heatmap': heatmap_base64
        })
    except Exception as e:
        app.logger.error('Error during prediction: %s', str(e))
        return jsonify({'error': 'Internal Server Error'}), 500

if __name__ == '__main__':
    port = int(os.getenv('PORT', 5000))
    app.run(debug=bool(os.getenv('DEBUG', True)), port=port)



