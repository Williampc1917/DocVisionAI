package com.example.docvisionai.service;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Storage;
import com.google.firebase.cloud.StorageClient;
import com.google.cloud.storage.Blob;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;



/**
 * Service class for handling Firebase Storage operations, including uploading images
 * and fetching signed URLs for images stored in Firebase.
 */
@Service
public class FirebaseStorageService {

    private static final Logger logger = Logger.getLogger(FirebaseStorageService.class.getName());



    /**
     * Uploads an image to Firebase Storage under a specific patient ID, report name, and pair ID.
     *
     * @param patientId   The unique identifier of the patient.
     * @param reportName  The name of the report associated with the image.
     * @param pairId      The unique identifier for the pair of images (original and heatmap).
     * @param imageBytes  The byte array of the image to be uploaded.
     * @param fileName    The name of the file to be saved in Firebase Storage.
     * @return The public URL of the uploaded image.
     * @throws IOException If there is an error during the image upload.
     */
    public String uploadImage(String patientId, String reportName, String pairId, byte[] imageBytes, String fileName) throws IOException {
        // Create the full path by including the reportName and pairId in the directory structure
        String fullPath = patientId + "/" + reportName + "/" + pairId + "/" + fileName;
        Blob blob = StorageClient.getInstance().bucket().create(fullPath, imageBytes, "image/jpeg");
        String imageUrl = blob.getMediaLink();
        logger.info("Uploaded image to: " + imageUrl);
        return imageUrl;
    }



    /**
     * Retrieves signed URLs for all images stored in a specified folder within Firebase Storage.
     * The signed URLs are valid for 15 minutes and allow temporary access to the images.
     *
     * @param folderPath The path of the folder containing the images in Firebase Storage (gs://bucket_name/...).
     * @return A list of signed image URLs.
     */
    public List<String> getImageUrlsInFolder(String folderPath) {
        List<String> imageUrls = new ArrayList<>();

        try {
            String bucketName = folderPath.split("/")[2];
            String pathPrefix = folderPath.replace("gs://" + bucketName + "/", "");

            // List all blobs (files) in the specified folder
            Page<Blob> blobs = StorageClient.getInstance().bucket().list(Storage.BlobListOption.prefix(pathPrefix));

            for (Blob blob : blobs.iterateAll()) {
                // Generate a signed URL valid for 15 minutes
                URL signedUrl = blob.signUrl(15, TimeUnit.MINUTES, Storage.SignUrlOption.withV4Signature());
                imageUrls.add(signedUrl.toString());
                logger.info("Fetched signed image URL: " + signedUrl);
            }

        } catch (Exception e) {
            logger.severe("Error fetching signed image URLs: " + e.getMessage());
        }

        return imageUrls;
    }



}
