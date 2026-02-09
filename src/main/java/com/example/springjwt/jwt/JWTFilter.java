package com.example.springjwt.jwt;

import com.example.springjwt.User.UserRepository;
import com.example.springjwt.dto.CustomUserDetails;
import com.example.springjwt.User.UserEntity;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JWTFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;

    public JWTFilter(JWTUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    // ✅ 공개(permitAll) 경로는 필터를 아예 건너뛴다.
    private boolean isPublic(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // CORS preflight
        if ("OPTIONS".equalsIgnoreCase(method)) return true;

        // 정적 리소스(동영상/썸네일)
        if (uri.startsWith("/uploads/")) return true;

        // 숏폼 공개 API (로그인 없이 조회 가능해야 하는 것들)
        if (uri.equals("/api/shorts/random")) return true;
        if (uri.equals("/api/shorts/random3")) return true;
        if (uri.matches("^/api/shorts/\\d+$")) return true; // /api/shorts/{userId}

        // 이미 공개로 쓰는 엔드포인트들 (필요시 추가)
        if (uri.equals("/join") || uri.equals("/login") || uri.equals("/admin/join")) return true;
        if (uri.equals("/api/recipes/search")) return true;
        // 예: 전체 공개 레시피 목록/추천이 공개라면 여기도 허용
        if (uri.startsWith("/api/recipes/public")) return true;
        if (uri.startsWith("/api/recipes/seasonal")) return true;

        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // ✅ 공개 경로는 무조건 통과 (여기서 바로 다음 필터로)
        if (isPublic(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Authorization 헤더 확인
        String authHeader = request.getHeader("Authorization");
        System.out.println("[JWTFilter] Authorization: " + authHeader + " | uri=" + requestURI);

        // ✅ 토큰이 없거나 형식이 아니면 '그냥 통과'
        // (인증이 필요한 경로는 뒤 단계에서 Spring Security가 막는다.
        //  공개 경로가 아니더라도 여기서 403/401을 내지 않는다.)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        // ✅ 만료 토큰도 조용히 통과(403 금지). 인증은 세팅하지 않음.
        if (jwtUtil.isExpired(token)) {
            System.out.println("[JWTFilter] token expired");
            filterChain.doFilter(request, response);
            return;
        }

        // ✅ 토큰이 유효하면만 사용자 조회 및 인증 세팅
        String username = jwtUtil.getUsername(token);
        String role = jwtUtil.getRole(token);
        System.out.println("[JWTFilter] username=" + username + ", role=" + role);

        UserEntity userEntity = userRepository.findByUsername(username);

        // (선택) 차단 계정 즉시 차단 — 이 경우에만 403
        if (userEntity != null && userEntity.isBlocked()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json; charset=UTF-8");
            response.getWriter().write("{\"error\": \"차단된 회원입니다.\"}");
            System.out.println("[JWTFilter] blocked user: " + username);
            return;
        }

        if (userEntity != null) {
            CustomUserDetails cud = new CustomUserDetails(userEntity);
            Authentication authToken =
                    new UsernamePasswordAuthenticationToken(cud, null, cud.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authToken);
        } else {
            // 유저가 없으면 인증 세팅 없이 통과 (인증 필요한 곳은 나중에 거부됨)
            System.out.println("[JWTFilter] user not found for token");
        }

        filterChain.doFilter(request, response);
    }
}
