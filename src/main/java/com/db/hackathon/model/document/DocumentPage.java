package com.db.hackathon.model.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentPage {

    private Integer pageNumber;

    private String text;

    private Integer characterCount;

    private boolean blankPage;

}
