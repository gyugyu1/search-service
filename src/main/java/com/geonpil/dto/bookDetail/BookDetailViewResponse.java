package com.geonpil.dto.bookDetail;

import lombok.Data;

@Data
public class BookDetailViewResponse {
    private Long bookId;
    private String title;
    private String authors;
    private String translators;
    private String publisher;
    private String contents;
    private String thumbnail;
    private String isbn;
    private String isbn10;
    private String isbn13;
    private Integer price;
    private Integer salePrice;
    private String category;
    private String status;
    private String url;
    private String processedAutors;
    private double rating;
}
