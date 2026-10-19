package com.example.EduSprint.storage;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.springframework.core.io.UrlResource;

import java.io.FileNotFoundException;
import java.net.URL;

// A presigned S3 URL is signed for GET only; UrlResource asks for the size with HEAD, gets 403 and
// cannot answer a browser's Range request (416). Size and existence come from the object's metadata instead.
public class S3ObjectResource extends UrlResource {

    private final AmazonS3 amazonS3;
    private final String bucketName;
    private final String key;
    private ObjectMetadata metadata;
    private boolean missing;

    public S3ObjectResource(URL presignedUrl, AmazonS3 amazonS3, String bucketName, String key) {
        super(presignedUrl);
        this.amazonS3 = amazonS3;
        this.bucketName = bucketName;
        this.key = key;
    }

    @Override
    public boolean exists() {
        return metadata() != null;
    }

    @Override
    public long contentLength() throws FileNotFoundException {
        ObjectMetadata found = metadata();
        if (found == null) {
            throw new FileNotFoundException(key);
        }
        return found.getContentLength();
    }

    private ObjectMetadata metadata() {
        if (metadata == null && !missing) {
            try {
                metadata = amazonS3.getObjectMetadata(bucketName, key);
            } catch (AmazonS3Exception e) {
                if (e.getStatusCode() != 404) {
                    throw e;
                }
                missing = true;
            }
        }
        return metadata;
    }
}
