package com.example.EduSprint.controller;

import com.example.EduSprint.storage.StorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
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

    @GetMapping("/{imageName}")
    public ResponseEntity<Resource> loadImage(@PathVariable String imageName) {
        Resource resource = storageService.loadAsResource(imageName);

        if (resource == null) {
            return ResponseEntity.notFound().build();
        }

        return new ResponseEntity<>(resource, HttpStatus.OK);
    }
}
