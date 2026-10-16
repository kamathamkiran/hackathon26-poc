package com.db.hackathon.entity;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class WorkflowEventKey implements Serializable {

    private String workflowId;
    private Integer id;
}
