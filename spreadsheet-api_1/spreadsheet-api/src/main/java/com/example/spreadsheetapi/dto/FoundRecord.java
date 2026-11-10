package com.example.spreadsheetapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** A highlighted row, with the new value already padded to Length. */
public record FoundRecord(
        @JsonProperty("FieldId") long fieldId,
        @JsonProperty("Name") String name,
        @JsonProperty("Length") int length,
        @JsonProperty("Validation") String validation,
        @JsonProperty("OriginalValue") String originalValue,
        @JsonProperty("NewValue") String newValue,   // as typed in the sheet, before padding
        @JsonProperty("Value") String value,         // padded value to send
        @JsonProperty("Comment") String comment) {
}
