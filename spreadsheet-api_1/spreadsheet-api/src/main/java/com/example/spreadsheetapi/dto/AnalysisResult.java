package com.example.spreadsheetapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AnalysisResult(
        @JsonProperty("ClaimId") String claimId,       // parsed from the sheet name, e.g. "Claim-9899-TaxFile" -> "9899"
        @JsonProperty("SheetName") String sheetName,
        @JsonProperty("Fields") List<FieldInfo> fields, // every distinct field in the file
        @JsonProperty("Found") List<FoundRecord> found) { // highlighted rows
}
