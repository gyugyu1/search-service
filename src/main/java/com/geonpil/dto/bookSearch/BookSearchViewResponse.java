package com.geonpil.dto.bookSearch;

import com.geonpil.dto.bookDetail.BookDetailViewResponse;
import lombok.Data;

import java.util.List;

@Data
public class BookSearchViewResponse {
    private List<BookDetailViewResponse> books;
    private Meta meta;
}
