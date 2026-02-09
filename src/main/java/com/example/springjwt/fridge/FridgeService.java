package com.example.springjwt.fridge;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.User.UserRepository;
import com.example.springjwt.fridge.history.FridgeHistory;
import com.example.springjwt.fridge.history.FridgeHistoryService;
import com.example.springjwt.point.PointActionType;
import com.example.springjwt.point.PointService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FridgeService {

    @Autowired
    private FridgeRepository fridgeRepository;
    @Autowired
    private UserRepository userRepository; // UserEntity 조회를 위한 Repository
    @Autowired
    private PointService pointService;
    @Autowired
    private FridgeHistoryService historyService;


    // 냉장고 항목 추가 (userId를 추가 인자로 받음)
    public Fridge createFridge(Fridge fridge, Long userId) {
        Integer intUserId = userId.intValue();
        UserEntity user = userRepository.findById(intUserId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        fridge.setUser(user);
        fridge.setCreatedAt(LocalDateTime.now());
        fridge.setUpdatedAt(LocalDateTime.now());
        fridgeRepository.save(fridge);

        // 히스토리 기록
        historyService.saveHistory(
                (long) user.getId(),
                fridge.getIngredientName(),
                fridge.getQuantity(),
                fridge.getFridgeDate(),
                fridge.getDateOption(),
                FridgeHistory.ActionType.ADD
        );

        // 포인트 처리
        long totalCount = fridgeRepository.countByUser(user);
        int newStep = (int) (totalCount / 10);
        int prevStep = user.getFridgePointStep();

        if (newStep > prevStep) {
            int diff = newStep - prevStep;
            pointService.addPoint(
                    user,
                    PointActionType.FRIDGE_INPUT,
                    diff * 10,
                    "냉장고 재료 누적 " + totalCount + "개 등록"
            );
            user.setFridgePointStep(newStep);
            userRepository.save(user);
        }
        return fridge;
    }

    // 로그인한 사용자의 냉장고 항목 조회
    @Transactional(readOnly = true)
    public List<Fridge> getFridgesByUserId(Long userId) {
        return fridgeRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    // 개별 항목 조회
    public Fridge getFridgeById(Long id) {
        return fridgeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("냉장고 항목을 찾을 수 없습니다."));
    }

    // 업데이트
    public void updateFridge(Long id, FridgeRequestDTO request, String username) {
        Fridge fridge = fridgeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Fridge not found"));

        if (!fridge.getUser().getUsername().equals(username)) {
            throw new AccessDeniedException("수정 권한 없음");
        }

        // request의 값으로 fridge 업데이트
        fridge.setIngredientName(request.getIngredientName());
        fridge.setStorageArea(request.getStorageArea());
        fridge.setFridgeDate(request.getFridgeDate());
        fridge.setDateOption(request.getDateOption());
        fridge.setQuantity(request.getQuantity());
        fridge.setUnitCategory(request.getUnitCategory());
        fridge.setUnitDetail(request.getUnitDetail());
        fridge.setUpdatedAt(LocalDateTime.now());

        fridgeRepository.save(fridge);
    }


    // 삭제
    @Transactional
    public void deleteFridgesByName(String ingredientName, UserEntity user) {
        List<Fridge> fridges = fridgeRepository.findAllByUserAndIngredientName(user, ingredientName);

        if (fridges.isEmpty()) {
            throw new IllegalArgumentException("사용자의 냉장고에 '" + ingredientName + "' 재료가 없습니다.");
        }

        fridgeRepository.deleteAll(fridges); // 성능상 delete 반복보다 효율적
    }


    //유통기한 찾기
    public List<Fridge> getExpiringFridgeItems(int daysLeft) {
        LocalDate today = LocalDate.now();
        LocalDate targetDate = today.plusDays(daysLeft);
        return fridgeRepository.findAll().stream()
                .filter(f -> f.getFridgeDate() != null)
                .filter(f -> !f.getFridgeDate().isBefore(today)) // 오늘 이후
                .filter(f -> !f.getFridgeDate().isAfter(targetDate)) // targetDate 이전
                .collect(Collectors.toList());
    }

    //냉장고 차감
    @Transactional
    public void useIngredients(List<UsedIngredientDTO> ingredients, UserEntity user) {
        for (UsedIngredientDTO dto : ingredients) {
            List<Fridge> fridgeList = fridgeRepository
                    .findAllByUserAndIngredientNameOrderByCreatedAtAsc(user, dto.getName());

            if (fridgeList.isEmpty()) {
                throw new IllegalArgumentException("재료를 찾을 수 없습니다: " + dto.getName());
            }

            double remainingToUse = dto.getAmount();

            for (Fridge fridge : fridgeList) {
                if (remainingToUse <= 0) break;

                double available = fridge.getQuantity();
                double usedAmount = Math.min(available, remainingToUse);

                if (usedAmount > 0) {
                    fridge.setQuantity(available - usedAmount);
                    fridge.setUpdatedAt(LocalDateTime.now());
                    fridgeRepository.save(fridge);

                    historyService.saveHistory(
                            (long) user.getId(),
                            dto.getName(),
                            usedAmount,
                            fridge.getFridgeDate(),
                            fridge.getDateOption(),
                            FridgeHistory.ActionType.USE
                    );

                    remainingToUse -= usedAmount;
                }
            }

            if (remainingToUse > 0) {
                throw new IllegalStateException("재료 수량이 부족합니다: " + dto.getName());
            }
        }
    }

    //영수증
    public void save(FridgeCreateRequest dto, UserEntity user) {
        Fridge fridge = new Fridge();
        fridge.setIngredientName(dto.getIngredientName());
        fridge.setQuantity(dto.getQuantity());
        fridge.setFridgeDate(LocalDate.parse(dto.getFridgeDate()));
        fridge.setDateOption(dto.getDateOption());
        fridge.setStorageArea(dto.getStorageArea());
        fridge.setUnitDetail(dto.getUnitDetail());
        fridge.setUnitCategory(UnitCategory.valueOf(dto.getUnitCategory()));
        fridge.setUser(user);
        fridge.setCreatedAt(LocalDateTime.now());
        fridge.setUpdatedAt(LocalDateTime.now());

        fridgeRepository.save(fridge);

        historyService.saveHistory(
                (long) user.getId(),
                dto.getIngredientName(),
                dto.getQuantity(),
                LocalDate.parse(dto.getFridgeDate()),
                dto.getDateOption(),
                FridgeHistory.ActionType.ADD
        );
    }

    @Transactional
    public void saveBatch(List<FridgeCreateRequest> dtos, UserEntity user) {
        for (FridgeCreateRequest dto : dtos) {
            Fridge fridge = new Fridge();
            fridge.setIngredientName(dto.getIngredientName());
            fridge.setQuantity(dto.getQuantity());
            fridge.setFridgeDate(LocalDate.parse(dto.getFridgeDate()));
            fridge.setDateOption(dto.getDateOption());
            fridge.setStorageArea(dto.getStorageArea());
            fridge.setUnitDetail(dto.getUnitDetail());
            fridge.setUnitCategory(UnitCategory.valueOf(dto.getUnitCategory()));
            fridge.setUser(user);
            fridge.setCreatedAt(LocalDateTime.now());
            fridge.setUpdatedAt(LocalDateTime.now());

            fridgeRepository.save(fridge);

            historyService.saveHistory(
                    (long) user.getId(),
                    dto.getIngredientName(),
                    dto.getQuantity(),
                    LocalDate.parse(dto.getFridgeDate()),
                    dto.getDateOption(),
                    FridgeHistory.ActionType.ADD
            );
        }
    }
}