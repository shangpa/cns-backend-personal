package com.example.springjwt.search;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchKeywordService {
    private final SearchKeywordRepository searchKeywordRepository;

    public void saveKeyword(String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            searchKeywordRepository.save(new SearchKeyword(null, keyword.trim(), LocalDateTime.now()));
        }
    }

    public List<String> getTop10Keywords() {
        LocalDateTime since = LocalDateTime.now().minusDays(14); // 최근 14일
        return searchKeywordRepository.findTopKeywordsSince(since, PageRequest.of(0, 10))
                .stream()
                .map(obj -> (String) obj[0])
                .collect(Collectors.toList());
    }
}
