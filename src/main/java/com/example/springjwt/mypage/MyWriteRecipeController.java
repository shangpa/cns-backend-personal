package com.example.springjwt.mypage;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.User.UserRepository;
import com.example.springjwt.jwt.JWTUtil;
import com.example.springjwt.mypage.MyWriteRecipeDTO;
import com.example.springjwt.mypage.MyWriteRecipeResponseDTO;
import com.example.springjwt.mypage.MyWriteRecipeService;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/user/recipes")
public class MyWriteRecipeController {

    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;
    private final MyWriteRecipeService myWriteRecipeService;

    public MyWriteRecipeController(
            JWTUtil jwtUtil,
            UserRepository userRepository,
            @Qualifier("myWriteRecipeServiceImpl") MyWriteRecipeService myWriteRecipeService
    ) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.myWriteRecipeService = myWriteRecipeService;
    }


    @GetMapping
    public ResponseEntity<MyWriteRecipeResponseDTO> getMyRecipes(
            @RequestHeader("Authorization") String token,
            @RequestParam String sort,
            @RequestParam(required = false) List<String> categories,
            @RequestParam(required = false) Long userId   // ✅ 추가
    ) {
        // 기존 로직 그대로
        String username = jwtUtil.getUsername(token);
        UserEntity me = userRepository.findByUsername(username);
        if (me == null) {
            return ResponseEntity.badRequest().build();
        }

        // ✅ 타겟 유저 결정: 파라미터 없으면 나(me), 있으면 해당 userId
        UserEntity target = (userId == null)
                ? me
                : userRepository.findById(Math.toIntExact(userId)).orElse(null);
        if (target == null) {
            return ResponseEntity.badRequest().build();
        }

        // 기존 카테고리/재료 분리 로직 유지
        List<String> categoryFilters = new ArrayList<>();
        List<String> ingredientFilters = new ArrayList<>();
        if (categories != null) {
            for (String filter : categories) {
                String enumVal = convertToEnumFormat(filter);
                if (enumVal != null) categoryFilters.add(enumVal);
                else ingredientFilters.add(filter);
            }
        }

        // ✅ 서비스는 그대로 사용하되, 대상 유저 id만 타겟으로 교체
        List<MyWriteRecipeDTO> dtoList =
                myWriteRecipeService.getMyRecipes(target.getId(), sort, categoryFilters, ingredientFilters);

        return ResponseEntity.ok(new MyWriteRecipeResponseDTO(dtoList.size(), dtoList));
    }

    private String extractBearerToken(String authHeader) {
        if (authHeader == null) return null;
        // "Bearer xxx" or "Bearerxxx" 둘 다 방어
        return authHeader.replaceFirst("(?i)^Bearer\\s+", "");
    }

    // ✅ 여기 아래에 추가해줘
    private String convertToEnumFormat(String text) {
        switch (text) {
            case "한식": return "koreaFood";
            case "중식": return "chineseFood";
            case "일식": return "japaneseFood";
            case "양식": return "westernFood";
            case "채식": return "vegetarianDiet";
            case "간식": return "snack";
            case "안주": return "alcoholSnack";
            case "밑반찬": return "sideDish";
            case "기타": return "etc";
            default: return null;
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMyRecipe(
            @RequestHeader("Authorization") String token,
            @PathVariable("id") int recipeId
    ) {
        String username = jwtUtil.getUsername(token);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }

        myWriteRecipeService.deleteMyRecipe(recipeId, user.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<MyWriteRecipeDTO> getMyRecipeDetail(
            @RequestHeader("Authorization") String token,
            @PathVariable("id") int recipeId
    ) {
        String username = jwtUtil.getUsername(token);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(myWriteRecipeService.getRecipeDetail(recipeId, user.getId()));
    }


}
