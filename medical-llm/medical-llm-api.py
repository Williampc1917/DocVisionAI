from flask import Flask, request, jsonify, stream_with_context, Response
from flask_cors import CORS
from medical_llm_service import ask_ai_about_patient_reports

app = Flask(__name__)
CORS(app)  # Enable CORS for all routes

@app.route('/api/ask_ai', methods=['POST'])
def ask_ai():
    """
        API endpoint to send a patient's ID and a question, and receive a response from the AI based on patient reports.

        Request:
            - Method: POST
            - JSON body:
                {
                    "patient_id": "12345",  # The unique identifier of the patient
                    "question": "What are the key findings from the latest report?"  # The question to ask the AI
                }

        Returns:
            - 200 OK: A streamed AI response based on the patient reports and the given question.
            - 400 Bad Request: If either 'patient_id' or 'question' is missing in the request.
                {
                    "error": "Missing patient_id or question in request"
                }
            - 500 Internal Server Error: If an error occurs during processing.
                {
                    "error": "[Error message]"
                }

        Raises:
            Exception: If an error occurs while processing the AI request.
        """
    try:
        data = request.json
        patient_id = data.get('patient_id')
        question = data.get('question')

        if not patient_id or not question:
            return jsonify({"error": "Missing patient_id or question in request"}), 400

        def generate():
            response = ask_ai_about_patient_reports(patient_id, question)
            yield response

        return Response(stream_with_context(generate()), content_type='text/plain')

    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    app.run(debug=True, port=4000)


