package com.example.EduSprint.storage;

import java.io.IOException;
import java.net.URL;
import java.util.stream.Stream;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import java.util.Date;
import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileSystemStorageService implements StorageService {

    private final AmazonS3 amazonS3;
    private final String bucketName;
    private final String region;

    @Autowired
    public FileSystemStorageService(StorageProperties properties) {
        this.bucketName = properties.getS3BucketName();
        this.region = properties.getRegion();

        // AWS credentials setup
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(properties.getAccessKey(), properties.getSecretKey());
        this.amazonS3 = AmazonS3ClientBuilder.standard()
                .withRegion(region)
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .build();
    }

    @Override
    public void store(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new StorageException("Failed to store empty file.");
            }

            String filename = file.getOriginalFilename();
            if (filename != null) {
                // Upload file to S3
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength(file.getSize());
                amazonS3.putObject(new PutObjectRequest(bucketName, filename, file.getInputStream(), metadata));
            }
        } catch (IOException e) {
            throw new StorageException("Failed to store file.", e);
        }
    }

    @Override
    public Stream<String> loadAll() {
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest().withBucketName(bucketName);
        ObjectListing objectListing = amazonS3.listObjects(listObjectsRequest);
        return objectListing.getObjectSummaries().stream()
                .map(S3ObjectSummary::getKey);
    }

    @Override
    public String load(String filename) {
        return amazonS3.getUrl(bucketName, filename).toString();
    }

    @Override
    public Resource loadAsResource(String filename) {
        try {
            // Generate a presigned URL with 1-hour validity
            GeneratePresignedUrlRequest generatePresignedUrlRequest =
                    new GeneratePresignedUrlRequest(bucketName, filename)
                            .withMethod(HttpMethod.GET)
                            .withExpiration(new Date(System.currentTimeMillis() + 3600000));

            URL presignedUrl = amazonS3.generatePresignedUrl(generatePresignedUrlRequest);
            return new UrlResource(presignedUrl);
        } catch (Exception e) {
            throw new StorageFileNotFoundException("Could not read file: " + filename, e);
        }
    }

    @Override
    public void deleteAll() {
        ObjectListing objectListing = amazonS3.listObjects(new ListObjectsRequest().withBucketName(bucketName));
        for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
            amazonS3.deleteObject(new DeleteObjectRequest(bucketName, objectSummary.getKey()));
        }
    }

    @Override
    public void init() {
        if (!amazonS3.doesBucketExistV2(bucketName)) {
            amazonS3.createBucket(bucketName);
        }
    }
}