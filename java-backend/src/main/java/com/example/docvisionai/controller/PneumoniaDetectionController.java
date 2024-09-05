package com.example.docvisionai.controller;

import com.example.docvisionai.service.PneumoniaDetectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/pneumonia-detection")
public class PneumoniaDetectionController {

    private static final Logger logger = Logger.getLogger(PneumoniaDetectionController.class.getName());

    private final PneumoniaDetectionService pneumoniaDetectionService;

    @Autowired
    public PneumoniaDetectionController(PneumoniaDetectionService pneumoniaDetectionService) {
        this.pneumoniaDetectionService = pneumoniaDetectionService;
    }



    /**
     * Controller for handling pneumonia detection requests.
     *
     * This controller provides an API for uploading medical images and detecting pneumonia.
     * The endpoint accepts multiple image files, processes each image using the PneumoniaDetectionService,
     * and returns a diagnosis with a confidence score and heatmap.
     *
     * @param files A list of image files (MultipartFile) to be analyzed for pneumonia.
     * @return A ResponseEntity containing a list of results for each image in the following format:
     *         [
     *           {
     *             "confidence": 0.92,
     *             "diagnosis": "Pneumonia",
     *             "heatmap": "<base64_encoded_heatmap_image>"
     *           },
     *           {
     *             "confidence": 0.75,
     *             "diagnosis": "Normal",
     *             "heatmap": "<base64_encoded_heatmap_image>"
     *           }
     *         ]
     *         If the file list is empty, returns a 400 Bad Request with:
     *         [
     *           { "error": "File list is empty" }
     *         ]
     *         If one or more files cannot be processed, the response will include an error message for each failed file:
     *         [
     *           { "error": "An error occurred: [Error Message]" }
     *         ]
     *         On general server errors, a 500 Internal Server Error is returned.
     * @throws IllegalArgumentException If an invalid file is uploaded.
     * @throws Exception If any other error occurs during image processing.
     */
    @PostMapping("/upload")
    public ResponseEntity<List<Map<String, Object>>> handleFileUpload(@RequestParam("images") List<MultipartFile> files) {
        if (files.isEmpty()) {
            return new ResponseEntity<>(Collections.singletonList(Map.of("error", "File list is empty")), HttpStatus.BAD_REQUEST);
        }

        List<Map<String, Object>> results = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                results.add(Map.of("error", "One of the files is empty"));
                continue;
            }
            try {
                Map<String, Object> result = pneumoniaDetectionService.processImage(file);
                results.add(result);
            } catch (IllegalArgumentException e) {
                logger.warning("Invalid file uploaded: " + e.getMessage());
                results.add(Map.of("error", e.getMessage()));
            } catch (Exception e) {
                logger.severe("An error occurred while processing the image: " + e.getMessage());
                results.add(Map.of("error", "An error occurred: " + e.getMessage()));
            }
        }

        return new ResponseEntity<>(results, HttpStatus.OK);
    }
}








