package com.geonpil.dto.bookSearch;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class BookSearchLogDoc {
    private final String keyword;
    private Long userId;
    private String ip;
    private String userAgent;
    private Instant searchedAt;
}
