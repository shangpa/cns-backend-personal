package com.example.springjwt.jwt;

import com.example.springjwt.dto.CustomUserDetails;
import com.example.springjwt.dto.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Iterator;


public class LoginFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    //JWTUtil 주입
    private final JWTUtil jwtUtil;

    public LoginFilter(AuthenticationManager authenticationManager, JWTUtil jwtUtil) {

        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        System.out.println("Login 필터 입니다 ---------------");

        try {
            // JSON 데이터를 읽기 위한 BufferedReader 사용
            BufferedReader reader = request.getReader();
            StringBuilder json = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                json.append(line);
            }

            // JSON 문자열을 파싱하여 username과 password 추출
            ObjectMapper objectMapper = new ObjectMapper();
            LoginRequest loginRequest = objectMapper.readValue(json.toString(), LoginRequest.class);

            String username = loginRequest.getUsername();
            String password = loginRequest.getPassword();
            System.out.println("username : " + username);
            System.out.println("password : " + password);

            // 검증을 위한 Authentication 객체 생성
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, password, null);

            // AuthenticationManager로 전달하여 검증 진행
            return authenticationManager.authenticate(authToken);

        } catch (IOException e) {
            throw new AuthenticationServiceException("Invalid JSON format", e);
        }
    }


    //로그인 성공시 실행하는 메소드 (여기서 JWT를 발급하면 됨)
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) {
        //CustomUserDetails에 정보 받아오기
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        //userName 받기
        String userName = customUserDetails.getUsername();
        //userRole 받기
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
        GrantedAuthority auth = iterator.next();
        String role = auth.getAuthority();
        //토큰 발급
        String token = jwtUtil.createJwt(userName, role, 1000L * 60 * 60 * 10); // 10시간
        /*
        Authorization 키에 token 값을 "Bearer " 넣음
        Authorization: 타입 인증토큰

        에시
        Authorization: Bearer String(인증토큰)
         */
        response.addHeader("Authorization", "Bearer " + token);

        // 응답이 이미 커밋되었는지 확인
        if (!response.isCommitted()) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            try (PrintWriter out = response.getWriter()) {
                int userId = customUserDetails.getUserEntity().getId();
                out.print("{ \"message\": \"Login successful\", \"token\": \"" + token + "\", \"userId\": " + userId + " }");
                out.flush();
            } catch (IOException ex) {
            throw new RuntimeException(ex);
            }
        }
    }

    //로그인 실패시 실행하는 메소드
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) {
        response.setStatus(401);
    }
}