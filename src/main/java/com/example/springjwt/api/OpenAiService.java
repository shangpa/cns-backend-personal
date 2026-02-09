package com.example.springjwt.api;

import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class OpenAiService {

    private final GoogleTranslateService googleTranslateService;

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    public String generateThumbnail(String prompt) {
        System.out.println("요청 prompt (원문): " + prompt);
        if (prompt == null || prompt.isBlank() || prompt.length() < 10) {
            throw new IllegalArgumentException("⚠️ 프롬프트가 너무 짧거나 비어 있습니다.");
        }

        String translatedPrompt = googleTranslateService.translateToEnglish(prompt);
        System.out.println("번역된 prompt: " + translatedPrompt);

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30_000); // 30초
        factory.setReadTimeout(30_000);    // 30초
        RestTemplate restTemplate = new RestTemplate(factory);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        JSONObject body = new JSONObject();
        body.put("model", "dall-e-3");
        body.put("prompt", translatedPrompt);
        body.put("n", 1);
        body.put("size", "1024x1024");

        HttpEntity<String> request = new HttpEntity<>(body.toString(), headers);
        ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, request, String.class);


        JSONObject responseJson = new JSONObject(response.getBody());
        String openAiImageUrl = responseJson.getJSONArray("data").getJSONObject(0).getString("url");

        System.out.println("OpenAI 이미지 생성 URL: " + openAiImageUrl);

        // 🔽 여기가 핵심: 외부 이미지 → 서버 저장 → 내부 URL 반환
        String localImageUrl = downloadAndStoreImageLocally(openAiImageUrl);
        System.out.println("서버에 저장된 썸네일 URL: " + localImageUrl);

        return localImageUrl;
    }

    public String downloadAndStoreImageLocally(String imageUrl) {
        try (InputStream in = new URL(imageUrl).openStream()) {
            // 업로드 폴더 생성
            Path uploadPath = Paths.get("uploads/");
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // ✅ 쿼리스트링 제거 후 확장자 추출
            String cleanUrl = imageUrl.split("\\?")[0];
            String extension = cleanUrl.contains(".") ? cleanUrl.substring(cleanUrl.lastIndexOf(".")) : ".png";

            String fileName = UUID.randomUUID().toString() + extension;
            Path filePath = uploadPath.resolve(fileName);

            // 파일 저장
            Files.copy(in, filePath);

            return "/uploads/" + fileName;
        } catch (Exception e) {
            throw new RuntimeException("썸네일 저장 실패: " + e.getMessage(), e);
        }
    }


}