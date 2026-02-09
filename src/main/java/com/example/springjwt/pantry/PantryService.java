package com.example.springjwt.pantry;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.pantry.dto.PantryCreateRequest;
import com.example.springjwt.pantry.dto.PantryResponse;
import com.example.springjwt.pantry.dto.PantryUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PantryService {

    private final PantryRepository pantryRepository;

    /* -------------------------
       생성
       ------------------------- */
    @Transactional
    public PantryResponse createPantry(UserEntity user, PantryCreateRequest req) {
        if (!StringUtils.hasText(req.getName())) {
            throw new IllegalArgumentException("냉장고 이름이 비어 있습니다.");
        }
        if (pantryRepository.existsByUser_IdAndNameIgnoreCase(user.getId(), req.getName().trim())) {
            throw new IllegalArgumentException("같은 이름의 냉장고가 이미 존재합니다: " + req.getName());
        }

        boolean isDefault = req.getIsDefault() != null && req.getIsDefault();
        int sortOrder = req.getSortOrder() != null ? req.getSortOrder() : 0;

        if (isDefault) {
            unsetDefaultForAll(user.getId());
        }

        Pantry saved = pantryRepository.save(
                Pantry.builder()
                        .user(user)
                        .name(req.getName().trim())
                        .note(req.getNote())
                        .imageUrl(req.getImageUrl())
                        .isDefault(isDefault)
                        .sortOrder(sortOrder)
                        .build()
        );

        return toResponse(saved);
    }

    /* -------------------------
       리스트 조회
       ------------------------- */
    @Transactional(readOnly = true)
    public List<PantryResponse> listPantries(UserEntity user) {
        return pantryRepository.findAllByUser_IdOrderBySortOrderAscCreatedAtAsc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /* -------------------------
       수정
       ------------------------- */
    @Transactional
    public PantryResponse updatePantry(UserEntity user, Long pantryId, PantryUpdateRequest req) {
        Pantry pantry = pantryRepository.findByIdAndUser_Id(pantryId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("해당 냉장고를 찾을 수 없습니다. id=" + pantryId));

        if (req.getName() != null) {
            String newName = req.getName().trim();
            if (!StringUtils.hasText(newName)) {
                throw new IllegalArgumentException("냉장고 이름이 비어 있습니다.");
            }
            if (!newName.equalsIgnoreCase(pantry.getName())
                    && pantryRepository.existsByUser_IdAndNameIgnoreCase(user.getId(), newName)) {
                throw new IllegalArgumentException("같은 이름의 냉장고가 이미 존재합니다: " + newName);
            }
            pantry.setName(newName);
        }

        if (req.getNote() != null) {
            pantry.setNote(req.getNote());
        }
        if (req.getImageUrl() != null) {
            pantry.setImageUrl(req.getImageUrl());
        }
        if (req.getSortOrder() != null) {
            pantry.setSortOrder(req.getSortOrder());
        }

        if (req.getIsDefault() != null) {
            if (req.getIsDefault()) {
                unsetDefaultForAll(user.getId());
                pantry.setDefault(true);
            } else {
                pantry.setDefault(false);
            }
        }

        return toResponse(pantry);
    }
    // 냉장고 삭제
    public void deletePantry(UserEntity user, Long pantryId) {
        Pantry pantry = pantryRepository.findByIdAndUser_Id(pantryId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("해당 냉장고가 없습니다."));
        //todo 냉장고 내부 재료 삭제해야함
        pantryRepository.delete(pantry);
    }


    /* -------------------------
       내부 유틸
       ------------------------- */
    @Transactional
    protected void unsetDefaultForAll(int userId) {
        var pantries = pantryRepository.findAllByUser_IdOrderBySortOrderAscCreatedAtAsc(userId);
        for (Pantry p : pantries) {
            if (p.isDefault()) p.setDefault(false);
        }
    }

    private PantryResponse toResponse(Pantry p) {
        return PantryResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .note(p.getNote())
                .imageUrl(p.getImageUrl())
                .isDefault(p.isDefault())
                .sortOrder(p.getSortOrder())
                .createdAt(p.getCreatedAt())
                .build();
    }

    //단건 조회
    @Transactional(readOnly = true)
    public PantryResponse getPantry(UserEntity user, Long pantryId) {
        var p = pantryRepository.findByIdAndUser_Id(pantryId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("해당 냉장고가 없습니다."));
        return toResponse(p);
    }

}
