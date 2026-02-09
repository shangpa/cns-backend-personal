package com.example.springjwt.recipe;

public class RecipeThumbnailResponse {
    private String imageUrl;

    public RecipeThumbnailResponse(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}