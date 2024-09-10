import logging
from google.cloud import firestore
from openai import OpenAI
from config import config
from datetime import datetime
from google.cloud.firestore import SERVER_TIMESTAMP
from google.cloud.firestore_v1 import SERVER_TIMESTAMP as TIMESTAMP

# Initialize Firestore client
db = firestore.Client.from_service_account_json(config.FIRESTORE_CREDENTIALS)

# Initialize OpenAI client with NVIDIA's API
client = OpenAI(
    base_url="https://integrate.api.nvidia.com/v1",  # NVIDIA's API URL
    api_key=config.NVIDIA_API_KEY
)

# Setup logging
logging.basicConfig(level=logging.INFO)

FIRESTORE_LIMIT = 1 * 1024 * 1024  # 1 MB limit


def start_new_session(user_id, patient_id):
    """
        Start a new conversation session for the specified user and patient.
        This creates a new Firestore document under the 'conversations' collection for that patient.

        Args:
            user_id (str): The ID of the user.
            patient_id (str): The ID of the patient.

        Returns:
            str: The session ID of the newly created session.
    """
    session_id = f"session_{datetime.utcnow().strftime('%Y%m%d%H%M%S')}"
    session_ref = db.collection('Users').document(user_id).collection('patients').document(patient_id).collection(
        'conversations').document(session_id)
    session_ref.set({
        'sessionStart': firestore.SERVER_TIMESTAMP,  # Use server timestamp for the session start time
        'messages': [],
        'reports_included': False,  # Track if patient reports have been included
        'last_fetch_time': firestore.SERVER_TIMESTAMP  # Store the fetch time as a Firestore Timestamp
    })
    logging.info(f"Started a new session with ID: {session_id}")
    return session_id


def update_last_fetch_time(user_id, patient_id, session_id):
    """
        Update the last fetch time of the session to the current server time.

        Args:
            user_id (str): The ID of the user.
            patient_id (str): The ID of the patient.
            session_id (str): The session ID to update.
    """
    session_ref = db.collection('Users').document(user_id).collection('patients').document(patient_id).collection(
        'conversations').document(session_id)
    session_ref.update({
        'last_fetch_time': firestore.SERVER_TIMESTAMP  # Update last fetch time as a Firestore Timestamp
    })
    logging.info(f"Updated last fetch time for session ID: {session_id}")


def save_message_to_session(user_id, patient_id, session_id, role, content):
    """
        Save a message to an ongoing conversation session. If the session size exceeds the Firestore limit,
        start a new session.

        Args:
            user_id (str): The ID of the user.
            patient_id (str): The ID of the patient.
            session_id (str): The session ID to save the message in.
            role (str): The role of the message sender (e.g., 'user' or 'assistant').
            content (str): The content of the message.
    """
    session_ref = db.collection('Users').document(user_id).collection('patients').document(patient_id).collection(
        'conversations').document(session_id)
    session_data = session_ref.get().to_dict()

    messages = session_data.get('messages', [])
    current_size = sum(len(m['content']) for m in messages) + len(content)

    if current_size > FIRESTORE_LIMIT:
        logging.info(f"Session {session_id} is too large. Starting a new session.")
        session_id = start_new_session(user_id, patient_id)
        session_ref = db.collection('Users').document(user_id).collection('patients').document(patient_id).collection(
            'conversations').document(session_id)

    # Save the message with a timestamp
    session_ref.update({
        'messages': firestore.ArrayUnion([{
            'role': role,
            'content': content,
            'timestamp': datetime.utcnow().isoformat()  # Store as an ISO string for simplicity
        }])
    })


def get_conversation_history(user_id, patient_id, session_id):
    """
        Retrieve the conversation history for the given user, patient, and session.

        Args:
            user_id (str): The ID of the user.
            patient_id (str): The ID of the patient.
            session_id (str): The session ID to fetch the history from.

        Returns:
            list: A list of messages in the conversation.
    """
    session_ref = db.collection('Users').document(user_id).collection('patients').document(patient_id).collection(
        'conversations').document(session_id)
    session_data = session_ref.get()

    if session_data.exists:
        return session_data.to_dict().get('messages', [])
    return []


def get_current_session_id(user_id, patient_id):
    """
        Retrieve the latest session ID for the user and patient, or create a new session if none exists.

        Args:
            user_id (str): The ID of the user.
            patient_id (str): The ID of the patient.

        Returns:
            str: The session ID.
    """
    sessions_ref = db.collection('Users').document(user_id).collection('patients').document(patient_id).collection(
        'conversations')
    sessions = sessions_ref.order_by('sessionStart', direction=firestore.Query.DESCENDING).limit(1).stream()
    latest_session = next(sessions, None)
    if latest_session:
        logging.info(f"Using existing session with ID: {latest_session.id}")
    else:
        logging.info(f"No existing session found. Starting a new one.")
    return latest_session.id if latest_session else start_new_session(user_id, patient_id)


