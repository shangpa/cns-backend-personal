package com.example.cns.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;

@Slf4j
public class GoogleTokenVerifierUtil {
    private static final String CLIENT_ID = "804313905644-lnp959rv01pfd058t441pfp3oinmtfaj.apps.googleusercontent.com";

    public static GoogleIdToken.Payload verify(String idTokenString) {
        try {
            log.debug("받은 ID Token: {}", idTokenString);

            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), JacksonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(CLIENT_ID))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken != null) {
                log.debug("ID Token 검증 성공: {}", idToken.getPayload().getEmail());
                return idToken.getPayload();
            } else {
                log.warn("ID Token 검증 실패");
            }
        } catch (Exception e) {
            log.error("검증 중 예외 발생", e);
        }
        return null;
    }

}
