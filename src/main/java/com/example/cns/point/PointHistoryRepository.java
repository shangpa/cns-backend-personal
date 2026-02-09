package com.example.cns.point;

import com.example.cns.User.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {
    List<PointHistory> findByUser(UserEntity user);
    List<PointHistory> findByUserOrderByCreatedAtDesc(UserEntity user);
    List<PointHistory> findByUserAndActionNotOrderByCreatedAtDesc(UserEntity user, PointActionType excluded);
    List<PointHistory> findByUserAndActionOrderByCreatedAtDesc(UserEntity user, PointActionType action);

}
