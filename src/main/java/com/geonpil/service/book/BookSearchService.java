package com.geonpil.service.book;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.geonpil.domain.Book;
import com.geonpil.dto.bookDetail.BookDetailViewResponse;
import com.geonpil.dto.bookSearch.BookSearchResponse;
import com.geonpil.dto.bookSearch.BookSearchViewResponse;
import com.geonpil.util.converter.BookConverterUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookSearchService {

    private final ObjectMapper mapper;

    @Value("${kakao.api.key}")
    private String kakaoApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public BookSearchViewResponse searchBooks(String query, int page, int size) {
        String url = UriComponentsBuilder
                .fromHttpUrl("https://dapi.kakao.com/v3/search/book")
                .queryParam("query", query)
                .queryParam("page", page)
                .queryParam("size", size)
                .build()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoApiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        String body = response.getBody();

        try {
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            BookSearchResponse result = mapper.readValue(body, BookSearchResponse.class);

            List<BookDetailViewResponse> processedBooks = new ArrayList<>();
            if (result.getDocuments() != null) {
                for (Book book : result.getDocuments()) {
                    processedBooks.add(BookConverterUtil.toDetailView(book));
                }
            }

            BookSearchViewResponse bookSearchViewResponse = new BookSearchViewResponse();
            bookSearchViewResponse.setMeta(result.getMeta());
            bookSearchViewResponse.setBooks(processedBooks);

            return bookSearchViewResponse;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 파싱 실패", e);
        }
    }
}
