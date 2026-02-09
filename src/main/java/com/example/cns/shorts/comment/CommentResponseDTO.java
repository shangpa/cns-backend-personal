package com.example.cns.shorts.comment;

import com.example.cns.User.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CommentResponseDTO {
    private Long id;
    private String content;
    private String createdAt;
    private String username;   // 👈 username만 포함

    public static CommentResponseDTO fromEntity(ShortComment comment) {
        UserEntity user = comment.getUser();
        return new CommentResponseDTO(
                comment.getId(),
                comment.getContent(),
                comment.getCreatedAt().toString(),
                user != null ? user.getUsername() : "(알 수 없음)" // 👈 null 방어
        );
    }
}
