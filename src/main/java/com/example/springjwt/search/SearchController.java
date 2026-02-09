// SearchController.java
package com.example.springjwt.search;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchKeywordService searchKeywordService;

    @GetMapping("/popular-keywords")
    public ResponseEntity<List<String>> getPopularKeywords() {
        List<String> topKeywords = searchKeywordService.getTop10Keywords();
        return ResponseEntity.ok(topKeywords);
    }

    @PostMapping("/save")
    public ResponseEntity<Void> saveKeyword(@RequestParam("keyword") String keyword) {
        searchKeywordService.saveKeyword(keyword);
        return ResponseEntity.ok().build();
    }
}
