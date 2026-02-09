package com.example.springjwt.fridge.history;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.User.UserRepository;
import com.example.springjwt.fridge.Fridge;
import com.example.springjwt.fridge.FridgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FridgeHistoryService {

    private final FridgeHistoryRepository historyRepository;
    private final FridgeRepository fridgeRepository;
    private final UserRepository userRepository;

    // 기록 저장
    public void saveHistory(Long userId, String ingredientName, double quantity, LocalDate fridgeDate, String dateOption, FridgeHistory.ActionType type) {
        UserEntity user = userRepository.findById(userId.intValue())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        FridgeHistory history = new FridgeHistory();
        history.setUser(user);
        history.setIngredientName(ingredientName);
        history.setQuantity(quantity);
        history.setFridgeDate(fridgeDate);
        history.setDateOption(dateOption);
        history.setActionType(type);
        history.setActionDate(LocalDateTime.now());

        historyRepository.save(history);
    }

    // 특정 재료의 이력 조회
    public List<FridgeHistoryResponse> getHistory(Long userId, String ingredientName) {
        UserEntity user = userRepository.findById(userId.intValue())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<FridgeHistory> histories = historyRepository.findByUserAndIngredientNameOrderByActionDateDesc(user, ingredientName);
        String unit = getUnitFromFridge(user, ingredientName);

        return histories.stream()
                .map(h -> mapToResponse(h, unit))
                .toList();
    }

    // 유저 전체 재료 이력 조회
    public List<FridgeHistoryResponse> getAllHistoryByUser(Long userId) {
        UserEntity user = userRepository.findById(userId.intValue())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<FridgeHistory> histories = historyRepository.findAllByUser(user);
        return histories.stream()
                .map(h -> mapToResponse(h, getUnitFromFridge(user, h.getIngredientName())))
                .toList();
    }

    // 냉장고에 있는 단위 가져오기
    private String getUnitFromFridge(UserEntity user, String ingredientName) {
        return fridgeRepository.findTopByUserAndIngredientNameOrderByUpdatedAtDesc(user, ingredientName)
                .map(Fridge::getUnitDetail)
                .orElse("개");
    }

    // 매핑 로직
    public FridgeHistoryResponse mapToResponse(FridgeHistory history, String unit) {
        return new FridgeHistoryResponse(
                history.getIngredientName(),
                history.getQuantity(),
                unit,
                history.getActionType().name(),
                history.getActionDate(),
                history.getFridgeDate(),
                history.getDateOption()
        );
    }
}
