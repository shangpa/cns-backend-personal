package com.example.springjwt.admin;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.User.UserRepository;
import com.example.springjwt.admin.log.AdminLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepository;
    private final AdminLogService adminLogService;

    @Transactional
    public void blockUser(int userId, String adminUsername, String reason) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));
        user.setBlocked(true);
        user.setBlockedAt(LocalDateTime.now());
        userRepository.save(user);

        adminLogService.logAdminAction(
                adminUsername, "BLOCK_USER", "USER", (long) userId, reason
        );
    }

    @Transactional
    public void unblockUser(int userId, String adminUsername, String reason) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));
        user.setBlocked(false);
        user.setBlockedAt(null);
        userRepository.save(user);

        adminLogService.logAdminAction(
                adminUsername, "UNBLOCK_USER", "USER", (long) userId, reason
        );
    }
}
