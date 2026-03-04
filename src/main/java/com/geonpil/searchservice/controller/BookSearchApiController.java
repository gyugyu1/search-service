package com.geonpil.searchservice.controller;

import com.geonpil.dto.bookSearch.BookSearchViewResponse;
import com.geonpil.dto.bookSearch.PopularKeyword;
import com.geonpil.service.book.BookSearchLogService;
import com.geonpil.service.book.BookSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 검색 서비스 REST API.
 * geonpil 앱(또는 다른 클라이언트)이 이 API를 호출합니다.
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class BookSearchApiController {

    private final BookSearchService bookSearchService;
    private final BookSearchLogService bookSearchLogService;

    /** 책 검색 */
    @GetMapping("/books")
    public ResponseEntity<BookSearchViewResponse> searchBooks(
            @RequestParam String query,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int size) {
        BookSearchViewResponse result = bookSearchService.searchBooks(query, page, size);
        return ResponseEntity.ok(result);
    }

    /** 자동완성 제안 */
    @GetMapping("/suggestions")
    public ResponseEntity<List<String>> getSuggestions(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit) {
        List<String> suggestions = bookSearchLogService.getSearchSuggestions(q, limit, bookSearchService);
        return ResponseEntity.ok(suggestions);
    }

    /** 인기 검색어 */
    @GetMapping("/popular")
    public ResponseEntity<List<PopularKeyword>> getPopularKeywords(
            @RequestParam(defaultValue = "10") int topN) {
        List<PopularKeyword> list = bookSearchLogService.getPopularKeywords(topN);
        return ResponseEntity.ok(list);
    }

    /** 검색 로그 저장 (geonpil에서 검색 시 호출) */
    @PostMapping("/log")
    public ResponseEntity<Void> logSearch(@RequestBody LogSearchRequest request) {
        bookSearchLogService.logSearch(
                request.getKeyword(),
                request.getUserId(),
                request.getIp(),
                request.getUserAgent()
        );
        return ResponseEntity.ok().build();
    }

    @lombok.Data
    public static class LogSearchRequest {
        private String keyword;
        private Long userId;
        private String ip;
        private String userAgent;
    }
}
