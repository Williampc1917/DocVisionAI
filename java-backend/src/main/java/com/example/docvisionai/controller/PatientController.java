package com.example.docvisionai.controller;

import com.example.docvisionai.service.FirestoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;



/**
 * REST controller for managing patient-related operations.
 * This class provides endpoints for saving and searching patients/reports in Firestore.
 */
@RestController
@RequestMapping("/api/patient")
public class PatientController {

    private static final Logger logger = Logger.getLogger(PatientController.class.getName());

    @Autowired
    private FirestoreService firestoreService;



    /**
     * Endpoint to save a new patient information to Firestore.
     *
     * @param patient A map containing patient data (e.g., PatientId, fullName, dob, gender, contactInfo, knownConditions, previousTreatments).
     * @return A ResponseEntity containing the response status and message.
     * @throws ExecutionException if there's an issue executing the Firestore operation.
     * @throws InterruptedException if the Firestore operation is interrupted.
     */
    @PostMapping("/savePatient")
    public ResponseEntity<Map<String, Object>> savePatient(@RequestBody Map<String, Object> patient) {
        Map<String, Object> response = new HashMap<>();
        try {
            logger.info("Received patient info: " + patient);
            response = firestoreService.savePatient(patient);
            return ResponseEntity.ok(response); // Return 200 OK status with the response
        } catch (ExecutionException | InterruptedException e) {
            logger.severe("Error saving patient info: " + e.getMessage());
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            logger.severe("Unexpected error: " + e.getMessage());
            response.put("status", "error");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }



    /**
     * Endpoint to search for a patient by patient ID or full name.
     * Only one of the parameters should be provided; if both are provided, patientId takes precedence.
     *
     * @param patientId Optional: The unique ID of the patient.
     * @param fullName Optional: The full name of the patient.
     * @return A ResponseEntity containing a patient object in the following format:
     *         {
     *           "gender": "Male",
     *           "contactInfo": "john.doe@example.com",
     *           "patientId": "12345",
     *           "dob": "1990-01-01",
     *           "fullName": "John Doe"
     *         }
     *         If no patient is found, a 404 status is returned.
     *         On server errors, a 500 status is returned.
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchPatient(@RequestParam(required = false) String patientId,
                                                             @RequestParam(required = false) String fullName) {
        try {
            Map<String, Object> result = null;

            // Search by patientId if provided
            if (patientId != null && !patientId.isEmpty()) {
                result = firestoreService.searchPatientById(patientId);
            }
            // If patientId is not provided, search by fullName
            else if (fullName != null && !fullName.isEmpty()) {
                result = firestoreService.searchPatientByName(fullName);
            }

            // Return the result if found, otherwise return 404
            if (result != null) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.severe("Error searching for patient: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }



    /**
     * Endpoint to retrieve all reports for a specific patient from Firestore.
     *
     * @param patientId The unique identifier of the patient whose reports are being retrieved.
     * @return A ResponseEntity containing a list of reports in the following format:
     *         [
     *           {
     *             "reportId": "9392493e-894e-4d54-8eaf-b41fc37b4a35",
     *             "reportDate": "09/04/2024",
     *             "typeOfStudy": "Chest X-ray"
     *           },
     *           ...
     *         ]
     *         If no reports are found, returns an empty list with HTTP 200 status.
     *         If an error occurs during retrieval, returns HTTP 500 status.
     * @throws Exception if there is an issue fetching reports from Firestore.
     */
    @GetMapping("/reports")
    public ResponseEntity<List<Map<String, Object>>> getPatientReports(@RequestParam String patientId) {
        try {
            List<Map<String, Object>> reports = firestoreService.getPatientReports(patientId);

            if (!reports.isEmpty()) {
                return ResponseEntity.ok(reports);
            } else {
                // Return an empty list with HTTP 200 status instead of 404
                return ResponseEntity.ok(Collections.emptyList());
            }
        } catch (Exception e) {
            logger.severe("Error fetching patient reports: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }



    /**
     * Endpoint to retrieve a specific radiology report for a patient based on patient ID and report name.
     *
     * @param patientId The unique identifier of the patient whose report is being fetched.
     * @param reportName The name or ID of the specific report to be retrieved.
     * @return A ResponseEntity containing the report data in the following format:
     *         {
     *           "leftLung": "Mild diffuse patchy opacities in the lower lobe, less pronounced than the right lung...",
     *           "dateOfStudy": "09/01/2024",
     *           "patientName": "John Doe",
     *           "reportId": "9392493e-894e-4d54-8eaf-b41fc37b4a35",
     *           "reportName": "9392493e-894e-4d54-8eaf-b41fc37b4a35",
     *           "patientID": "12345",
     *           "numberOfViews": "",
     *           "airways": "trachea is midline, no significant bronchial obstruction visualized.",
     *           "created_at": { "seconds": 1725480726, "nanos": 101000000 },
     *           "technique": "Posteroanterior (PA) and Lateral views, Digital X-ray",
     *           "radiologistName": "William Pineda",
     *           "rightLung": "Prominent consolidation in the lower lobe...",
     *           "patientDOB": "1990-01-01",
     *           "radiologistTitle": "Chief Radiologist",
     *           "imageFolderUrls": [
     *             "gs://reference-node-426102-r4.appspot.com/12345/...",
     *             "gs://reference-node-426102-r4.appspot.com/12345/..."
     *           ],
     *           "reportDate": "09/04/2024",
     *           "clinicalHistory": "Patient presents with a 7-day history of persistent cough...",
     *           "impression": "Findings are consistent with bilateral lower lobe pneumonia...",
     *           "lungs": "Bilateral patchy opacities seen in the lower lobes...",
     *           "typeOfStudy": "Chest X-ray",
     *           "pleura": "Pleural spaces are clear with no evidence of pneumothorax or pleural effusion."
     *         }
     *         If the report is successfully retrieved, an HTTP 200 status is returned.
     *         If an error occurs during retrieval, an HTTP 500 status is returned with an error message.
     */
    @GetMapping("/getReport")
    public ResponseEntity<Map<String, Object>> getReport(
            @RequestParam String patientId,
            @RequestParam String reportName) {
        try {
            Map<String, Object> reportData = firestoreService.getReportData(patientId, reportName);
            return ResponseEntity.ok(reportData);
        } catch (Exception e) {
            logger.severe("Error fetching report data: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", e.getMessage()));
        }
    }



    /**
     * Endpoint to check if preliminary findings exist for a specific report of a patient.
     *
     * @param patientId The unique identifier of the patient whose preliminary findings are being checked.
     * @param reportId The unique identifier of the specific report for which to check the preliminary findings.
     * @return A ResponseEntity containing a JSON object with the following structure:
     *         {
     *           "radiologistNotes": "N/A",
     *           "keyFindings": "N/A",
     *           "exists": true,  // Indicates if preliminary findings exist
     *           "studyDateTime": "",
     *           "suggestions": "Current examination should focus on...",
     *           "concerns": "Concerns of bilateral lower lobe pneumonia...",
     *           "relevantHistory": "N/A"
     *         }
     *         If preliminary findings are found, the response will include relevant data.
     *         If an error occurs during the process, an HTTP 500 status is returned with an error message.
     */
    @GetMapping("/checkPreliminaryFindings")
    public ResponseEntity<?> checkPreliminaryFindings(
            @RequestParam String patientId,
            @RequestParam String reportId) {
        try {
            logger.info("Received request to check preliminary findings for patientId: " + patientId + ", reportId: " + reportId);

            // Call the service method to check preliminary findings
            Map<String, Object> result = firestoreService.checkPreliminaryFindings(patientId, reportId);

            logger.info("Returning result to frontend: " + result.toString());

            // Return the result as a JSON response
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.severe("Error in checkPreliminaryFindings endpoint: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while checking preliminary findings");
        }
    }



    /**
     * Endpoint to save preliminary findings for a specific patient report.
     *
     * @param request A JSON object containing the following required fields:
     *                {
     *                  "patientId": "12345",  // The unique identifier of the patient
     *                  "reportId": "abc123",  // The unique identifier of the report
     *                  Other fields representing the preliminary findings to be saved
     *                }
     * @return A ResponseEntity containing a JSON response:
     *         {
     *           "status": "success"  // If the preliminary findings are successfully saved
     *         }
     *         On failure, returns an HTTP 500 status with a response:
     *         {
     *           "status": "error",
     *           "message": "An error occurred while saving the preliminary findings."
     *         }
     * @throws Exception If there is an error during the save operation, an internal server error (500) is returned.
     */
    @PostMapping("/savePreliminaryFindings")
    public ResponseEntity<Map<String, Object>> savePreliminaryFindings(@RequestBody Map<String, Object> request) {
        try {
            String patientId = (String) request.get("patientId");
            String reportId = (String) request.get("reportId");

            // Remove patientId and reportId from the request map before saving the findings
            request.remove("patientId");
            request.remove("reportId");

            // Pass the remaining map to the service method to save the findings
            firestoreService.savePreliminaryFindings(patientId, reportId, request);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.severe("Error saving preliminary findings: " + e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }



    /**
     * Retrieves preliminary findings for a specific patient report from Firestore.
     *
     * @param patientId The unique identifier of the patient.
     * @param reportId The unique identifier of the report.
     * @return A Map containing the preliminary findings in the following format if the document exists:
     *         {
     *           "radiologistNotes": "N/A",
     *           "keyFindings": "N/A",
     *           "studyDateTime": "",
     *           "suggestions": "Current examination should focus on...",
     *           "concerns": "Concerns of bilateral lower lobe pneumonia...",
     *           "relevantHistory": "N/A"
     *         }
     *         If no findings are found, returns null and logs a warning.
     *         On server errors, an exception is thrown with a log message.
     * @throws ExecutionException if there is an issue executing the Firestore operation.
     * @throws InterruptedException if the Firestore operation is interrupted.
     */
    @GetMapping("/getPreliminaryFindings")
    public ResponseEntity<?> getPreliminaryFindings(@RequestParam String patientId, @RequestParam String reportId) {
        try {
            Map<String, Object> findings = firestoreService.getPreliminaryFindings(patientId, reportId);
            if (findings != null) {
                return ResponseEntity.ok(findings);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Preliminary findings not found for the specified report ID.");
            }
        } catch (Exception e) {
            logger.severe("An error occurred while fetching the preliminary findings: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching the preliminary findings: " + e.getMessage());
        }
    }



    /**
     * Endpoint to save a report for a specific patient in Firestore.
     *
     * @param patientId The unique identifier of the patient for whom the report is being saved.
     * @param reportId The unique identifier of the report to be saved.
     * @param reportData A Map containing the report details (e.g., findings, impressions, radiologist's notes).
     * @return A ResponseEntity containing a JSON response on success:
     *         {
     *           "status": "success"
     *         }
     *         If an error occurs during saving, an HTTP 500 Internal Server Error is returned with:
     *         {
     *           "status": "error",
     *           "message": "An error occurred while saving the report."
     *         }
     * @throws ExecutionException If an error occurs while executing the Firestore operation.
     * @throws InterruptedException If the Firestore operation is interrupted.
     */
    @PostMapping("/saveReport")
    public ResponseEntity<Map<String, Object>> saveReport(
            @RequestParam String patientId,
            @RequestParam String reportId, // Add reportId as a request parameter
            @RequestBody Map<String, Object> reportData) {
        try {
            firestoreService.saveReport(patientId, reportId, reportData); // Pass reportId to the FirestoreService
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (ExecutionException | InterruptedException e) {
            logger.severe("Error saving report: " + e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }



    /**
     * Endpoint to start a new analysis for a patient, saving the report as in-progress in Firestore.
     *
     * @param request A JSON object containing the following required fields:
     *                {
     *                  "patientId": "12345",               // The unique identifier of the patient
     *                  "patientName": "John Doe",          // The full name of the patient
     *                  "reportStartDate": "09/04/2024",    // The date the report analysis starts
     *                  "radiologistName": "Dr. Smith",     // The name of the radiologist performing the analysis
     *                  "studyType": "Chest X-ray",         // The type of study being conducted
     *                  "sessionNotes": "Initial findings", // Any session notes for the analysis
     *                  "userId": "user123"                // The ID of the user starting the analysis
     *                }
     * @return A ResponseEntity containing a JSON object with the following structure on success:
     *         {
     *           "status": "success",
     *           "reportId": "abc123-unique-report-id"
     *         }
     *         If an error occurs while starting the analysis, an HTTP 500 Internal Server Error is returned with:
     *         {
     *           "status": "error",
     *           "message": "An error occurred while starting the analysis."
     *         }
     * @throws Exception If there is an error during the process of saving the in-progress report.
     */
    @PostMapping("/startAnalysis")
    public ResponseEntity<Map<String, Object>> startAnalysis(@RequestBody Map<String, Object> request) {
        try {
            String patientId = (String) request.get("patientId");
            String patientName = (String) request.get("patientName"); // Get the patientName from the request
            String reportStartDate = (String) request.get("reportStartDate");
            String radiologistName = (String) request.get("radiologistName");
            String studyType = (String) request.get("studyType");
            String sessionNotes = (String) request.get("sessionNotes");
            String userId = (String) request.get("userId"); // Get the userId from the request

            // Pass these variables to your service to save the in-progress report
            String reportId = firestoreService.saveInProgressReport(patientId, patientName, reportStartDate, radiologistName, studyType, sessionNotes, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("reportId", reportId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.severe("Error starting analysis: " + e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }



    /**
     * Endpoint to finalize an in-progress report for a specific patient and remove it from the user's in-progress reports.
     *
     * @param requestData A JSON object containing the following required fields:
     *                    {
     *                      "patientId": "12345",     // The unique identifier of the patient
     *                      "reportId": "report_001", // The unique identifier of the report to be finalized
     *                      "userId": "user123"       // The ID of the user finalizing the report
     *                    }
     * @return A ResponseEntity containing a JSON object on success:
     *         {
     *           "status": "success"
     *         }
     *         If an error occurs while finalizing the report, an HTTP 500 Internal Server Error is returned with:
     *         {
     *           "status": "error",
     *           "message": "An error occurred while finalizing the report."
     *         }
     * @throws ExecutionException If there is an error during Firestore operations.
     * @throws InterruptedException If the Firestore operation is interrupted.
     */
    @PostMapping("/finalizeInProgressReport")
    public ResponseEntity<Map<String, Object>> finalizeInProgressReport(@RequestBody Map<String, String> requestData) {
        String patientId = requestData.get("patientId");
        String reportId = requestData.get("reportId");
        String userId = requestData.get("userId");  // Accept the userId

        try {
            // Finalize the in-progress report and remove it from the user's inProgressReports array
            firestoreService.finalizeInProgressReport(patientId, reportId, userId);

            // Prepare and return a success response
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (ExecutionException | InterruptedException e) {
            logger.severe("Error finalizing in-progress report: " + e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }



    /**
     * Endpoint to check if a patient exists in the Firestore database by patient ID.
     *
     * @param patientId The unique identifier of the patient to be checked.
     * @return A ResponseEntity containing a JSON object with the following structure on success:
     *         {
     *           "exists": true,
     *           "patientData": {
     *             "patientId": "12345",
     *             "patientName": "John Doe",
     *             "dob": "1980-05-20",
     *             "gender": "male",
     *             "contactInfo": "john.doe@example.com",
     *             "knownConditions": "Hypertension",
     *             "previousTreatments": "Blood pressure medication"
     *           }
     *         }
     *         If the patient is not found:
     *         {
     *           "exists": false
     *         }
     *         On server error, returns an HTTP 500 Internal Server Error.
     * @throws Exception If an error occurs while searching for the patient.
     */
    @GetMapping("/checkPatient")
    public ResponseEntity<Map<String, Object>> checkPatientExists(@RequestParam String patientId) {
        try {
            Map<String, Object> result = firestoreService.searchPatientById(patientId);
            if (result != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("exists", true);
                response.put("patientData", result); // Optional: return the found patient data
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("exists", false);
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
