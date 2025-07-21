package com.example.Movie.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;

@Service
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket.name}")
    private String bucketName;

    public S3Service() {
        this.s3Client = S3Client.builder()
                .region(Region.US_EAST_2)
                .build();
    }

    public String uploadPoster(String movieId, String posterUrl) throws IOException {
        String key = "originals/" + movieId + ".jpg";
        String s3Url = "https://" + bucketName + ".s3.us-east-2.amazonaws.com/" + key;

        // Check if the object already exists
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());

            // If no exception, file exists — return existing URL
            return s3Url;

        } catch (NoSuchKeyException e) {
            // 404 Not Found — continue to upload
        } catch (S3Exception e) {
            if (e.statusCode() != 404) {
                throw e;
            }
            // If 404, continue to upload
        }

        // Download and upload the poster
        URL url = new URL(posterUrl);
        BufferedImage image = ImageIO.read(url);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", os);
        byte[] bytes = os.toByteArray();

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType("image/jpeg")
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(bytes));

        return s3Url;
    }
}
