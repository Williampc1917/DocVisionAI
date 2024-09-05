package com.example.docvisionai.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import com.google.cloud.Timestamp;



/**
 * Service class for handling Firestore database operations, including saving patient data, searching for patients,
 * and retrieving patient reports and specific report data.
 */
@Service
public class FirestoreService {

    private static final Logger logger = Logger.getLogger(FirestoreService.class.getName());

    @Autowired
    private Firestore firestore;



    /**
     * Saves a patient's data to the Firestore database.
     *
     * @param patient A Map containing patient data, including patientId and other relevant fields.
     * @return A Map with a status of "success" and the timestamp of when the document was updated.
     * @throws ExecutionException   If the Firestore operation fails.
     * @throws InterruptedException If the Firestore operation is interrupted.
     */
    public Map<String, Object> savePatient(Map<String, Object> patient) throws ExecutionException, InterruptedException {
        String patientId = (String) patient.get("patientId");
        logger.info("Saving patient with patientId: " + patientId);
        WriteResult writeResult = firestore.collection("Patients").document(patientId).set(patient).get();
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("updateTime", writeResult.getUpdateTime());
        return response;
    }



    /**
     * Searches for a patient in Firestore by patientId.
     *
     * @param patientId The unique identifier of the patient to be searched.
     * @return A Map containing the patient's data if found, or null if no patient matches the patientId.
     * @throws ExecutionException   If the Firestore operation fails.
     * @throws InterruptedException If the Firestore operation is interrupted.
     */
    public Map<String, Object> searchPatientById(String patientId) throws ExecutionException, InterruptedException {
        ApiFuture<QuerySnapshot> future = firestore.collection("Patients")
                .whereEqualTo("patientId", patientId)
                .get();
        QuerySnapshot snapshot = future.get();
        if (!snapshot.isEmpty()) {
            DocumentSnapshot document = snapshot.getDocuments().get(0);
            return filterPatientData(document.getData());
        }
        return null;
    }



    /**
     * Searches for a patient in Firestore by their full name.
     *
     * @param fullName The full name of the patient to be searched.
     * @return A Map containing the patient's data if found, or null if no patient matches the fullName.
     * @throws ExecutionException   If the Firestore operation fails.
     * @throws InterruptedException If the Firestore operation is interrupted.
     */
    public Map<String, Object> searchPatientByName(String fullName) throws ExecutionException, InterruptedException {
        ApiFuture<QuerySnapshot> future = firestore.collection("Patients")
                .whereEqualTo("fullName", fullName)
                .get();
        QuerySnapshot snapshot = future.get();
        if (!snapshot.isEmpty()) {
            DocumentSnapshot document = snapshot.getDocuments().get(0);
            return filterPatientData(document.getData());
        }
        return null;
    }



    /**
     * Retrieves all radiology reports associated with a specific patient, ordered by report date.
     *
     * @param patientId The unique identifier of the patient whose reports are being retrieved.
     * @return A list of Maps, each containing data for one of the patient's reports.
     * @throws ExecutionException   If the Firestore operation fails.
     * @throws InterruptedException If the Firestore operation is interrupted.
     */
    public List<Map<String, Object>> getPatientReports(String patientId) throws ExecutionException, InterruptedException {
        List<Map<String, Object>> reportsList = new ArrayList<>();

        // Firestore Query: Accessing the 'RadiologyReports' subcollection within a specific patient's document
        ApiFuture<QuerySnapshot> future = firestore.collection("Patients")  // Access the 'Patients' collection
                .document(patientId)  // Get the document corresponding to the specific patientId
                .collection("RadiologyReports")  // Access the 'RadiologyReports' subcollection within that document
                .orderBy("reportDate", Query.Direction.DESCENDING)  // Order by 'reportDate'
                .get();

        // Process the results from the query
        QuerySnapshot snapshot = future.get();
        if (!snapshot.isEmpty()) {
            for (DocumentSnapshot document : snapshot.getDocuments()) {
                //System.out.println("Document Data: " + document.getData());  // Log the document data
                Map<String, Object> reportData = filterReportData(document.getData());
                reportsList.add(reportData);
            }
        } else {
            System.out.println("No reports found for patient: " + patientId);
        }

        return reportsList;
    }



