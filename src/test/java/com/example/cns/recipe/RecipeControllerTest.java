package com.example.cns.recipe;

import com.example.cns.User.UserEntity;
import com.example.cns.dto.CustomUserDetails;
import com.example.cns.search.RecipeSearchService;
import com.example.cns.search.SearchKeywordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import com.example.cns.config.SecurityConfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = RecipeController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(RecipeControllerTest.TestSecurityConfig.class)
class RecipeControllerTest {

    /** 세션 기반(IF_REQUIRED)으로 대체 — authentication() 포스트 프로세서가 세션에 저장하는 방식 사용 */
    @TestConfiguration
    @EnableWebSecurity
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
            return http.build();
        }
    }

    @Autowired MockMvc mockMvc;

    @MockBean RecipeService recipeService;
    @MockBean RecipeDraftService recipeDraftService;
    @MockBean RecipeRepository recipeRepository;
    @MockBean RecipeSearchService recipeSearchService;
    @MockBean SearchKeywordService searchKeywordService;
    @MockBean RecommendService recommendService;

    private Authentication authOf(int userId) {
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setUsername("testuser");
        user.setRole("ROLE_USER");
        CustomUserDetails principal = new CustomUserDetails(user);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Test
    void createDraft_authenticated_returns200() throws Exception {
        when(recipeDraftService.createDraftTransactional(any(), any())).thenReturn(10L);

        mockMvc.perform(post("/api/recipes/drafts")
                        .with(authentication(authOf(1)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"테스트 레시피\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.recipeId").value(10));
    }

    @Test
    void createDraft_unauthenticated_returns4xx() throws Exception {
        mockMvc.perform(post("/api/recipes/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"테스트 레시피\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void deleteDraft_authenticated_returns204() throws Exception {
        doNothing().when(recipeDraftService).deleteDraft(anyLong(), anyInt());

        mockMvc.perform(delete("/api/recipes/drafts/1")
                        .with(authentication(authOf(1))))
                .andExpect(status().isNoContent());
    }

    @Test
    void publishDraft_authenticated_returns200() throws Exception {
        Recipe published = Recipe.builder().recipeId(5L).title("완성 레시피").build();
        when(recipeDraftService.publishDraft(anyLong(), any(Boolean.class), anyInt()))
                .thenReturn(published);

        mockMvc.perform(post("/api/recipes/5/publish")
                        .with(authentication(authOf(1)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isPublic\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipeId").value(5));
    }
}
