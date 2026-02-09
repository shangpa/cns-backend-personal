package com.example.springjwt.fridge;

public enum UnitCategory {
    WEIGHT(new String[] {"kg", "g", "근", "푼"}),
    VOLUME(new String[] {"mL", "L", "스푼", "컵"}),
    COUNT(new String[] {"알", "봉지", "포기", "개", "통", "장", "마리"});

    private final String[] validDetails;

    UnitCategory(String[] validDetails) {
        this.validDetails = validDetails;
    }

    public String[] getValidDetails() {
        return validDetails;
    }

    // 단위 세부 항목이 유효한지 체크하는 메서드
    public boolean isValidDetail(String detail) {
        for (String validDetail : validDetails) {
            if (validDetail.equalsIgnoreCase(detail)) {
                return true;
            }
        }
        return false;
    }
}
