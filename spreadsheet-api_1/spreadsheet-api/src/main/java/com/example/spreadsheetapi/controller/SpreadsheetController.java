package com.example.spreadsheetapi.controller;

import com.example.spreadsheetapi.dto.AnalysisResult;
import com.example.spreadsheetapi.dto.FieldValue;
import com.example.spreadsheetapi.service.SpreadsheetService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@CrossOrigin(origins = "*") // restrict to the front end's address before this leaves your machine
@RestController
@RequestMapping("/api/spreadsheet")
public class SpreadsheetController {

    private final SpreadsheetService service;

    public SpreadsheetController(SpreadsheetService service) {
        this.service = service;
    }

    /** Original endpoint: just FieldId + Value for the highlighted rows. */
    @PostMapping(value = "/highlighted", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public List<FieldValue> highlighted(@RequestParam("file") MultipartFile file) throws IOException {
        try (InputStream in = file.getInputStream()) {
            return service.extract(in);
        }
    }

    /** For the business-team page: highlighted rows plus every field in the file. */
    @PostMapping(value = "/analyse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public AnalysisResult analyse(@RequestParam("file") MultipartFile file) throws IOException {
        try (InputStream in = file.getInputStream()) {
            return service.analyse(in);
        }
    }
}
