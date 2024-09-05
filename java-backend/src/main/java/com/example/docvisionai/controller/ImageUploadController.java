package com.example.docvisionai.controller;

import com.example.docvisionai.service.FirebaseStorageService;
import com.google.firebase.cloud.StorageClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/uploads")
public class ImageUploadController {

    @Autowired
    private FirebaseStorageService firebaseStorageService;
    private static final Logger logger = Logger.getLogger(ImageUploadController.class.getName());



    /**
     * Endpoint to save original and heatmap images for a patient, along with generating a unique report.
     *
     * @param patientId The unique identifier of the patient for whom the images are being saved.
     * @param images A list of ImageData objects containing the original image and heatmap data encoded in Base64 format.
     * @return A ResponseEntity containing a JSON object with the following structure on success:
     *         {
     *           "reportName": "abc123-unique-report-name",
     *           "folderPaths": [
     *             "gs://bucket_name/12345/abc123-unique-report-name/abc1-image-id",
     *             "gs://bucket_name/12345/abc123-unique-report-name/abc2-image-id"
     *           ]
     *         }
     *         On failure, a 400 Bad Request response is returned with an error message:
     *         {
     *           "error": "An error occurred while saving images: [error message]"
     *         }
     *         On server errors, a 500 Internal Server Error is returned with:
     *         {
     *           "error": "An error occurred while processing the images."
     *         }
     * @throws Exception If an error occurs during image processing or upload.
     */
    @PostMapping("/save-images")
    public ResponseEntity<Map<String, Object>> saveImages(@RequestParam String patientId, @RequestBody List<ImageData> images) {
        try {
            logger.info("Received images data for patientId: " + patientId);

            // Generate a unique reportName (UUID)
            String reportName = UUID.randomUUID().toString();
            Map<String, Object> response = new HashMap<>();
            List<String> folderPaths = new ArrayList<>();  // Renamed from folderUrls to folderPaths

            for (ImageData imageData : images) {
                logger.info("Processing image: " + imageData.getCustomFileName());

                // Generate a unique identifier for the pair of images (pairUUID)
                String pairId = UUID.randomUUID().toString();

                byte[] originalImageBytes = Base64.getDecoder().decode(imageData.getOriginalImage());
                byte[] heatmapImageBytes = Base64.getDecoder().decode(imageData.getHeatmapData());

                // Upload the original image and heatmap image using the reportName and pairId
                String originalImageUrl = firebaseStorageService.uploadImage(patientId, reportName, pairId, originalImageBytes, imageData.getCustomFileName());
                String heatmapImageUrl = firebaseStorageService.uploadImage(patientId, reportName, pairId, heatmapImageBytes, imageData.getHeatmapFileName());

                // Construct the gs:// folder path correctly using the full path
                String folderPath = "gs://" + StorageClient.getInstance().bucket().getName() + "/" + patientId + "/" + reportName + "/" + pairId;

                if (!folderPaths.contains(folderPath)) {
                    folderPaths.add(folderPath);
                }
            }

            // Add the reportName and folder paths to the response
            response.put("reportName", reportName);
            response.put("folderPaths", folderPaths);  // Use folderPaths here

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.severe("Error saving images: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", e.getMessage()));
        }
    }



    /**
     * Endpoint to retrieve signed URLs for images stored in specific folder paths in Firebase Storage.
     *
     * @param folderPaths A list of folder paths in Firebase Storage from which to retrieve the image URLs.
     *                    Example:
     *                    [
     *                      "gs://bucket_name/patient123/report001/pair001",
     *                      "gs://bucket_name/patient123/report001/pair002"
     *                    ]
     * @return A ResponseEntity containing a map where the folder path is the key and the value is a list of signed image URLs.
     *         Example on success:
     *         {
     *           "gs://bucket_name/patient123/report001/pair001": [
     *             "https://storage.googleapis.com/signed_url_heatmap_001",
     *             "https://storage.googleapis.com/signed_url_original_001"
     *           ],
     *           "gs://bucket_name/patient123/report001/pair002": [
     *             "https://storage.googleapis.com/signed_url_heatmap_002",
     *             "https://storage.googleapis.com/signed_url_original_002"
     *           ]
     *         }
     *         On failure, returns a 400 Bad Request with an error message:
     *         {
     *           "error": ["An error occurred while fetching image URLs: [error message]"]
     *         }
     * @throws Exception If an error occurs while retrieving the image URLs from Firebase Storage.
     */
    // New method to get image URLs from folder paths
    @PostMapping("/getImageUrls")
    public ResponseEntity<Map<String, List<String>>> getImageUrls(@RequestBody List<String> folderPaths) {
        try {
            Map<String, List<String>> imageUrlsMap = new HashMap<>();

            for (String folderPath : folderPaths) {
                List<String> imageUrls = firebaseStorageService.getImageUrlsInFolder(folderPath);
                imageUrlsMap.put(folderPath, imageUrls);
            }

            return ResponseEntity.ok(imageUrlsMap);
        } catch (Exception e) {
            logger.severe("Error fetching image URLs: " + e.getMessage());

            // Create an error response map with an empty list as the value
            Map<String, List<String>> errorResponse = new HashMap<>();
            errorResponse.put("error", Arrays.asList(e.getMessage()));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }



    // Class to represent the image data sent from the frontend
    public static class ImageData {
        private String originalImage;
        private String heatmapData;
        private String customFileName;
        private String heatmapFileName;

        // Getters and setters
        public String getOriginalImage() {
            return originalImage;
        }

        public void setOriginalImage(String originalImage) {
            this.originalImage = originalImage;
        }

        public String getHeatmapData() {
            return heatmapData;
        }

        public void setHeatmapData(String heatmapData) {
            this.heatmapData = heatmapData;
        }

        public String getCustomFileName() {
            return customFileName;
        }

        public void setCustomFileName(String customFileName) {
            this.customFileName = customFileName;
        }

        public String getHeatmapFileName() {
            return heatmapFileName;
        }

        public void setHeatmapFileName(String heatmapFileName) {
            this.heatmapFileName = heatmapFileName;
        }
    }

}
