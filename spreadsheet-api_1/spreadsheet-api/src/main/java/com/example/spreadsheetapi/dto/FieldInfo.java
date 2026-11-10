package com.example.spreadsheetapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** One field defined in the tax file (used to fill the "add a field" combo box). */
public record FieldInfo(
        @JsonProperty("FieldId") long fieldId,
        @JsonProperty("Name") String name,
        @JsonProperty("Length") int length,
        @JsonProperty("Validation") String validation) {
}
