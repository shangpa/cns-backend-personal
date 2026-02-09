package com.example.springjwt.tradepost.request;

import com.example.springjwt.User.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserSimpleDTO {
    private Long id;
    private String nickname;

    public static UserSimpleDTO fromEntity(UserEntity user) {
        return new UserSimpleDTO((long) user.getId(), user.getUsername());
    }
}
