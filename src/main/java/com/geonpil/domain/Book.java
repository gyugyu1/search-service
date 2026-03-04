package com.geonpil.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class Book {
    private Long bookId;
    private String isbn;
    private String title;
    private List<String> authors;
    private List<String> translators;
    private String publisher;
    private String thumbnail;
    private String contents;
    private OffsetDateTime datetime;
    private Integer price;
    @JsonProperty("sale_price")
    private Integer salePrice;
    private String category;
    private String status;
    private String url;
    private OffsetDateTime createdAt;
    private String processedAutors;
    private double rating;

    public void setAuthors(List<String> authors) {
        this.authors = authors;
        this.processedAutors = getDisplayAuthor(authors);
    }

    public String getAuthorsString() {
        return authors != null ? String.join(", ", authors) : "";
    }

    public String getTranslatorsString() {
        if (translators != null && !translators.isEmpty()) {
            return String.join(", ", translators);
        }
        return "";
    }

    public static String getDisplayAuthor(List<String> authors) {
        if (authors == null || authors.isEmpty()) return "작자 미상";
        if (authors.size() == 1) return authors.get(0);
        return authors.get(0) + " 외 " + (authors.size() - 1) + "명";
    }
}
