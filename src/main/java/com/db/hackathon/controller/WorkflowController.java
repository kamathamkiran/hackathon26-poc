package com.db.hackathon.controller;

import com.db.hackathon.dto.WorkflowResponse;
import com.db.hackathon.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

@RestController
@RequestMapping("/workflow")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    @PostMapping(
            value = "/process",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public void process(
            @RequestParam("filePath") String filePath) {

        //return workflowService.process(filePath);
        return;
    }

}