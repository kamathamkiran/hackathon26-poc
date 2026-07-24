package com.db.hackathon.model.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewIssue {

    private String field;

    private String previousValue;

    private String updatedValue;

    private Double confidence;

    private Integer pageNumber;

    private String message;
}
