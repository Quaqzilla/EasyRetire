package com.example.spreadsheetapi.service;

import com.example.spreadsheetapi.dto.AnalysisResult;
import com.example.spreadsheetapi.dto.FieldInfo;
import com.example.spreadsheetapi.dto.FieldValue;
import com.example.spreadsheetapi.dto.FoundRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the tax-file sheet.
 *  - Every row defines a field (TaxFileFieldId, OriginalName, Length, Validation).
 *  - Highlighted rows are the records to resubmit. Their new value is the value under the column
 *    whose header is empty (any position, any file):
 *      Validation Numeric      -> padded with leading zeros up to Length
 *      Validation Alphanumeric -> padded with leading spaces up to Length
 */
@Service
public class SpreadsheetService {

    static final String COL_FIELD_ID = "TaxFileFieldId";
    static final String COL_LENGTH = "Length";
    static final String COL_VALIDATION = "Validation";
    static final String COL_NAME = "OriginalName";   // optional
    static final String COL_VALUE = "Value";         // optional
    static final String COL_COMMENT = "Comment";     // optional

    // \b stops "Alphanumeric" from matching as "Numeric".
    private static final Pattern NUMERIC = Pattern.compile("\\bnumeric\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern ALPHANUMERIC = Pattern.compile("\\balphanumeric\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CLAIM_IN_NAME = Pattern.compile("claim[-_ ]?(\\d+)", Pattern.CASE_INSENSITIVE);

    private final DataFormatter formatter = new DataFormatter();

    /** FieldId + padded Value for each highlighted row. */
    public List<FieldValue> extract(InputStream in) throws IOException {
        List<FieldValue> out = new ArrayList<>();
        for (FoundRecord f : analyse(in).found()) {
            out.add(new FieldValue(f.fieldId(), f.value()));
        }
        return out;
    }

    public AnalysisResult analyse(InputStream in) throws IOException {
        List<FoundRecord> found = new ArrayList<>();
        Map<Long, FieldInfo> fields = new LinkedHashMap<>();

        try (Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheetAt(0);
            String sheetName = sheet.getSheetName();
            String claimId = claimIdFrom(sheetName);

            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) return new AnalysisResult(claimId, sheetName, List.of(), found);

            // header name -> column index (columns with an empty header are simply not in the map)
            Map<String, Integer> cols = new HashMap<>();
            int lastCol = headerRow.getLastCellNum();
            for (int c = 0; c < lastCol; c++) {
                String h = text(headerRow.getCell(c));
                if (!h.isEmpty()) cols.put(h.toLowerCase(Locale.ROOT), c);
            }
            int idCol = require(cols, COL_FIELD_ID);
            int lenCol = require(cols, COL_LENGTH);
            int valCol = require(cols, COL_VALIDATION);
            Integer nameCol = cols.get(COL_NAME.toLowerCase(Locale.ROOT));
            Integer valueCol = cols.get(COL_VALUE.toLowerCase(Locale.ROOT));
            Integer commentCol = cols.get(COL_COMMENT.toLowerCase(Locale.ROOT));

            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                Cell idCell = row.getCell(idCol);
                Long fieldId = toLong(text(idCell));
                if (fieldId == null) continue;

                int length = toInt(text(row.getCell(lenCol)));
                String validation = text(row.getCell(valCol));
                String name = cellText(row, nameCol);

                fields.putIfAbsent(fieldId, new FieldInfo(fieldId, name, length, validation));

                if (!isHighlighted(idCell)) continue;

                String newValue = newValue(row, headerRow, lastCol);
                if (newValue == null) continue; // highlighted but nothing under an empty header

                String padded = newValue;
                if (NUMERIC.matcher(validation).find()) {
                    padded = pad(newValue, length, '0');
                } else if (ALPHANUMERIC.matcher(validation).find()) {
                    padded = pad(newValue, length, ' ');
                }

                found.add(new FoundRecord(fieldId, name, length, validation,
                        cellText(row, valueCol), newValue, padded, cellText(row, commentCol)));
            }
            return new AnalysisResult(claimId, sheetName, new ArrayList<>(fields.values()), found);
        }
    }

    /**
     * The new value for a row: the value in a column with an empty header.
     * If several unheaded columns hold a value, the red-text one wins; otherwise the first one.
     * Returns null if no unheaded column has a value on this row.
     */
    private String newValue(Row row, Row headerRow, int lastCol) {
        int max = Math.max(lastCol, row.getLastCellNum());
        Cell firstCandidate = null;

        for (int c = 0; c < max; c++) {
            if (!text(headerRow.getCell(c)).isEmpty()) continue; // has a header, not the new-value column
            Cell cell = row.getCell(c);
            if (text(cell).isEmpty()) continue;

            if (isRedText(cell)) return text(cell);
            if (firstCandidate == null) firstCandidate = cell;
        }
        return firstCandidate == null ? null : text(firstCandidate);
    }

    /** Left-pads value with padChar up to length; longer values are returned unchanged. */
    static String pad(String value, int length, char padChar) {
        if (value.length() >= length) return value;
        return String.valueOf(padChar).repeat(length - value.length()) + value;
    }

    static String claimIdFrom(String sheetName) {
        Matcher m = CLAIM_IN_NAME.matcher(sheetName == null ? "" : sheetName);
        return m.find() ? m.group(1) : "";
    }

    private String cellText(Row row, Integer col) {
        return col == null ? "" : text(row.getCell(col));
    }

    private Long toLong(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            return new BigDecimal(s).longValue();
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int toInt(String s) {
        Long l = toLong(s);
        return l == null ? 0 : l.intValue();
    }

    /** Solid fill that is not white / "no colour". */
    private boolean isHighlighted(Cell cell) {
        if (cell == null) return false;
        CellStyle style = cell.getCellStyle();
        if (style.getFillPattern() != FillPatternType.SOLID_FOREGROUND) return false;

        Color color = style.getFillForegroundColorColor();
        if (color instanceof XSSFColor xc) {
            if (xc.isRGB() || xc.getRGB() != null) {
                byte[] rgb = xc.getRGB();
                return !(rgb != null && (rgb[0] & 0xFF) == 255 && (rgb[1] & 0xFF) == 255 && (rgb[2] & 0xFF) == 255);
            }
            if (xc.isThemed()) return xc.getTheme() != 0; // theme 0 = background (white)
            return !xc.isIndexed() || xc.getIndex() != IndexedColors.WHITE.getIndex();
        }
        return color != null; // legacy .xls palette colour
    }

    /** True if the cell's font colour is red (e.g. FFFF0000). */
    private boolean isRedText(Cell cell) {
        Workbook wb = cell.getSheet().getWorkbook();
        Font font = wb.getFontAt(cell.getCellStyle().getFontIndex());
        if (font instanceof XSSFFont xf) {
            XSSFColor c = xf.getXSSFColor();
            if (c != null && c.getRGB() != null) {
                byte[] rgb = c.getRGB();
                return (rgb[0] & 0xFF) >= 200 && (rgb[1] & 0xFF) <= 80 && (rgb[2] & 0xFF) <= 80;
            }
        }
        return false;
    }

    /** Cell text; numbers come out as plain digits (no 1.27E8), strings are trimmed. */
    private String text(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
            case FORMULA -> cell.getCachedFormulaResultType() == CellType.NUMERIC
                    ? BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString()
                    : cell.getStringCellValue().trim();
            case BLANK -> "";
            default -> formatter.formatCellValue(cell).trim();
        };
    }

    private int require(Map<String, Integer> cols, String name) {
        Integer i = cols.get(name.toLowerCase(Locale.ROOT));
        if (i == null) throw new IllegalArgumentException("Column not found: " + name);
        return i;
    }
}
