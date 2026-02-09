package com.example.springjwt.admin.dto;

public class BoardMonthlyStatsDTO {
    private String yearMonth;
    private Long count;

    public BoardMonthlyStatsDTO(String yearMonth, Long count) {
        this.yearMonth = yearMonth;
        this.count = count;
    }

    // getter, setter 필수
    public String getYearMonth() {
        return yearMonth;
    }

    public void setYearMonth(String yearMonth) {
        this.yearMonth = yearMonth;

    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
    @Override
    public String toString() {
        return "BoardMonthlyStatsDTO{" +
                "yearMonth='" + yearMonth + '\'' +
                ", count=" + count +
                '}';
    }
}
