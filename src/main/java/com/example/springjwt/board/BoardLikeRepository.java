package com.example.springjwt.board;

import com.example.springjwt.User.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BoardLikeRepository extends JpaRepository<BoardLike, Long> {
    boolean existsByUserAndBoard(UserEntity user, Board board);
    Optional<BoardLike> findByUserAndBoard(UserEntity user, Board board);
    long countByBoard(Board board); // 좋아요 수 조회용
    boolean existsByUserUsernameAndBoard(String username, Board board);//좋아요 눌렀는지 확인용
    void deleteAllByBoard(Board board);
}
