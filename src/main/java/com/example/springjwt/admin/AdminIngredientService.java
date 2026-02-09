package com.example.springjwt.admin;

import com.example.springjwt.ingredient.*;
import com.example.springjwt.admin.dto.IngredientAdminRequest;
import com.example.springjwt.ingredient.dto.IngredientMasterResponse;
import com.example.springjwt.admin.log.AdminLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminIngredientService {

    private final IngredientMasterRepository ingredientRepo;
    private final UnitRepository unitRepo;
    private final AdminLogService adminLogService;

    @Transactional
    public IngredientMasterResponse create(String adminUsername, IngredientAdminRequest dto) {
        var unit = unitRepo.findById(dto.getDefaultUnitId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 단위입니다."));

        var entity = IngredientMaster.builder()
                .nameKo(dto.getNameKo())
                .category(dto.getCategory())
                .defaultUnit(unit)
                .iconUrl(dto.getIconUrl())
                .build();

        ingredientRepo.save(entity);

        adminLogService.logAdminAction(adminUsername, "CREATE_INGREDIENT", "INGREDIENT", entity.getId(), "재료 생성");
        return IngredientMasterResponse.fromEntity(entity);
    }

    @Transactional
    public IngredientMasterResponse update(Long id, String adminUsername, IngredientAdminRequest dto) {
        var entity = ingredientRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("재료가 존재하지 않습니다."));

        var unit = unitRepo.findById(dto.getDefaultUnitId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 단위입니다."));

        entity.setNameKo(dto.getNameKo());
        entity.setCategory(dto.getCategory());
        entity.setDefaultUnit(unit);
        entity.setIconUrl(dto.getIconUrl());

        ingredientRepo.save(entity);

        adminLogService.logAdminAction(adminUsername, "UPDATE_INGREDIENT", "INGREDIENT", entity.getId(), "재료 수정");
        return IngredientMasterResponse.fromEntity(entity);
    }

    @Transactional
    public void delete(Long id, String adminUsername, String reason) {
        var entity = ingredientRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("재료가 존재하지 않습니다."));

        ingredientRepo.delete(entity);

        adminLogService.logAdminAction(adminUsername, "DELETE_INGREDIENT", "INGREDIENT", id, reason);
    }

    public IngredientMasterResponse getById(Long id) {
        return ingredientRepo.findById(id)
                .map(IngredientMasterResponse::fromEntity)
                .orElseThrow(() -> new IllegalArgumentException("재료가 존재하지 않습니다."));
    }
}
