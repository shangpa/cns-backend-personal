package com.example.springjwt.config;

import com.example.springjwt.User.UserRepository;
import com.example.springjwt.jwt.JWTUtil;
import com.example.springjwt.jwt.JwtHandshakeInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private JWTUtil jwtUtil;
    @Autowired
    private UserRepository userRepository;

    // 클라이언트 연결 경로 설정
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/websocket")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new JwtHandshakeInterceptor(jwtUtil, userRepository)); // 추가!
    }

    // 메시지 브로커 설정
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic"); // 구독 주소 prefix
        registry.setApplicationDestinationPrefixes("/app"); // 전송 주소 prefix
    }
}
