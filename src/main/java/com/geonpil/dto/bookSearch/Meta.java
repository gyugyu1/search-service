package com.geonpil.dto.bookSearch;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Meta {
    private int total_count;
    private int pageable_count;

    @JsonProperty("is_end")
    private boolean is_end;
}
