package com.example.springjwt.fridge.stats;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.fridge.Fridge;
import com.example.springjwt.fridge.FridgeRepository;
import com.example.springjwt.fridge.history.FridgeHistory;
import com.example.springjwt.fridge.history.FridgeHistoryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class FridgeStatsService {

    private final FridgeRepository fridgeRepository;          // 현재 냉장고에 담긴 재료를 조회하기 위한 Repository
    private final FridgeHistoryRepository historyRepository;  // 재료 사용/추가 이력을 조회하기 위한 Repository

    // 생성자 주입 (Spring이 Repository를 자동으로 넣어줌)
    public FridgeStatsService(FridgeRepository fridgeRepository, FridgeHistoryRepository historyRepository) {
        this.fridgeRepository = fridgeRepository;
        this.historyRepository = historyRepository;
    }

    /**
     * 로그인한 사용자의 냉장고 통계 데이터를 계산하여 반환
     *
     * @param user 로그인한 사용자 엔티티
     * @return FridgeStatsResponse (지금 재료 수, 먹은 수, 못 지킨 수, 냉장/냉동/실외 분류 수 포함)
     */
    public FridgeStatsResponse getStats(UserEntity user) {
        // 1. 현재 냉장고에 담긴 재료 리스트 (최근 수정 순)
        List<Fridge> fridges = fridgeRepository.findByUserIdOrderByUpdatedAtDesc((long) user.getId());

        // 2. 해당 사용자의 모든 재료 사용/추가 이력
        List<FridgeHistory> histories = historyRepository.findAllByUser(user);

        // ====== [지금 냉장고에 보관된 재료 수] ======
        long nowCount = fridges.size();

        // ====== [유통기한 내에 먹은 재료 수] ======
        long eatenCount = histories.stream()
                // ① 액션 타입이 "USE"(사용)인 경우
                .filter(h -> h.getActionType() == FridgeHistory.ActionType.USE)
                // ② 해당 재료의 유통기한(fridgeDate)이 null이 아니고,
                //    실제 사용 날짜(actionDate)가 유통기한을 넘기지 않은 경우
                .filter(h -> h.getFridgeDate() != null &&
                        !h.getActionDate().toLocalDate().isAfter(h.getFridgeDate()))
                .count();

        // 오늘 날짜
        LocalDate today = LocalDate.now();

        // ====== [유통기한을 지키지 못한 재료 수] ======
        long expiredCount = fridges.stream()
                .filter(f -> f.getFridgeDate() != null)          // 날짜가 있는 재료만
                .filter(f -> "유통기한".equals(f.getDateOption())) // dateOption이 유통기한인 경우만
                .filter(f -> f.getFridgeDate().isBefore(today))  // 오늘 이전이면 만료
                .count();

        // ====== [보관 위치별 분류 수] ======
        long refrigeratedCount = fridges.stream()
                .filter(f -> f.getStorageArea().equalsIgnoreCase("냉장"))
                .count();

        long frozenCount = fridges.stream()
                .filter(f -> f.getStorageArea().equalsIgnoreCase("냉동"))
                .count();

        long roomCount = fridges.stream()
                .filter(f -> f.getStorageArea().equalsIgnoreCase("실온")
                        || f.getStorageArea().equalsIgnoreCase("실외")) // 실온/실외 둘 다 처리
                .count();

        // 계산된 통계를 DTO에 담아 반환
        return new FridgeStatsResponse(
                nowCount,          // 지금 냉장고에 있는 재료 개수
                eatenCount,        // 유통기한 내 먹은 재료 개수
                expiredCount,      // 유통기한 지키지 못한 재료 개수
                refrigeratedCount, // 냉장 보관 재료 개수
                frozenCount,       // 냉동 보관 재료 개수
                roomCount          // 실온/실외 보관 재료 개수
        );
    }
}
