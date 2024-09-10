package com.example.docvisionai.controller;
import com.example.docvisionai.service.FirestoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
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



    /**
     * Endpoint to add a new task for a user.
     *
     * @param task A JSON object containing the userId, task title, and completed status.
     * @return Response indicating success or failure.
     */
    @PostMapping("/tasks")
    public ResponseEntity<Map<String, Object>> addTask(@RequestBody Map<String, Object> task) {
        try {
            String taskId = firestoreService.saveUserTask(task); // Save the task and get taskId
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("taskId", taskId); // Include taskId in the response
            response.put("title", task.get("title")); // Include the title
            response.put("completed", task.get("completed")); // Include completed status
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to save task.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }



    /**
     * Endpoint to fetch tasks for a specific user by userId.
     *
     * @param userId The unique ID of the user whose tasks are being fetched.
     * @return A list of tasks for the user.
     */
    @GetMapping("/{userId}/tasks")
    public ResponseEntity<List<Map<String, Object>>> getUserTasks(@PathVariable String userId) {
        try {
            List<Map<String, Object>> tasks = firestoreService.getUserTasks(userId);
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to fetch tasks.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonList(response));
        }
    }



    /**
     * Endpoint to retrieve a user profile from Firestore based on userId.
     *
     * @param userId The unique identifier of the user.
     * @return A Map containing the user's profile data or an error message if not found.
     * @throws ExecutionException If there is an error executing the Firestore operation.
     * @throws InterruptedException If the Firestore operation is interrupted.
     */
    @GetMapping("/profile/{userId}")
    public ResponseEntity<Map<String, Object>> getUserProfile(@PathVariable String userId) throws ExecutionException, InterruptedException {
        Map<String, Object> userProfile = firestoreService.searchUserById(userId);
        if (userProfile != null) {
            return ResponseEntity.ok(userProfile);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Collections.singletonMap("message", "User not found"));
        }
    }



    /**
     * Endpoint to update user profile with partial updates.
     * @param userId - The ID of the user whose profile needs to be updated.
     * @param updatedFields - A map of updated fields from the frontend.
     * @return A response indicating the success or failure of the update operation.
     */
    @PutMapping("/profile/{userId}")
    public ResponseEntity<Map<String, Object>> updateUserProfile(
            @PathVariable String userId,
            @RequestBody Map<String, Object> updatedFields) {

        try {
            // Retrieve the existing profile
            Map<String, Object> existingProfile = firestoreService.getUserProfile(userId);

            if (existingProfile == null) {
                // If the user profile is not found, return a 404 error
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Collections.singletonMap("message", "User not found"));
            }

            // Merge the updated fields with the existing profile
            for (Map.Entry<String, Object> entry : updatedFields.entrySet()) {
                existingProfile.put(entry.getKey(), entry.getValue());
            }

            // Save the updated profile
            firestoreService.saveUserProfile(userId, existingProfile);

            // Return success response
            return ResponseEntity.ok(Collections.singletonMap("status", "Profile updated successfully"));
        } catch (Exception e) {
            // Handle exceptions and return a 500 error if something goes wrong
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("message", "An error occurred while updating the profile."));
        }
    }

    /**
     * Endpoint to mark a task as completed for a given user.
     *
     * @param userId - The ID of the user whose task needs to be updated.
     * @param taskId - The ID of the task to be marked as done.
     * @param taskUpdate - A Map containing the updated task data (in this case, the 'completed' field).
     * @return A ResponseEntity with a success or error message.
     */
    @PutMapping("/{userId}/tasks/{taskId}")
    public ResponseEntity<Map<String, Object>> markTaskAsDone(
            @PathVariable String userId,
            @PathVariable String taskId,
            @RequestBody Map<String, Object> taskUpdate) {

        try {
            // Update the task's completed status
            firestoreService.updateUserTask(userId, taskId, taskUpdate);

            // Return success response
            return ResponseEntity.ok(Collections.singletonMap("status", "Task marked as done successfully"));

        } catch (Exception e) {
            // Handle any exceptions and return error message
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("message", "An error occurred while updating the task"));
        }
    }


    /**
     * A simple health check endpoint to verify if the service is running.
     *
     * @return A message indicating that the service is up and running.
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Service is running!");
    }



}


