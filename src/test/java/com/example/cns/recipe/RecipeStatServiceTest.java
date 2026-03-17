package com.example.cns.recipe;

import com.example.cns.admin.dto.RecipeStatDTO;
import com.example.cns.admin.enums.StatType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeStatServiceTest {

    @Mock
    RecipeRepository recipeRepository;

    @InjectMocks
    RecipeStatService recipeStatService;

    @Test
    void getCategoryStats_returnsAllCategories_withZeroDefault() {
        // given: DB에 아무 카테고리도 없는 상태
        when(recipeRepository.countByCategory()).thenReturn(List.of());

        // when
        List<RecipeStatDTO> result = recipeStatService.getCategoryStats();

        // then: RecipeCategory enum 값 수만큼 결과 반환, 모두 0
        assertThat(result).hasSize(RecipeCategory.values().length);
        assertThat(result).allMatch(dto -> dto.getCount() == 0L);
    }

    @Test
    void getRecipeStats_yearly_appendsMonthSuffix() {
        // given
        when(recipeRepository.countByYear(2024))
                .thenReturn(List.of(new Object[]{1, 5L}, new Object[]{2, 3L}));

        // when
        List<RecipeStatDTO> result = recipeStatService.getRecipeStats(
                StatType.YEARLY, null, null, 2024, null);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getLabel()).isEqualTo("1월");
        assertThat(result.get(0).getCount()).isEqualTo(5L);
        assertThat(result.get(1).getLabel()).isEqualTo("2월");
    }

    @Test
    void getRecipeStats_missingParams_throwsIllegalArgument() {
        // YEARLY인데 year 누락
        assertThatThrownBy(() ->
                recipeStatService.getRecipeStats(StatType.YEARLY, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("유효하지 않은 파라미터");
    }

    @Test
    void getRecipeStats_daily_usesDateRange() {
        // given
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end   = LocalDate.of(2024, 1, 31);
        List<Object[]> rows = Collections.singletonList(new Object[]{"2024-01-15", 2L});
        when(recipeRepository.countByDateRange(
                start.atStartOfDay(), end.atTime(23, 59, 59)))
                .thenReturn(rows);

        // when
        List<RecipeStatDTO> result = recipeStatService.getRecipeStats(
                StatType.DAILY, start, end, null, null);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCount()).isEqualTo(2L);
    }
}
