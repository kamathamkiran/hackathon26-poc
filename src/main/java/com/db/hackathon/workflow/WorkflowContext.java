package com.db.hackathon.workflow;

import com.db.hackathon.entity.WorkflowEntity;
import com.db.hackathon.model.aggrement.Agreement;
import com.db.hackathon.model.document.DocumentAnalysis;
import com.db.hackathon.model.validation.ValidationResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowContext {

    private WorkflowEntity workflow;

    private MultipartFile pdf;

    private DocumentAnalysis documentAnalysis;

    private Agreement agreement;

    private ValidationResult validationResult;

}
