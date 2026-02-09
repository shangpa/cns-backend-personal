package com.example.springjwt.pantry.stats;

import com.example.springjwt.pantry.PantryStockRepository;
import com.example.springjwt.pantry.history.PantryHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PantryStatsService {
    private final PantryStockRepository stockRepo;
    private final PantryHistoryRepository histRepo;

    public Overview getOverview(Long pantryId, LocalDate from, LocalDate to) {
        // 기간(이번 달) – 이미 쓰고 있던 eaten 계산용
        LocalDate f = (from != null) ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate t = (to != null)   ? to   : LocalDate.now();
        var fromDt = f.atStartOfDay();
        var toDt   = t.plusDays(1).atStartOfDay().minusNanos(1);

        // 1) 현재 재고 수
        Long nowCount = nz(stockRepo.countNow(pantryId));

        // 2) 유통기한 못 지킨 수 = 오늘(LocalDate.now()) 기준 지난 재고
        Long expiredCount = nz(stockRepo.countExpiredStocks(pantryId, LocalDate.now()));

        // 3) (기존) 기간 내 먹은 수
        Long eatenCount = nz(histRepo.countEatenInPeriod(pantryId, fromDt, toDt));

        // 4) 카테고리별 현재 재고 분포
        Map<String, Long> categoryCounts = new LinkedHashMap<>();
        List<Object[]> rows = stockRepo.countByCategory(pantryId);
        if (rows != null) {
            for (Object[] r : rows) {
                String cat = r[0] != null ? r[0].toString() : "UNKNOWN";
                long cnt   = (r[1] instanceof Number) ? ((Number) r[1]).longValue() : 0L;
                categoryCounts.put(cat, cnt);
            }
        }

        // 5) 이번 달 가장 많이 먹은 재료명 (이미 추가해 둔 기능)
        String topConsumedName = null;
        var topRows = histRepo.topConsumedInPeriod(pantryId, fromDt, toDt); // ✅ 3개 인자만
        if (topRows != null && !topRows.isEmpty() && topRows.get(0)[0] != null) {
            topConsumedName = topRows.get(0)[0].toString();
        }

        return new Overview(
                nowCount,
                eatenCount,
                expiredCount,
                categoryCounts,
                topConsumedName
        );
    }

    private static Long nz(Long v) { return v == null ? 0L : v; }

    public record Overview(
            Long nowCount,
            Long eatenCount,
            Long expiredCount,
            Map<String, Long> categoryCounts, // ✅ 카테고리 통계
            String topConsumedName
    ) {}
}
