package com.geonpil.dto.bookSearch;

import com.geonpil.domain.Book;
import lombok.Data;

import java.util.List;

@Data
public class BookSearchResponse {
    private Book document;
    private List<Book> documents;
    private Meta meta;
}
