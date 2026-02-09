package com.example.springjwt.report;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<?> report(
            @RequestBody ReportRequestDTO dto,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        reportService.report(dto, userDetails.getUsername());
        return ResponseEntity.ok("신고가 접수되었습니다.");
    }
}