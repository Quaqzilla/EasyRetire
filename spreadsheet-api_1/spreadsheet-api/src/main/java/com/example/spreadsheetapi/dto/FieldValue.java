package com.example.spreadsheetapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** One output item: {"FieldId": 181, "Value": "000000127999991"}. */
public record FieldValue(
        @JsonProperty("FieldId") long fieldId,
        @JsonProperty("Value") String value) {
}