    /**
     * Retrieves the data for a specific radiology report associated with a patient.
     *
     * @param patientId  The unique identifier of the patient whose report is being retrieved.
     * @param reportName The name or ID of the report to be retrieved.
     * @return A Map containing the data for the specified report.
     * @throws ExecutionException        If the Firestore operation fails.
     * @throws InterruptedException      If the Firestore operation is interrupted.
     * @throws NoSuchElementException    If no report is found with the specified reportName.
     */
    public Map<String, Object> getReportData(String patientId, String reportName) throws ExecutionException, InterruptedException {
        DocumentReference reportDocRef = firestore.collection("Patients")
                .document(patientId)
                .collection("RadiologyReports")
                .document(reportName);

        DocumentSnapshot document = reportDocRef.get().get();

        if (document.exists()) {
            return document.getData();  // Returning the report data as a map
        } else {
            logger.warning("No report found with reportName: " + reportName);
            throw new NoSuchElementException("Report not found.");
        }
    }



    /**
     * Checks if preliminary findings exist for a specific patient report.
     * If the "preliminaryFindingsSaved" flag is set to true, returns the relevant preliminary findings.
     * Otherwise, returns a response with "exists: false".
     *
     * @param patientId The unique identifier of the patient.
     * @param reportId  The unique identifier of the report.
     * @return A map containing the preliminary findings if they exist, or "exists: false" if not.
     */

    public Map<String, Object> checkPreliminaryFindings(String patientId, String reportId) {
        try {
            logger.info("Checking preliminary findings for patientId: " + patientId + ", reportId: " + reportId);

            // Reference to the document in Firestore
            DocumentReference reportRef = firestore.collection("Patients")
                    .document(patientId)
                    .collection("InProgressReports")
                    .document(reportId);

            // Retrieve the document
            DocumentSnapshot document = reportRef.get().get();

            // Check if document exists
            if (document.exists()) {
                logger.info("Document found for reportId: " + reportId);

                // Check the preliminaryFindingsSaved flag
                Boolean preliminaryFindingsSaved = document.getBoolean("preliminaryFindingsSaved");
                logger.info("preliminaryFindingsSaved flag is: " + preliminaryFindingsSaved);

                if (preliminaryFindingsSaved != null && preliminaryFindingsSaved) {
                    // If true, return the necessary data fields
                    Map<String, Object> data = new HashMap<>();
                    data.put("exists", true);
                    data.put("studyDateTime", document.getString("studyDateTime"));
                    data.put("keyFindings", document.getString("keyFindings"));
                    data.put("relevantHistory", document.getString("relevantHistory"));
                    data.put("concerns", document.getString("concerns"));
                    data.put("suggestions", document.getString("suggestions"));
                    data.put("radiologistNotes", document.getString("radiologistNotes"));

                    logger.info("Preliminary findings data: " + data.toString());

                    return data;
                }
            } else {
                logger.info("No document found for reportId: " + reportId);
            }

            // If the flag is false or the document does not exist, return exists: false
            Map<String, Object> response = new HashMap<>();
            response.put("exists", false);
            logger.info("Returning exists: false");
            return response;

        } catch (Exception e) {
            logger.severe("Error checking preliminary findings: " + e.getMessage());
            throw new RuntimeException("Error checking preliminary findings", e);
        }
    }



    /**
     * Saves preliminary findings for a specific patient report, setting the "preliminaryFindingsSaved" flag to true.
     *
     * @param patientId The unique identifier of the patient.
     * @param reportId  The unique identifier of the report.
     * @param findings  A map containing the preliminary findings to be saved.
     * @throws ExecutionException   If the Firestore operation fails.
     * @throws InterruptedException If the Firestore operation is interrupted.
     */
    public void savePreliminaryFindings(String patientId, String reportId, Map<String, Object> findings) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection("Patients")
                .document(patientId)
                .collection("InProgressReports")
                .document(reportId);

