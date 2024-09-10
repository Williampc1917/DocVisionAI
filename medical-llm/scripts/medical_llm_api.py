from flask import Flask, request, jsonify, stream_with_context, Response
from flask_cors import CORS
from medical_llm_service import ask_ai_about_patient_reports

app = Flask(__name__)
CORS(app)  # Enable CORS for all routes

@app.route('/api/ask_ai', methods=['POST'])
def ask_ai():
    """
        API endpoint to send a user's ID, patient's ID, and a question, and receive a response from the AI based on patient reports.

        Request:
            - Method: POST
            - JSON body:
                {
                    "user_id": "user123",  # The unique identifier of the user
                    "patient_id": "12345",  # The unique identifier of the patient
                    "question": "What are the key findings from the latest report?"  # The question to ask the AI
                }

        Returns:
            - 200 OK: A streamed AI response based on the patient reports and the given question.
            - 400 Bad Request: If 'user_id', 'patient_id', or 'question' is missing in the request.
                {
                    "error": "Missing user_id, patient_id, or question in request"
                }
            - 500 Internal Server Error: If an error occurs during processing.
                {
                    "error": "[Error message]"
                }

        Raises:
            Exception: If an error occurs while processing the AI request.
    """
    try:
        # Extract data from the request
        data = request.json
        user_id = data.get('user_id')  # Fetch the user_id from the request
        patient_id = data.get('patient_id')  # Fetch the patient_id from the request
        question = data.get('question')  # Fetch the question from the request

        # Validate the required fields
        if not user_id or not patient_id or not question:
            return jsonify({"error": "Missing user_id, patient_id, or question in request"}), 400

        # Function to generate the response
        def generate():
            # Pass user_id to the function handling AI interaction (if required)
            response = ask_ai_about_patient_reports(user_id, patient_id, question)
            yield response

        # Return the response
        return Response(stream_with_context(generate()), content_type='text/plain')

    except Exception as e:
        # Handle any unexpected errors
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    app.run(debug=True, port=4000)


