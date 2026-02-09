// BoardCommentResponseDTO.java
package com.example.springjwt.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class BoardCommentResponseDTO {
    private Long id;
    private String username;
    private String content;
    private LocalDateTime createdAt;
}
