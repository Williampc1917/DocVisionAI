package com.example.docvisionai.service;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;



/**
 * Service class responsible for handling pneumonia detection by sending medical images to a Flask-based
 * AI model for prediction. This service verifies that the uploaded image is grayscale and prepares the image
 * for sending to the Flask API.
 */
@Service
public class PneumoniaDetectionService {
    private static final String PYTHON_API_URL = "https://reference-node-426102-r4.uk.r.appspot.com/predict";


    //"http://127.0.0.1:5000/predict";

    private static final Logger logger = Logger.getLogger(PneumoniaDetectionService.class.getName());

    static {
        // Register the TwelveMonkeys ImageIO plugins
        ImageIO.scanForPlugins();
    }



    /**
     * Processes the uploaded image by verifying that it is grayscale and sending it to the Flask API for pneumonia detection.
     *
     * @param file The uploaded image file (in MultipartFile format).
     * @return A map containing the response from the Flask API, which includes prediction results.
     * @throws IOException If there is an issue with reading or processing the image file.
     */
    public Map<String, Object> processImage(MultipartFile file) throws IOException {
        // Check if the file is an image
        if (!isGrayscaleImage(file)) {
            logger.severe("Uploaded file is not a grayscale image");
            throw new IllegalArgumentException("Uploaded file is not a grayscale image");
        }

        BufferedImage image = readImage(file);

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // Create a temporary file to hold the uploaded image
        File tempFile = File.createTempFile("uploaded-", ".jpg");
        try {
            ImageIO.write(image, "jpg", tempFile);
        } catch (IOException e) {
            logger.severe("Failed to write image to temporary file: " + e.getMessage());
            throw e;
        }

        // Prepare the request body with the image file
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", new FileSystemResource(tempFile));

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        Map<String, Object> responseBody;
        try {
            // Send the image to the Flask API and retrieve the response
            ResponseEntity<Map> response = restTemplate.postForEntity(PYTHON_API_URL, requestEntity, Map.class);

            logger.info("Response code: " + response.getStatusCodeValue());
            responseBody = response.getBody();
        } catch (Exception e) {
            logger.severe("Error occurred while calling Flask API: " + e.getMessage());
            throw new RuntimeException("Failed to process image with Flask API", e);
        } finally {
            // Clean up temporary file
            if (tempFile.exists() && !tempFile.delete()) {
                logger.warning("Failed to delete temporary file: " + tempFile.getAbsolutePath());
            }
        }

        return responseBody;
    }



    /**
     * Checks if the uploaded file is a grayscale image.
     *
     * @param file The uploaded image file (in MultipartFile format).
     * @return True if the image is in grayscale format, otherwise false.
     */
    private boolean isGrayscaleImage(MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename();
            logger.info("Checking file: " + fileName);

            try (ImageInputStream iis = ImageIO.createImageInputStream(file.getInputStream())) {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
                if (readers.hasNext()) {
                    ImageReader reader = readers.next();
                    reader.setInput(iis);
                    IIOMetadata metadata = reader.getImageMetadata(0);

                    // Check if the image metadata suggests it's grayscale
                    if (isGrayscaleMetadata(metadata)) {
                        return true;
                    }

                    // Verify by analyzing the pixel color components
                    BufferedImage image = reader.read(0);
                    int numComponents = image.getColorModel().getNumComponents();
                    if (numComponents == 1) {
                        // Single component (grayscale) image
                        return true;
                    } else if (numComponents == 3) {
                        // Check if it's a grayscale image saved with 3 components
                        return isGrayscale(image);
                    }
                }
            }
            return false;
        } catch (IOException e) {
            logger.severe("IOException while reading file: " + e.getMessage());
            return false;
        }
    }



    /**
     * Checks if the image metadata indicates that it is grayscale.
     *
     * @param metadata The metadata of the image.
     * @return True if the metadata suggests the image is grayscale, otherwise false.
     */
    private boolean isGrayscaleMetadata(IIOMetadata metadata) {
        IIOMetadataNode standardTree = (IIOMetadataNode) metadata.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);
        IIOMetadataNode colorSpaceNode = (IIOMetadataNode) standardTree.getElementsByTagName("ColorSpaceType").item(0);
        if (colorSpaceNode != null) {
            String colorSpaceType = colorSpaceNode.getAttribute("name");
            return "GRAY".equalsIgnoreCase(colorSpaceType);
        }
        return false;
    }



    /**
     * Checks if the image is grayscale by analyzing the RGB values of each pixel.
     * A grayscale image has equal red, green, and blue values for all pixels.
     *
     * @param image The image to be analyzed.
     * @return True if the image is grayscale, otherwise false.
     */
    private boolean isGrayscale(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                if (r != g || g != b) {
                    return false; // Not grayscale
                }
            }
        }
        return true; // All pixels have r == g == b
    }



    /**
     * Reads and returns the BufferedImage from the uploaded MultipartFile.
     *
     * @param file The uploaded image file (in MultipartFile format).
     * @return A BufferedImage representation of the uploaded file.
     * @throws IOException If there is an issue reading the image file.
     */
    private BufferedImage readImage(MultipartFile file) throws IOException {
        BufferedImage image = null;
        try (ImageInputStream iis = ImageIO.createImageInputStream(file.getInputStream())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                reader.setInput(iis);
                image = reader.read(0);
            }
            if (image == null) {
                logger.severe("Invalid image file");
                throw new IllegalArgumentException("Invalid image file");
            }
        } catch (IOException e) {
            logger.severe("Failed to read image file: " + e.getMessage());
            throw e;
        }
        return image;
    }

}



