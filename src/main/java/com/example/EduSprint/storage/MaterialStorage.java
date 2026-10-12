package com.example.EduSprint.storage;

import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Date;

// Materijali vlastitih predmeta na S3 (isti bucket kao slike, prefiks own-courses/{kreator}/{predmet}/).
// Frontend uploada izravno na S3 preko presigned URL-a; backend samo čita, provjerava i briše.
@Component
public class MaterialStorage {

    private final AmazonS3 s3;
    private final String bucket;

    public MaterialStorage(StorageProperties properties) {
        this.bucket = properties.getS3BucketName();
        this.s3 = AmazonS3ClientBuilder.standard()
                .withRegion(properties.getRegion())
                .withCredentials(new AWSStaticCredentialsProvider(
                        new BasicAWSCredentials(properties.getAccessKey(), properties.getSecretKey())))
                .build();
    }

    public static String coursePrefix(long accountId, long courseId) {
        return "own-courses/" + accountId + "/" + courseId + "/";
    }

    public String presignPut(String key, String contentType, Duration validFor) {
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, key)
                .withMethod(HttpMethod.PUT)
                .withContentType(contentType)
                .withExpiration(new Date(System.currentTimeMillis() + validFor.toMillis()));
        return s3.generatePresignedUrl(request).toString();
    }

    public boolean exists(String key) {
        return s3.doesObjectExist(bucket, key);
    }

    public long size(String key) {
        return s3.getObjectMetadata(bucket, key).getContentLength();
    }

    // Skida objekt u privremenu datoteku (PDF do 100 MB ne drži se u memoriji). Pozivatelj je briše.
    public File download(String key) throws IOException {
        File file = Files.createTempFile("own-course-", ".bin").toFile();
        try (InputStream in = s3.getObject(bucket, key).getObjectContent()) {
            Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        return file;
    }

    public byte[] bytes(String key) throws IOException {
        try (InputStream in = s3.getObject(bucket, key).getObjectContent()) {
            return in.readAllBytes();
        }
    }

    public void put(String key, byte[] content, String contentType) {
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentLength(content.length);
        meta.setContentType(contentType);
        s3.putObject(bucket, key, new ByteArrayInputStream(content), meta);
    }

    public void delete(String key) {
        s3.deleteObject(bucket, key);
    }

    public void deletePrefix(String prefix) {
        if (prefix == null || !prefix.startsWith("own-courses/") || prefix.length() <= "own-courses/".length()) {
            throw new IllegalArgumentException("Smije se brisati samo mapa vlastitog predmeta: " + prefix);
        }
        ListObjectsV2Request request = new ListObjectsV2Request().withBucketName(bucket).withPrefix(prefix);
        ListObjectsV2Result result;
        do {
            result = s3.listObjectsV2(request);
            for (S3ObjectSummary o : result.getObjectSummaries()) s3.deleteObject(bucket, o.getKey());
            request.setContinuationToken(result.getNextContinuationToken());
        } while (result.isTruncated());
    }
}
