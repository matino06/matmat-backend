package com.example.EduSprint.controller;

import com.example.EduSprint.storage.PrivateStorageKeys;
import com.example.EduSprint.storage.StorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/image")
public class ImageController {

    public final StorageService storageService;

    public ImageController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/{*imageName}")
    public ResponseEntity<Resource> loadImage(@PathVariable String imageName) {
        if (imageName.startsWith("/")) {
            imageName = imageName.substring(1);
        }
        // Answer photos and creators' materials are private; this endpoint is public.
        if (PrivateStorageKeys.isPrivate(imageName)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = storageService.loadAsResource(imageName);

        if (resource == null) {
            return ResponseEntity.notFound().build();
        }

        // Without an explicit type Spring picks application/json, and then answers a browser's Range request for a video with 406.
        MediaType type = MediaTypeFactory.getMediaType(imageName).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok().contentType(type).body(resource);
    }
}
