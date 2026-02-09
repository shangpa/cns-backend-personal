package com.example.springjwt.controller;

import com.example.springjwt.dto.JoinDTO;
import com.example.springjwt.jwt.ApiResponse;
import com.example.springjwt.User.JoinService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@ResponseBody
public class JoinController {

    private final JoinService joinService;

    public JoinController(JoinService joinService) {
        this.joinService = joinService;
    }

    @PostMapping("/join")
    public ResponseEntity<ApiResponse> joinProcess(@RequestBody JoinDTO joinDTO) {
        // 회원가입 처리 결과
        boolean isJoined = joinService.joinProcess(joinDTO);

        if (isJoined) {
            // 회원가입 성공 응답
            ApiResponse response = new ApiResponse(true, "회원가입 성공");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            // 이미 존재하는 회원명일 때
            ApiResponse response = new ApiResponse(false, "이미 존재하는 회원명입니다.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}