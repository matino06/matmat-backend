package com.example.EduSprint.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.stream.Stream;

public interface StorageService {

    void init();

    void store(MultipartFile file);

    Stream<String> loadAll();

    String load(String filename);

    Resource loadAsResource(String filename);

    void deleteAll();

}