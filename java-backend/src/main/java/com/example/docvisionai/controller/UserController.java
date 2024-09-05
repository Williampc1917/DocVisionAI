package com.example.docvisionai.controller;
import com.example.docvisionai.service.FirestoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.HashMap;



@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private FirestoreService firestoreService;



    /**
     * Endpoint to save a user profile to Firestore.
     *
     * @param user A JSON object containing the following user details:
     *             {
     *               "userId": "user123",                // The unique identifier of the user
     *               "fullName": "Dr. John Doe",         // The full name of the user
     *               "jobTitle": "Radiologist",          // The job title of the user
     *               "specialty": "Pulmonology",         // The specialty of the user
     *               "institution": "City Hospital",     // The institution where the user works
     *               "licenseNumber": 123456,            // The user's professional license number
     *               "reportTemplate": "Standard Report Template" // The report template preferred by the user
     *             }
     * @return A Map containing a success message and the timestamp of the update on success:
     *         {
     *           "status": "success",
     *           "updateTime": "2024-09-04T12:00:00Z"
     *         }
     *         On failure, an HTTP 500 Internal Server Error is returned with:
     *         {
     *           "status": "error",
     *           "message": "An error occurred while saving the user profile."
     *         }
     * @throws ExecutionException If there is an error executing the Firestore operation.
     * @throws InterruptedException If the Firestore operation is interrupted.
     */
    @PostMapping("/saveUser")
    public Map<String, Object> saveUser(@RequestBody Map<String, Object> user) throws ExecutionException, InterruptedException {
        return firestoreService.saveUser(user);
    }



    /**
     * Endpoint to check if a user exists in the Firestore database by user ID.
     *
     * @param userId The unique identifier of the user to be checked, passed as a path variable.
     * @return A Map containing a boolean field "exists" indicating whether the user exists in the database:
     *         {
     *           "exists": true  // or false if the user is not found
     *         }
     *         On failure, an HTTP 500 Internal Server Error will be returned with:
     *         {
     *           "status": "error",
     *           "message": "An error occurred while checking the user profile."
     *         }
     * @throws ExecutionException If there is an error executing the Firestore operation.
     * @throws InterruptedException If the Firestore operation is interrupted.
     */
    @GetMapping("/checkUser/{userId}")
    public Map<String, Object> checkUser(@PathVariable String userId) throws ExecutionException, InterruptedException {
        Map<String, Object> response = new HashMap<>();
        boolean exists = firestoreService.checkUserExists(userId);
        response.put("exists", exists);
        return response;
    }



    /**
     * Endpoint to retrieve a list of pending reports for a specific user by user ID.
     *
     * @param userId The unique identifier of the user whose pending reports are being retrieved.
     * @return A ResponseEntity containing a list of pending reports in the following format on success:
     *         [
     *           {
     *             "reportId": "report_001",
     *             "patientName": "John Doe",
     *             "patientId": "patient_001",
     *             "reportStartDate": "2024-09-04",
     *             "studyType": "Chest X-Ray"
     *           },
     *           {
     *             "reportId": "report_002",
     *             "patientName": "Jane Smith",
     *             "patientId": "patient_002",
     *             "reportStartDate": "2024-09-01",
     *             "studyType": "MRI"
     *           }
     *         ]
     *         If an error occurs, an HTTP 500 Internal Server Error is returned with:
     *         {
     *           "status": "error",
     *           "message": "An error occurred while fetching pending reports."
     *         }
     * @throws Exception If there is an error during the Firestore operation.
     */
    @GetMapping("/pendingReports")
        public ResponseEntity<List<Map<String, Object>>> getPendingReports(@RequestParam String userId) {
            List<Map<String, Object>> pendingReports = firestoreService.getPendingReportsByUserId(userId);
            return ResponseEntity.ok(pendingReports);
        }

}


