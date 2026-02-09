package com.example.springjwt.pantry;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.dto.CustomUserDetails;
import com.example.springjwt.pantry.dto.PantryCreateRequest;
import com.example.springjwt.pantry.dto.PantryResponse;
import com.example.springjwt.pantry.dto.PantryUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/pantries")
@RequiredArgsConstructor
public class PantryController {

    private final PantryService pantryService;

    //냉장고 생성
    @PostMapping
    public ResponseEntity<PantryResponse> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody PantryCreateRequest request
    ) {
        UserEntity user = ((CustomUserDetails) userDetails).getUserEntity();
        PantryResponse created = pantryService.createPantry(user, request);
        return ResponseEntity
                .created(URI.create("/api/pantries/" + created.getId()))
                .body(created);
    }

    //냉장고 조회
    @GetMapping
    public ResponseEntity<List<PantryResponse>> list(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UserEntity user = ((CustomUserDetails) userDetails).getUserEntity();
        return ResponseEntity.ok(pantryService.listPantries(user));
    }

    //단건 조회
    @GetMapping("/{pantryId}")
    public ResponseEntity<PantryResponse> getOne(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long pantryId
    ) {
        var user = ((CustomUserDetails) userDetails).getUserEntity();
        return ResponseEntity.ok(pantryService.getPantry(user, pantryId));
    }

    //냉장고 수정
    @PutMapping("/{pantryId}")
    public ResponseEntity<PantryResponse> update(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long pantryId,
            @RequestBody PantryUpdateRequest request
    ) {
        UserEntity user = ((CustomUserDetails) userDetails).getUserEntity();
        return ResponseEntity.ok(pantryService.updatePantry(user, pantryId, request));
    }

    //냉장고삭제
    @DeleteMapping("/{pantryId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long pantryId
    ) {
        UserEntity user = ((CustomUserDetails) userDetails).getUserEntity();
        pantryService.deletePantry(user, pantryId);
        return ResponseEntity.noContent().build(); // 204 응답
    }

}
