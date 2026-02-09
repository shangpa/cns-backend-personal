package com.example.springjwt.api.vision;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class GcpVisionClient {

    public List<String> detectLabels(MultipartFile imageFile) {
        try {
            // ✅ resources/gcp-key.json에서 credentials 읽기
            ClassPathResource resource = new ClassPathResource("gcp-key.json");
            InputStream credentialsStream = resource.getInputStream();

            GoogleCredentials credentials = GoogleCredentials
                    .fromStream(credentialsStream)
                    .createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));

            ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                    .build();

            try (ImageAnnotatorClient vision = ImageAnnotatorClient.create(settings)) {
                ByteString imgBytes = ByteString.copyFrom(imageFile.getBytes());

                Image image = Image.newBuilder().setContent(imgBytes).build();
                Feature feature = Feature.newBuilder().setType(Feature.Type.LABEL_DETECTION).build();
                AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                        .addFeatures(feature)
                        .setImage(image)
                        .build();

                List<AnnotateImageResponse> responses = vision.batchAnnotateImages(List.of(request)).getResponsesList();

                List<String> results = new ArrayList<>();
                for (AnnotateImageResponse res : responses) {
                    for (EntityAnnotation annotation : res.getLabelAnnotationsList()) {
                        results.add(annotation.getDescription().toLowerCase());
                    }
                }
                return results;
            }

        } catch (Exception e) {
            throw new RuntimeException("GCP Vision API 호출 실패: " + e.getMessage(), e);
        }
    }
}
