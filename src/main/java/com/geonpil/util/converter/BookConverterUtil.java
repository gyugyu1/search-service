package com.geonpil.util.converter;

import com.geonpil.domain.Book;
import com.geonpil.dto.bookDetail.BookDetailViewResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.geonpil.util.IsbnUtil.extractIsbn10;
import static com.geonpil.util.IsbnUtil.extractIsbn13;

public class BookConverterUtil {

    public static BookDetailViewResponse toDetailView(Book book) {
        BookDetailViewResponse dto = new BookDetailViewResponse();
        dto.setBookId(book.getBookId());
        dto.setTitle(book.getTitle());
        dto.setAuthors(book.getAuthorsString());
        dto.setTranslators(book.getTranslatorsString());
        dto.setPublisher(book.getPublisher());
        dto.setContents(book.getContents());
        dto.setThumbnail(book.getThumbnail());
        dto.setIsbn(book.getIsbn());
        dto.setIsbn10(extractIsbn10(book.getIsbn()));
        dto.setIsbn13(extractIsbn13(book.getIsbn()));
        dto.setPrice(book.getPrice());
        dto.setSalePrice(book.getSalePrice());
        dto.setCategory(book.getCategory());
        dto.setStatus(book.getStatus());
        dto.setUrl(book.getUrl());
        dto.setProcessedAutors(book.getProcessedAutors());
        dto.setRating(book.getRating());
        return dto;
    }
}
