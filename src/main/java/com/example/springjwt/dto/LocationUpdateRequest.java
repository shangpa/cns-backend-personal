package com.example.springjwt.dto;

import lombok.Data;

@Data
public class LocationUpdateRequest {
    private Double latitude;
    private Double longitude;
}