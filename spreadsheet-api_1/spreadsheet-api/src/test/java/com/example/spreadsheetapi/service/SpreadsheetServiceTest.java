package com.example.spreadsheetapi.service;

import com.example.spreadsheetapi.dto.FieldValue;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpreadsheetServiceTest {

    @Test
    void extractsHighlightedRowsAndPadsNumericValues() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/Tester.xlsx")) {
            List<FieldValue> out = new SpreadsheetService().extract(in);
            assertEquals(List.of(
                    new FieldValue(180, "20261009"),              // CCYYMMDD: not numeric, untouched
                    new FieldValue(181, "000000127999991"),       // Numeric, length 15
                    new FieldValue(182, "000000388235810"),
                    new FieldValue(263, "00000000000127999991"),  // Numeric, length 20
                    new FieldValue(264, "00000000000388235810"),
                    new FieldValue(266, "00000000000260235819"),
                    new FieldValue(298, "000000388235810")        // uses the red value, not the Value column
            ), out);
        }
    }
}
