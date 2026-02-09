package com.example.springjwt.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GooglePublicKeysManager;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;

import java.util.Collections;

public class GoogleTokenVerifierUtil {
    private static final String CLIENT_ID = "207116637821-9s9rbj2mn86707fg9khds5o2b78m7h2q.apps.googleusercontent.com";

    public static GoogleIdToken.Payload verify(String idTokenString) {
        try {
            System.out.println("✅ 받은 ID Token: " + idTokenString);

            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), JacksonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(CLIENT_ID))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken != null) {
                System.out.println("✅ ID Token 검증 성공: " + idToken.getPayload().getEmail());
                return idToken.getPayload();
            } else {
                System.out.println("❌ ID Token 검증 실패");
            }
        } catch (Exception e) {
            System.out.println("❌ 검증 중 예외 발생");
            e.printStackTrace();
        }
        return null;
    }

}
