package com.db.hackathon.model.extraction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtractedField {

    private String value;

    private Integer pageNumber;

    private Double confidence;

    private String sourceText;

}
