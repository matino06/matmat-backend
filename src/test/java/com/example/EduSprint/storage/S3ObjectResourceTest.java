package com.example.EduSprint.storage;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.example.EduSprint.controller.ImageController;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.net.InetSocketAddress;
import java.net.URL;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class S3ObjectResourceTest {

    private static final byte[] VIDEO = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

    private HttpServer s3;
    private URL presignedUrl;

    // Like S3: a URL presigned for GET answers HEAD with 403, so it cannot tell the file's size.
    @BeforeEach
    void startFakeS3() throws Exception {
        s3 = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        s3.createContext("/", exchange -> {
            if (exchange.getRequestMethod().equals("HEAD")) {
                exchange.sendResponseHeaders(403, -1);
            } else {
                exchange.sendResponseHeaders(200, VIDEO.length);
                exchange.getResponseBody().write(VIDEO);
            }
            exchange.close();
        });
        s3.start();
        presignedUrl = new URL("http://127.0.0.1:" + s3.getAddress().getPort() + "/v15.mp4?X-Amz-Signature=x");
    }

    @AfterEach
    void stopFakeS3() {
        s3.stop(0);
    }

    // Browsers ask for a video in pieces (Range: bytes=0-); without the size the answer was 416 and the video never played.
    @Test
    void videoFromS3IsServedInPieces() throws Exception {
        AmazonS3 amazonS3 = mock(AmazonS3.class);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(VIDEO.length);
        when(amazonS3.getObjectMetadata("bucket", "fizika_matura/video/v15.mp4")).thenReturn(metadata);
        StorageService storage = mock(StorageService.class);
        when(storage.loadAsResource("fizika_matura/video/v15.mp4"))
                .thenReturn(new S3ObjectResource(presignedUrl, amazonS3, "bucket", "fizika_matura/video/v15.mp4"));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ImageController(storage)).build();

        mvc.perform(get("/image/fizika_matura/video/v15.mp4").header("Range", "bytes=0-"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string("Content-Range", "bytes 0-9/10"))
                .andExpect(content().bytes(VIDEO));
        mvc.perform(get("/image/fizika_matura/video/v15.mp4").header("Range", "bytes=8-"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string("Content-Range", "bytes 8-9/10"))
                .andExpect(content().bytes(new byte[]{9, 10}));
    }

    @Test
    void sizeIsAskedOncePerResource() throws Exception {
        AmazonS3 amazonS3 = mock(AmazonS3.class);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(VIDEO.length);
        when(amazonS3.getObjectMetadata("bucket", "v15.mp4")).thenReturn(metadata);
        S3ObjectResource resource = new S3ObjectResource(presignedUrl, amazonS3, "bucket", "v15.mp4");

        resource.exists();
        resource.contentLength();
        resource.contentLength();

        verify(amazonS3, times(1)).getObjectMetadata("bucket", "v15.mp4");
    }

    @Test
    void missingFileIsNotFound() throws Exception {
        AmazonS3 amazonS3 = mock(AmazonS3.class);
        AmazonS3Exception notFound = new AmazonS3Exception("Not Found");
        notFound.setStatusCode(404);
        when(amazonS3.getObjectMetadata("bucket", "nema.png")).thenThrow(notFound);
        StorageService storage = mock(StorageService.class);
        when(storage.loadAsResource("nema.png")).thenReturn(new S3ObjectResource(presignedUrl, amazonS3, "bucket", "nema.png"));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ImageController(storage)).build();

        mvc.perform(get("/image/nema.png")).andExpect(status().isNotFound());
    }
}
