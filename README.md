# Tax file resubmission

Reads a claim's tax file (Excel), finds the highlighted rows, and lets the business team
approve them or add missing fields before resubmitting the claim.

## How it works
1. The team uploads the tax file in `frontend/tax-file-resubmission.html`.
2. The Spring Boot service (`POST /api/spreadsheet/analyse`) returns every field in the file and
   the highlighted rows. The new value is the one under the column with no heading.
3. Numeric fields are padded with leading zeros, and alphanumeric fields with leading spaces,
   up to the Length in the file.
4. The team approves or rejects each field, can add others, then resubmits.

## Run the service
Requires JDK 17+ and Maven.

    mvn spring-boot:run

It starts on http://localhost:8080. Test it with:

    curl.exe -F "file=@sample.xlsx" http://localhost:8080/api/spreadsheet/analyse

## Open the page
Open `frontend/tax-file-resubmission.html` in a browser.

## Configuration (top of the page's script)
- `analyseUrl`: where the Spring Boot service runs.
- `resubmit.url` and `buildBody`: the RISE `Support_ResubmitClaimTaxFile` call. Set these from Swagger.
  Resubmitting stays switched off until `resubmit.url` is set.

## Tests
    mvn test

## Rebuilding the page
`python frontend/build.py <path to design-system "project" folder>` rebuilds
`frontend/tax-file-resubmission.html` from `frontend/src/template.html`.

## Before this is shared more widely
- Replace `@CrossOrigin(origins = "*")` in `SpreadsheetController` with the page's real address.
- Host the service somewhere shared, and update `analyseUrl`.
- Never commit real tax files or access tokens.
