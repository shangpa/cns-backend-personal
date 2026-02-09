package com.example.springjwt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;

@SpringBootApplication
public class SpringJwtApplication {

    public static void main(String[] args) {
        System.setProperty("GOOGLE_APPLICATION_CREDENTIALS",
                new File("src/main/resources/gcp-key.json").getAbsolutePath());
        SpringApplication.run(SpringJwtApplication.class, args);
    }

}
