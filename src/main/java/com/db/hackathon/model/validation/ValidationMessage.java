package com.db.hackathon.model.validation;

import com.db.hackathon.adk.agent.validation.ValidationCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationMessage {

    private String field;

    private ValidationCode code;

    private String message;

    private Integer pageNumber;
}
