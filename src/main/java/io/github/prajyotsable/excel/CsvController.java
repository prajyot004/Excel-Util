package io.github.prajyotsable.excel;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * REST endpoint that streams a CSV file directly to the browser.
 *
 * GET /api/users/download/csv
 *
 * Uses StreamingResponseBody so rows are piped straight from the generator
 * into the HTTP response socket — zero intermediate byte[] buffer in heap.
 * Safe for any number of rows.
 */
@RestController
@RequestMapping("/api/users")
public class CsvController {

    private final CsvGenerationUtil   csvUtil   = new CsvGenerationUtil();
    private final ExcelGenerationUtil excelUtil = new ExcelGenerationUtil();

    /**
     * Streams the CSV file to the frontend.
     *
     * The browser receives Content-Disposition: attachment, so it triggers
     * a file-save dialog / automatic download automatically.
     *
     * Endpoint : GET http://localhost:8080/api/users/download/csv
     */
    @GetMapping("/download/csv")
    public ResponseEntity<StreamingResponseBody> downloadCsv() {

        // Replace this with your real service / repository call
        List<UserRecord> records = TestDataGenerator.generateUsers(1_000_000);

        // StreamingResponseBody is a functional interface:
        //   void writeTo(OutputStream outputStream) throws IOException
        // Spring Boot runs it on an async thread and flushes chunks to the
        // client as they are written — the web thread is never blocked.
        StreamingResponseBody responseBody = outputStream ->
                csvUtil.streamCsvToResponse(records, UserRecord.class, outputStream);

        return ResponseEntity.ok()
                // tells the browser: save this as a file named users.csv
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"users.csv\"")
                // no Content-Length — size is unknown until fully written
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(responseBody);
    }

    /**
     * Streams the Excel (.xlsx) file to the frontend.
     *
     * Endpoint : GET http://localhost:8080/api/users/download/xlsx
     */
    @GetMapping("/download/xlsx")
    public ResponseEntity<StreamingResponseBody> downloadExcel() {

        // Replace this with your real service / repository call
        List<UserRecord> records = TestDataGenerator.generateUsers(1_000_000);

        StreamingResponseBody responseBody = outputStream ->
                excelUtil.streamExcelToResponse(records, UserRecord.class, outputStream);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"users.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(responseBody);
    }

    // ── Method 2: generateCsvAsBytes ─────────────────────────────────────────
    /**
     * Returns the entire CSV as a byte[] in the response body.
     * Spring sets Content-Length automatically so the browser shows exact % progress.
     * Capped at 100 000 rows — the full byte[] lives in JVM heap.
     *
     * Endpoint: GET /api/users/download/csv/bytes
     */
    @GetMapping("/download/csv/bytes")
    public ResponseEntity<byte[]> downloadCsvAsBytes() throws IOException {
        List<UserRecord> records = TestDataGenerator.generateUsers(1_000_000);
        byte[] bytes = csvUtil.generateCsvAsBytes(records, UserRecord.class);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"users.csv\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(bytes.length))
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }

    // ── Method 3: generateCsvAsBase64 ────────────────────────────────────────
    /**
     * Returns a JSON body { fileName, data } where data is Base64-encoded CSV.
     * The browser JS decodes it locally and triggers a Blob download — no extra request.
     * Capped at 100 000 rows — Base64 is ~33% larger than raw bytes.
     *
     * Endpoint: GET /api/users/download/csv/base64
     */
    @GetMapping("/download/csv/base64")
    public ResponseEntity<Map<String, String>> downloadCsvAsBase64() throws IOException {
        List<UserRecord> records = TestDataGenerator.generateUsers(1_000_000);
        String base64 = csvUtil.generateCsvAsBase64(records, UserRecord.class);
        return ResponseEntity.ok(Map.of("fileName", "users.csv", "data", base64));
    }

    // ── Method 1: generateCsvToFile ──────────────────────────────────────────
    /**
     * Saves the CSV directly to the server's local filesystem.
     * Returns JSON with the absolute path where the file was written.
     * Nothing is downloaded to the browser — this is a server-side write.
     *
     * Endpoint: GET /api/users/download/csv/file
     */
    @GetMapping("/download/csv/file")
    public ResponseEntity<Map<String, String>> saveCsvToFile() throws IOException {
        List<UserRecord> records = TestDataGenerator.generateUsers(1_000_000);
        String path = "users_export.csv";
        csvUtil.generateCsvToFile(records, UserRecord.class, path);
        return ResponseEntity.ok(Map.of(
                "method",  "generateCsvToFile",
                "message", "File saved on server",
                "path",    new java.io.File(path).getAbsolutePath()
        ));
    }
}
