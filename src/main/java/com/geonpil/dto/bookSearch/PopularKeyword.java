package com.geonpil.dto.bookSearch;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PopularKeyword {
    private String keyword;
    private Long count;
}
