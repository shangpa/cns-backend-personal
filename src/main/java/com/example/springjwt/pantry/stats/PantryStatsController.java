package com.example.springjwt.pantry.stats;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.dto.CustomUserDetails;
import com.example.springjwt.pantry.PantryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/pantry/stats")
@RequiredArgsConstructor
public class PantryStatsController {

    private final PantryStatsService statsService;
    private final PantryRepository pantryRepository;

    @GetMapping("/overview")
    public ResponseEntity<PantryStatsResponse> overview(
            @AuthenticationPrincipal UserDetails ud,
            @RequestParam(required = false) Long pantryId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to
    ) {
        UserEntity user = ((CustomUserDetails) ud).getUserEntity();

        Long pid = (pantryId != null)
                ? pantryRepository.findByIdAndUser_Id(pantryId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("해당 냉장고가 없습니다. id=" + pantryId)).getId()
                : pantryRepository.findFirstByUser_IdAndIsDefaultTrue(user.getId())
                .orElseGet(() -> pantryRepository.findAllByUser_IdOrderBySortOrderAscCreatedAtAsc(user.getId())
                        .stream().findFirst()
                        .orElseThrow(() -> new IllegalStateException("등록된 냉장고가 없습니다.")))
                .getId();

        var o = statsService.getOverview(pid, from, to);
        return ResponseEntity.ok(new PantryStatsResponse(
                o.nowCount(),
                o.eatenCount(),
                o.expiredCount(),
                o.categoryCounts(),
                o.topConsumedName()
        ));
    }

    // ✅ 새 DTO
    public record PantryStatsResponse(
            Long nowCount,
            Long eatenCount,
            Long expiredCount,
            Map<String, Long> categoryCounts,
            String topConsumedName
    ) {}
}
