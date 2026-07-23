package com.db.hackathon.model.validation;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationError {

    private String field;

    private String message;
}
