package com.example.cns.admin.dto;


public class RecipeMonthlyStatsDTO {
    private String month;
    private long count;

    public RecipeMonthlyStatsDTO(String month, long count) {
        this.month = month;
        this.count = count;
    }
    public String getMonth() {
        return month;
    }

    public long getCount() {
        return count;
    }
}
