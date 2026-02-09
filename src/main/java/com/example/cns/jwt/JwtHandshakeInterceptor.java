package com.example.cns.jwt;
import com.example.cns.User.UserRepository;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtHandshakeInterceptor(JWTUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        System.out.println("🛰️ beforeHandshake 실행됨");

        // 쿼리 파라미터에서 토큰 추출
        String query = request.getURI().getQuery(); // 예: token=eyJ...
        System.out.println("🛰️ [Interceptor] query: " + query);

        if (query != null && query.startsWith("token=")) {
            String jwtToken = query.substring(6); // token= 이후부터 잘라냄
            System.out.println("🔐 [Interceptor] 쿼리로부터 토큰 추출: " + jwtToken);

            if (!jwtUtil.isExpired(jwtToken)) {
                String username = jwtUtil.getUsername(jwtToken);
                System.out.println("✅ [Interceptor] 토큰 유효, 사용자: " + username);
                attributes.put("username", username);
                return true;
            } else {
                System.out.println("❌ [Interceptor] 토큰 만료");
            }
        } else {
            System.out.println("❌ [Interceptor] 토큰 없음 (query)");
        }

        return false;
    }
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}