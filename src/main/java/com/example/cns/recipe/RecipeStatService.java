package com.example.cns.recipe;

import com.example.cns.admin.dto.BoardMonthlyStatsDTO;
import com.example.cns.admin.dto.RecipeMonthlyStatsDTO;
import com.example.cns.admin.dto.RecipeStatDTO;
import com.example.cns.admin.enums.StatType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecipeStatService {

    private final RecipeRepository recipeRepository;

    @Transactional(readOnly = true)
    public List<RecipeStatDTO> getCategoryStats() {
        List<Object[]> raw = recipeRepository.countByCategory();

        Map<RecipeCategory, Long> map = raw.stream()
                .collect(Collectors.toMap(
                        obj -> (RecipeCategory) obj[0],
                        obj -> (Long) obj[1]
                ));

        List<RecipeStatDTO> result = new ArrayList<>();
        for (RecipeCategory category : RecipeCategory.values()) {
            long count = map.getOrDefault(category, 0L);
            result.add(new RecipeStatDTO(category.name(), count));
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<RecipeStatDTO> getMonthlyCategoryStatsByName(String category) {
        try {
            RecipeCategory enumCategory = RecipeCategory.valueOf(category);
            return recipeRepository.countMonthlyBySpecificCategory(enumCategory).stream()
                    .map(obj -> new RecipeStatDTO(obj[0].toString(), (Long) obj[1]))
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 카테고리입니다: " + category);
        }
    }

    @Transactional(readOnly = true)
    public List<RecipeMonthlyStatsDTO> getRecentFourMonthsStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fourMonthsAgo = now.minusMonths(3)
                .withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        return recipeRepository.findRecentRecipeCounts(fourMonthsAgo);
    }

    @Transactional(readOnly = true)
    public List<BoardMonthlyStatsDTO> countRecipeMonthly(LocalDateTime startDate) {
        List<Object[]> raw = recipeRepository.countRecipeMonthlyRaw(startDate);
        return raw.stream()
                .map(row -> new BoardMonthlyStatsDTO((String) row[0], (Long) row[1]))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BoardMonthlyStatsDTO> sumRecipeViewsMonthly(LocalDateTime startDate) {
        List<Object[]> raw = recipeRepository.sumRecipeViewsMonthlyRaw(startDate);
        return raw.stream()
                .map(row -> new BoardMonthlyStatsDTO((String) row[0], (Long) row[1]))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RecipeStatDTO> getRecipeStats(StatType type, LocalDate startDate, LocalDate endDate, Integer year, Integer month) {
        if (type == StatType.YEARLY && year != null) {
            return recipeRepository.countByYear(year).stream()
                    .map(obj -> new RecipeStatDTO(obj[0] + "월", (Long) obj[1]))
                    .collect(Collectors.toList());

        } else if (type == StatType.MONTHLY && year != null && month != null) {
            return recipeRepository.countByMonth(year, month).stream()
                    .map(obj -> new RecipeStatDTO(obj[0] + "일", (Long) obj[1]))
                    .collect(Collectors.toList());

        } else if (type == StatType.DAILY && startDate != null && endDate != null) {
            return recipeRepository.countByDateRange(startDate.atStartOfDay(), endDate.atTime(23, 59, 59)).stream()
                    .map(obj -> new RecipeStatDTO(obj[0].toString(), (Long) obj[1]))
                    .collect(Collectors.toList());
        }

        throw new IllegalArgumentException("유효하지 않은 파라미터입니다.");
    }
}
