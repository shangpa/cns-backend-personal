package com.example.cns.shorts;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RecipeShortCreateRequest {
    private String title;
    private String videoUrl;
    private String thumbnailUrl;
    private boolean isPublic = true;
}