        // Set the 'preliminaryFindingsSaved' flag to true
        findings.put("preliminaryFindingsSaved", true);

        // Update the in-progress report with preliminary findings and the flag
        ApiFuture<WriteResult> writeResult = docRef.update(findings);
        logger.info("Preliminary findings saved at: " + writeResult.get().getUpdateTime());
    }



    /**
     * Retrieves the preliminary findings for a specific patient report.
     * If the report exists, returns the relevant preliminary findings.
     *
     * @param patientId The unique identifier of the patient.
     * @param reportId  The unique identifier of the report.
     * @return A map containing the preliminary findings, or null if the report does not exist.
     * @throws ExecutionException   If the Firestore operation fails.
     * @throws InterruptedException If the Firestore operation is interrupted.
     */
    public Map<String, Object> getPreliminaryFindings(String patientId, String reportId) throws ExecutionException, InterruptedException {
        try {
            // Construct the document path in Firestore
            DocumentReference docRef = firestore.collection("Patients")
                    .document(patientId)
                    .collection("InProgressReports")
                    .document(reportId);

            // Fetch the document snapshot
            ApiFuture<DocumentSnapshot> future = docRef.get();
            DocumentSnapshot document = future.get();

            if (document.exists()) {
                // Extract only the fields related to preliminary findings
                Map<String, Object> findings = new HashMap<>();
                findings.put("studyDateTime", document.getString("studyDateTime"));
                findings.put("keyFindings", document.getString("keyFindings"));
                findings.put("relevantHistory", document.getString("relevantHistory"));
                findings.put("concerns", document.getString("concerns"));
                findings.put("suggestions", document.getString("suggestions"));
                findings.put("radiologistNotes", document.getString("radiologistNotes"));

                return findings;
            } else {
                logger.warning("Preliminary findings not found for patientId: " + patientId + ", reportId: " + reportId);
                return null;
            }
        } catch (Exception e) {
            logger.severe("Failed to retrieve preliminary findings: " + e.getMessage());
            throw e;
        }
    }



    /**
     * Saves a finalized patient report to Firestore in the "RadiologyReports" collection.
     *
     * @param patientId   The unique identifier of the patient.
     * @param reportId    The unique identifier of the report.
     * @param reportData  A map containing the report data to be saved.
     * @throws ExecutionException   If the Firestore operation fails.
     * @throws InterruptedException If the Firestore operation is interrupted.
     */
    public void saveReport(String patientId, String reportId, Map<String, Object> reportData) throws ExecutionException, InterruptedException {
        DocumentReference patientDocRef = firestore.collection("Patients").document(patientId);
        CollectionReference reportsCollection = patientDocRef.collection("RadiologyReports");
        DocumentReference reportDocRef = reportsCollection.document(reportId); // Use reportId as document name

        // Add Firestore Timestamp to reportData
        reportData.put("created_at", Timestamp.now());

        ApiFuture<WriteResult> writeResult = reportDocRef.set(reportData);
        System.out.println("Report saved with ID: " + reportId + " at " + writeResult.get().getUpdateTime());
    }



    /**
     * Saves an in-progress report for a patient in Firestore. A unique report ID is generated and stored
     * in the "InProgressReports" subcollection for the patient, and the report reference is also added
     * to the user's "inProgressReports" array in their document.
     *
     * @param patientId      The unique identifier of the patient.
     * @param patientName    The name of the patient.
     * @param reportStartDate The start date of the report.
     * @param radiologistName The name of the radiologist creating the report.
     * @param studyType      The type of study (e.g., X-ray, MRI).
     * @param sessionNotes   Notes about the session.
     * @param userId         The unique identifier of the user (radiologist).
     * @return The unique report ID of the newly created in-progress report.
     * @throws ExecutionException   If the Firestore operation fails.
     * @throws InterruptedException If the Firestore operation is interrupted.
     */
    public String saveInProgressReport(String patientId, String patientName, String reportStartDate, String radiologistName, String studyType, String sessionNotes, String userId) throws ExecutionException, InterruptedException {
        // Generate a new report ID
        String reportId = UUID.randomUUID().toString();

        // Create a reference to the InProgressReports collection for the given patient
        CollectionReference reportsRef = firestore.collection("Patients").document(patientId).collection("InProgressReports");

        // Create the report data
        Map<String, Object> reportData = new HashMap<>();
        reportData.put("reportId", reportId);
        reportData.put("patientId", patientId);
        reportData.put("patientName", patientName); // Add the patientName to the report data
        reportData.put("reportStartDate", reportStartDate);
        reportData.put("radiologistName", radiologistName);
        reportData.put("studyType", studyType);
        reportData.put("sessionNotes", sessionNotes);
        reportData.put("userId", userId); // Add the userId to the report data
        reportData.put("createdAt", System.currentTimeMillis()); // Optionally track when the report was created
        reportData.put("status", "in_progress");

        // Add the binary flag for preliminary findings (set to false initially)
        reportData.put("preliminaryFindingsSaved", false);

        // Save the report data to Firestore
        ApiFuture<WriteResult> result = reportsRef.document(reportId).set(reportData);

        // Create a reference to the user's document
        DocumentReference userRef = firestore.collection("Users").document(userId);

        // Create a map to hold the patientId and reportId pair
        Map<String, String> reportReference = new HashMap<>();
        reportReference.put("reportId", reportId);
        reportReference.put("patientId", patientId);

        // Update the user's document to include this report reference in the inProgressReports array
        userRef.update("inProgressReports", FieldValue.arrayUnion(reportReference));

        // Return the report ID
        return reportId;
    }



    /**
     * Finalizes an in-progress report by deleting it from the "InProgressReports" subcollection for the patient.
     * Also removes the report reference from the user's "inProgressReports" array.
     *
     * @param patientId The unique identifier of the patient.
     * @param reportId  The unique identifier of the report to be finalized.
     * @param userId    The unique identifier of the user (radiologist).
     * @throws ExecutionException   If the Firestore operation fails.
     * @throws InterruptedException If the Firestore operation is interrupted.
     */
    public void finalizeInProgressReport(String patientId, String reportId, String userId) throws ExecutionException, InterruptedException {
        // Reference to the in-progress report document
        DocumentReference inProgressDocRef = firestore.collection("Patients")
                .document(patientId)
                .collection("InProgressReports")
                .document(reportId);

        // Delete the in-progress report from Firestore
        ApiFuture<WriteResult> writeResult = inProgressDocRef.delete();
        logger.info("In-progress report with ID: " + reportId + " has been deleted at " + writeResult.get().getUpdateTime());

        // Reference to the user's document
        DocumentReference userDocRef = firestore.collection("Users").document(userId);

        // Create the map object that needs to be removed from the array
        Map<String, String> reportReference = new HashMap<>();
        reportReference.put("reportId", reportId);
        reportReference.put("patientId", patientId);

        // Remove the specific reportReference map from the inProgressReports array
        ApiFuture<WriteResult> userUpdateResult = userDocRef.update("inProgressReports", FieldValue.arrayRemove(reportReference));
        logger.info("Report ID: " + reportId + " with patient ID: " + patientId + " has been removed from the user's inProgressReports array.");

        // Wait for both operations to complete (optional, but ensures consistency)
        writeResult.get();
        userUpdateResult.get();
    }



    /**
     * Saves a new user profile or updates an existing one in the "Users" collection in Firestore.
     *
     * @param user A map containing user data such as userId, fullName, jobTitle, etc.
     * @return A map containing a status message and the timestamp of the update.
     * @throws ExecutionException   If the Firestore operation fails.
     * @throws InterruptedException If the Firestore operation is interrupted.
     */
    public Map<String, Object> saveUser(Map<String, Object> user) throws ExecutionException, InterruptedException {
        String userId = (String) user.get("userId");
        WriteResult writeResult = firestore.collection("Users").document(userId).set(user).get();
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("updateTime", writeResult.getUpdateTime());
        return response;
    }



    /**
     * Checks if a user exists in the "Users" collection in Firestore by userId.
     *
     * @param userId The unique identifier of the user.
     * @return true if the user exists, false otherwise.
     * @throws ExecutionException   If the Firestore operation fails.
     * @throws InterruptedException If the Firestore operation is interrupted.
     */
    public boolean checkUserExists(String userId) throws ExecutionException, InterruptedException {
        DocumentSnapshot document = firestore.collection("Users").document(userId).get().get();
        return document.exists();
    }



    /**
     * Retrieves a list of pending (in-progress) reports for a specific user by their userId.
     * The reports are stored in the "inProgressReports" array field in the user's document in Firestore.
     *
     * @param userId The unique identifier of the user (radiologist).
     * @return A list of maps, each containing the details of an in-progress report.
     */
    public List<Map<String, Object>> getPendingReportsByUserId(String userId) {
        List<Map<String, Object>> pendingReports = new ArrayList<>();

        try {
            // Fetch user document
            DocumentReference userDocRef = firestore.collection("Users").document(userId);
            ApiFuture<DocumentSnapshot> future = userDocRef.get();
            DocumentSnapshot document = future.get();

            if (document.exists()) {
                // Get the inProgressReports array
                List<Map<String, String>> inProgressReports = (List<Map<String, String>>) document.get("inProgressReports");

                // Iterate through inProgressReports and fetch each report
                for (Map<String, String> reportInfo : inProgressReports) {
                    String patientId = reportInfo.get("patientId");
                    String reportId = reportInfo.get("reportId");

                    // Fetch the report from Patients collection
                    DocumentReference reportDocRef = firestore.collection("Patients")
                            .document(patientId)
                            .collection("InProgressReports")
                            .document(reportId);

                    ApiFuture<DocumentSnapshot> reportFuture = reportDocRef.get();
                    DocumentSnapshot reportDocument = reportFuture.get();

                    if (reportDocument.exists()) {
                        // Create a map to store report details
                        Map<String, Object> reportData = new HashMap<>();
                        reportData.put("reportId", reportId); // Include reportId in the response
                        reportData.put("patientName", reportDocument.getString("patientName"));
                        reportData.put("patientId", patientId);
                        reportData.put("reportStartDate", reportDocument.getString("reportStartDate"));
                        reportData.put("studyType", reportDocument.getString("studyType"));

                        // Add to the list of pending reports
                        pendingReports.add(reportData);
                    }
                }
            }
        } catch (Exception e) {
            logger.severe("Error fetching pending reports: " + e.getMessage());
        }

        return pendingReports;
    }



    /**
     * Filters and extracts specific patient-related fields from the provided data map.
     * This method is used to ensure that only relevant patient information is returned.
     *
     * @param data A map containing the full set of patient data.
     * @return A filtered map containing only essential patient fields (patientId, fullName, dob, gender, contactInfo).
     */
    private Map<String, Object> filterPatientData(Map<String, Object> data) {
        Map<String, Object> filteredData = new HashMap<>();
        filteredData.put("patientId", data.get("patientId"));
        filteredData.put("fullName", data.get("fullName"));
        filteredData.put("dob", data.get("dob"));
        filteredData.put("gender", data.get("gender"));
        filteredData.put("contactInfo", data.get("contactInfo"));
        return filteredData;
    }



    /**
     * Filters and extracts specific report-related fields from the provided data map.
     * This method ensures that only relevant report information is returned, such as report ID, date, and study type.
     *
     * @param data A map containing the full set of report data.
     * @return A filtered map containing key report fields (reportId, reportDate, typeOfStudy).
     */
    private Map<String, Object> filterReportData(Map<String, Object> data) {
        Map<String, Object> filteredData = new HashMap<>();
        filteredData.put("reportId", data.get("reportName"));  // 'reportName' for unique report ID
        filteredData.put("reportDate", data.get("reportDate"));  // Use 'reportDate' for the date
        filteredData.put("typeOfStudy", data.get("typeOfStudy"));  // Use 'typeOfStudy' for the type of study
        // Add other fields as necessary
        return filteredData;
    }

















}