def get_patient_radiology_reports(patient_id, last_fetch_time=None):
    """
        Retrieve radiology reports for the specified patient, optionally filtering by a timestamp.

        Args:
            patient_id (str): The ID of the patient.
            last_fetch_time (Timestamp, optional): Only retrieve reports created after this time.

        Returns:
            list: A list of radiology report documents.
    """
    logging.info(f"Retrieving reports for patient ID: {patient_id}")
    reports_ref = db.collection('Patients').document(patient_id).collection('RadiologyReports')

    if last_fetch_time:
        logging.info(f"Querying for reports with created_at > {last_fetch_time}")
        reports = reports_ref.where('created_at', '>', last_fetch_time).stream()
    else:
        logging.info("Querying for all reports (no last_fetch_time provided)")
        reports = reports_ref.stream()

    report_list = []
    for report in reports:
        report_data = report.to_dict()
        report_id = report.id  # Retrieve the reportId from Firestore document ID
        report_data['reportId'] = report_id  # Add reportId to the dictionary
        logging.info(f"Retrieved Report ID: {report_id}")
        report_list.append(report_data)

    if len(report_list) == 0:
        logging.info(f"No new reports found for patient ID: {patient_id}")
    else:
        logging.info(f"Found {len(report_list)} reports for patient ID: {patient_id}")

    return report_list


def create_flexible_prompt(patient_id, question, reports):
    """
    Create a flexible prompt that includes all patient reports and the radiologist's question,
    ensuring that no reports are missed.

    Args:
        patient_id (str): The ID of the patient.
        question (str): The question from the radiologist.
        reports (list): List of patient radiology reports.

    Returns:
        str: The generated prompt to be sent to the AI model.
    """
    prompt = "Based on the following patient history and radiology reports, please answer the question below:\n\n"
    prompt += f"Patient ID: {patient_id}\n\n"

    for report in reports:
        # Include the reportId and patientName in the prompt
        prompt += f"Report ID: {report.get('reportId', 'N/A')}\n"
        prompt += f"Patient Name: {report.get('patientName', 'N/A')}\n"
        prompt += f"- Report Date: {report.get('reportDate', 'N/A')} (Created At: {report.get('created_at', 'N/A')})\n"
        prompt += f"  - Type of Study: {report.get('typeOfStudy', 'N/A')}\n"
        prompt += f"  - Clinical History: {report.get('clinicalHistory', 'N/A')}\n"
        prompt += f"  - Findings:\n"
        prompt += f"    - Airways: {report.get('airways', 'N/A')}\n"
        prompt += f"    - Left Lung: {report.get('leftLung', 'N/A')}\n"
        prompt += f"    - Right Lung: {report.get('rightLung', 'N/A')}\n"
        prompt += f"    - Pleura: {report.get('pleura', 'N/A')}\n"
        prompt += f"  - Impression: {report.get('impression', 'N/A')}\n"
        prompt += f"  - Technique: {report.get('technique', 'N/A')}\n"
        prompt += "\n"

    prompt += f"Radiologist's Question: {question}\n"

    logging.info(f"Generated prompt for AI:\n{prompt}")

    return prompt


def ask_ai_about_patient_reports(user_id, patient_id, question):
    """
        Handle the AI interaction by generating a prompt based on the user's patient reports and the radiologist's question.
        Track session history and updates.

        Args:
            user_id (str): The ID of the user.
            patient_id (str): The ID of the patient.
            question (str): The radiologist's question.

        Returns:
            str: The AI-generated response.
    """
    session_id = get_current_session_id(user_id, patient_id)
    conversation_history = get_conversation_history(user_id, patient_id, session_id)

    session_ref = db.collection('Users').document(user_id).collection('patients').document(patient_id).collection('conversations').document(session_id)
    session_data = session_ref.get().to_dict()

    reports_included = session_data.get('reports_included', False)
    last_fetch_time = session_data.get('last_fetch_time')

    if not reports_included:
        # Include all reports in the initial session
        reports = get_patient_radiology_reports(patient_id)
        if not reports:
            return "No reports found for this patient."
        prompt = create_flexible_prompt(patient_id, question, reports)
        session_ref.update({'reports_included': True, 'last_fetch_time': firestore.SERVER_TIMESTAMP})
        logging.info(f"Included the following patient reports in the prompt:\n{reports}")
    else:
        # Check for new reports since the last fetch time
        new_reports = get_patient_radiology_reports(patient_id, last_fetch_time)
        if new_reports:
            logging.info(f"New reports found: {new_reports}")
            all_reports = get_patient_radiology_reports(patient_id)
            prompt = create_flexible_prompt(patient_id, question, all_reports)
            session_ref.update({'last_fetch_time': firestore.SERVER_TIMESTAMP})
        else:
            logging.info("No new reports found.")
            prompt = question

    conversation_history.append({"role": "user", "content": prompt})

    completion = client.chat.completions.create(
        model="writer/palmyra-med-70b-32k",
        messages=conversation_history,
        temperature=0.4,
        top_p=0.7,
        max_tokens=1500,
        stream=False
    )

    response = completion.choices[0].message.content
    logging.info(f"AI Response:\n{response}")

    save_message_to_session(user_id, patient_id, session_id, "user", prompt)
    save_message_to_session(user_id, patient_id, session_id, "assistant", response)

    return response
