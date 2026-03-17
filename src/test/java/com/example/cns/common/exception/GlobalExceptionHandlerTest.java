package com.example.cns.common.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    static class TestBody {
        @NotBlank(message = "이름은 필수입니다")
        private String name;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    @RestController
    static class TestController {
        @GetMapping("/test/not-found")
        public void notFound() { throw new NoSuchElementException("리소스를 찾을 수 없습니다"); }

        @GetMapping("/test/bad-request")
        public void badRequest() { throw new IllegalArgumentException("잘못된 요청입니다"); }

        @GetMapping("/test/forbidden")
        public void forbidden() { throw new AccessDeniedException("접근이 거부되었습니다"); }

        @PostMapping("/test/valid")
        public void validBody(@Valid @RequestBody TestBody body) {}
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void noSuchElement_returns404WithErrorCode() throws Exception {
        mockMvc.perform(get("/test/not-found").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("리소스를 찾을 수 없습니다"));
    }

    @Test
    void illegalArgument_returns400WithErrorCode() throws Exception {
        mockMvc.perform(get("/test/bad-request").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다"));
    }

    @Test
    void accessDenied_returns403WithErrorCode() throws Exception {
        mockMvc.perform(get("/test/forbidden").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void validationFail_returns400WithValidationError() throws Exception {
        // name 필드 누락 → @NotBlank 위반
        mockMvc.perform(post("/test/valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("이름은 필수입니다")));
    }
}
