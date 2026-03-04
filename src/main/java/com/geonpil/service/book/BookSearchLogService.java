package com.geonpil.service.book;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import com.geonpil.dto.bookDetail.BookDetailViewResponse;
import com.geonpil.dto.bookSearch.BookSearchLogDoc;
import com.geonpil.dto.bookSearch.BookSearchViewResponse;
import com.geonpil.dto.bookSearch.PopularKeyword;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookSearchLogService {

    private static final String INDEX = "book_search_log";

    private final ElasticsearchClient esClient;

    @PostConstruct
    public void ensureIndexExists() {
        try {
            boolean exists = esClient.indices().exists(ExistsRequest.of(e -> e.index(INDEX))).value();

            if (!exists) {
                log.info("book_search_log 인덱스가 없어 생성합니다.");
                createIndex();
            } else {
                try {
                    var mapping = esClient.indices().getMapping(m -> m.index(INDEX));
                    var indexMapping = mapping.get(INDEX);

                    if (indexMapping == null || indexMapping.mappings() == null) {
                        log.debug("book_search_log 인덱스 매핑 정보를 확인할 수 없습니다.");
                        return;
                    }

                    var properties = indexMapping.mappings().properties();
                    if (properties == null || !properties.containsKey("keyword")) {
                        log.debug("book_search_log 인덱스가 이미 존재합니다.");
                        return;
                    }

                    var keywordProp = properties.get("keyword");
                    if (keywordProp == null) {
                        log.debug("book_search_log 인덱스가 이미 존재합니다.");
                        return;
                    }

                    if (keywordProp._kind() == co.elastic.clients.elasticsearch._types.mapping.Property.Kind.Text) {
                        log.warn("book_search_log 인덱스의 keyword 필드가 text 타입입니다. aggregation을 위해 재생성합니다.");
                        esClient.indices().delete(DeleteIndexRequest.of(d -> d.index(INDEX)));
                        createIndex();
                    } else {
                        log.debug("book_search_log 인덱스가 이미 존재하고 올바른 매핑을 가지고 있습니다.");
                    }
                } catch (Exception e) {
                    log.warn("인덱스 매핑 확인 실패, 인덱스를 재생성합니다: {}", e.getMessage());
                    try {
                        esClient.indices().delete(DeleteIndexRequest.of(d -> d.index(INDEX)));
                    } catch (Exception deleteEx) {
                        log.warn("인덱스 삭제 실패 (무시): {}", deleteEx.getMessage());
                    }
                    createIndex();
                }
            }
        } catch (Exception e) {
            log.error("book_search_log 인덱스 생성 실패: {}", e.getMessage(), e);
        }
    }

    private void createIndex() {
        try {
            esClient.indices().create(CreateIndexRequest.of(c -> c
                    .index(INDEX)
                    .mappings(TypeMapping.of(m -> m
                            .properties("keyword", Property.of(p -> p.keyword(k -> k)))
                            .properties("userId", Property.of(p -> p.long_(l -> l)))
                            .properties("ip", Property.of(p -> p.keyword(k -> k)))
                            .properties("userAgent", Property.of(p -> p.keyword(k -> k)))
                            .properties("searchedAt", Property.of(p -> p.date(d -> d.format("strict_date_optional_time||epoch_millis"))))
                    ))
            ));

            log.info("book_search_log 인덱스 생성 완료");
        } catch (Exception e) {
            log.error("인덱스 생성 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("인덱스 생성 실패", e);
        }
    }

    @Async
    public void logSearch(String keyword, Long userId, String ip, String userAgent) {
        String normalized = normalizeKeyword(keyword);

        if (normalized == null) {
            log.debug("검색어가 null이거나 비어있어 로깅을 건너뜁니다.");
            return;
        }

        BookSearchLogDoc doc = new BookSearchLogDoc(normalized, userId, ip, trimUserAgent(userAgent), Instant.now());

        try {
            log.debug("Elasticsearch에 검색 로그 저장 시도: keyword={}, userId={}, index={}", normalized, userId, INDEX);
            esClient.index(i -> i
                    .index(INDEX)
                    .document(doc));
            log.debug("검색 로그 저장 성공: keyword={}", normalized);
        } catch (Exception e) {
            log.error("검색 로그 저장 실패: keyword={}, error={}", normalized, e.getMessage(), e);
        }
    }

    public List<PopularKeyword> getPopularKeywords(int topN) {
        try {
            log.debug("인기 검색어 조회 시도: topN={}, index={}", topN, INDEX);

            boolean indexExists = esClient.indices().exists(ExistsRequest.of(e -> e.index(INDEX))).value();
            if (!indexExists) {
                log.debug("인덱스가 존재하지 않아 빈 리스트 반환: index={}", INDEX);
                return new ArrayList<>();
            }

            var response = esClient.search(s -> s
                            .index(INDEX)
                            .size(0)
                            .query(Query.of(q -> q.matchAll(m -> m)))
                            .aggregations("popular_keywords", a -> a
                                    .terms(t -> t
                                            .field("keyword")
                                            .size(topN)
                                    )
                            ),
                    BookSearchLogDoc.class
            );

            List<PopularKeyword> popularKeywords = new ArrayList<>();

            if (response.aggregations() != null && response.aggregations().containsKey("popular_keywords")) {
                var agg = response.aggregations().get("popular_keywords");
                if (agg.isSterms()) {
                    var termsAgg = agg.sterms();
                    if (termsAgg != null && termsAgg.buckets() != null) {
                        for (StringTermsBucket bucket : termsAgg.buckets().array()) {
                            popularKeywords.add(new PopularKeyword(
                                    bucket.key().stringValue(),
                                    bucket.docCount()
                            ));
                        }
                    }
                }
            }

            log.debug("인기 검색어 조회 성공: count={}", popularKeywords.size());
            return popularKeywords;

        } catch (Exception e) {
            log.error("인기 검색어 조회 실패: error={}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public List<String> getSearchSuggestions(String prefix, int limit, BookSearchService bookSearchService) {
        try {
            if (prefix == null || prefix.trim().isEmpty()) {
                return new ArrayList<>();
            }

            String normalizedPrefix = normalizeKeyword(prefix);
            if (normalizedPrefix == null) {
                return new ArrayList<>();
            }

            log.debug("검색어 자동완성 조회 시도: prefix={}, limit={}", normalizedPrefix, limit);

            BookSearchViewResponse searchResponse = bookSearchService.searchBooks(normalizedPrefix, 1, limit * 2);

            if (searchResponse == null || searchResponse.getBooks() == null || searchResponse.getBooks().isEmpty()) {
                log.debug("검색어 자동완성 조회 결과 없음: prefix={}", normalizedPrefix);
                return new ArrayList<>();
            }

            List<String> suggestions = searchResponse.getBooks().stream()
                    .map(BookDetailViewResponse::getTitle)
                    .filter(title -> title != null && !title.isEmpty())
                    .filter(title -> title.toLowerCase().startsWith(normalizedPrefix.toLowerCase()))
                    .distinct()
                    .limit(limit)
                    .collect(Collectors.toList());

            log.debug("검색어 자동완성 조회 성공: count={}", suggestions.size());
            return suggestions;

        } catch (Exception e) {
            log.error("검색어 자동완성 조회 실패: prefix={}, error={}", prefix, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isEmpty()) return null;
        String k = keyword.trim();
        if (k.isEmpty()) return null;
        if (k.length() > 200) k = k.substring(0, 200);
        return k;
    }

    private String trimUserAgent(String ua) {
        if (ua == null) return null;
        ua = ua.trim();
        if (ua.length() > 500) ua = ua.substring(0, 500);
        return ua;
    }
}
