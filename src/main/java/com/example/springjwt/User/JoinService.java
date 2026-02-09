package com.example.springjwt.User;

import com.example.springjwt.dto.JoinDTO;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class JoinService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public JoinService(UserRepository userRepository, BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.userRepository = userRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    //일반 유저
    public boolean joinProcess(JoinDTO joinDTO) {

        String name = joinDTO.getName();
        String username = joinDTO.getUsername();
        String password = joinDTO.getPassword();

        Boolean isExist = userRepository.existsByUsername(username);

        if (isExist) {
            return false;  // 이미 존재하는 회원명일 경우 false 반환
        }

        UserEntity data = new UserEntity();
        data.setName(name);
        data.setUsername(username);
        data.setPassword(bCryptPasswordEncoder.encode(password));
        data.setRole("ROLE_USER");  // 일반 사용자로 설정

        userRepository.save(data);
        return true;  // 회원가입 성공
    }

    //관리자
    public boolean joinAdminProcess(JoinDTO joinDTO) {

        String username = joinDTO.getUsername();
        if (userRepository.existsByUsername(username)) {
            return false;
        }

        UserEntity admin = new UserEntity();
        admin.setName(joinDTO.getName());
        admin.setUsername(username);
        admin.setPassword(bCryptPasswordEncoder.encode(joinDTO.getPassword()));
        admin.setRole("ROLE_ADMIN");  // ✅ 핵심
        userRepository.save(admin);
        return true;
    }

}