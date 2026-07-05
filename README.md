# Stream Batch Insert — Excel & CSV Generation Utilities

A lightweight, high-performance, memory-efficient Java library designed to write large datasets into **Excel (.xlsx)** and **CSV** formats directly from collections of POJOs (Plain Old Java Objects).

By leveraging **Apache POI SXSSF** (Streaming Usermodel API) for Excel and **Java MethodHandles** for dynamic property extraction, this library achieves lightning-fast serialization with minimal heap footprint, preventing `OutOfMemoryError` even when exporting millions of rows.

---

## Features

- **Memory Efficient**: Streams rows directly to files, byte buffers, or network response streams. Only a small, configurable window of rows lives in heap memory at any time.
- **High-Performance Reflection**: Utilizes `MethodHandle` getters which are 3-5x faster than standard Java reflection once inlined by the JIT compiler.
- **RFC 4180 Compliance**: Properly handles CSV escaping (fields containing commas, quotes, carriage returns, or line feeds are double-quoted, and inner double-quotes are doubled).
- **Flexible Exporters**:
  - Save directly to a file on disk (perfect for background processes or cron jobs).
  - Return as a `byte[]` (ideal for small-to-medium files where size estimation is needed).
  - Return as a Base64-encoded String (useful for JSON APIs or email attachments).
  - Pipe directly into any `OutputStream` (recommended for Spring Boot / REST streaming response downloads).
- **Selective & Ordered Headers**: Supply custom column names to filter and order fields dynamically, writing only what you need.

---

## Installation

Add the library to your `pom.xml` dependencies:

```xml
<dependency>
    <groupId>io.github.prajyot004</groupId>
    <artifactId>Excel-utility</artifactId>
    <version>1.0.0</version>
</dependency>
```

Ensure you have **Java 17 or higher** configured.

---

## Quick Start Examples

Suppose we have a standard DTO / POJO representing a user:

```java
public class UserRecord {
    private final Long id;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final Integer age;
    private final LocalDate createdDate;

    public UserRecord(Long id, String firstName, String lastName, String email, Integer age, LocalDate createdDate) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.age = age;
        this.createdDate = createdDate;
    }

    // Getters for all fields...
}
```

### 1. CSV Generation (`CsvGenerationUtil`)

#### A. Write directly to disk
Best for batch processes, scheduled operations, or CLI tools.
```java
CsvGenerationUtil csvUtil = new CsvGenerationUtil();
List<UserRecord> users = fetchUsersFromDatabase();

csvUtil.generateCsvToFile(users, UserRecord.class, "users.csv");
```

#### B. Stream directly to HTTP response in a Spring Boot Controller
Pipes rows straight from the DB/collection into the HTTP socket with a 256 KB buffer—**recommmended for large files** as it prevents keeping the entire file in the server's memory.
```java
import io.github.prajyotsable.excel.CsvGenerationUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
public class DownloadController {

    private final CsvGenerationUtil csvUtil = new CsvGenerationUtil();

    @GetMapping("/api/users/download/csv")
    public ResponseEntity<StreamingResponseBody> downloadCsv() {
        List<UserRecord> users = fetchUsers();

        StreamingResponseBody body = outputStream -> {
            // Write directly to network stream (do not close outputStream inside)
            csvUtil.streamCsvToResponse(users, UserRecord.class, outputStream);
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"users.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(body);
    }
}
```

#### C. Get CSV as a byte array (with progress bar compatibility)
Useful if you want to set the `Content-Length` header in a REST response so the client browser displays a download progress percentage.
```java
byte[] csvBytes = csvUtil.generateCsvAsBytes(users, UserRecord.class);

return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"users.csv\"")
        .contentLength(csvBytes.length)
        .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
        .body(csvBytes);
```

#### D. Select custom headers / Re-order columns
```java
List<String> headers = List.of("id", "email", "firstName"); // only exports these 3 columns in this exact order
csvUtil.generateCsvToFile(users, UserRecord.class, headers, "custom_users.csv");
```

---

### 2. Excel Generation (`ExcelGenerationUtil`)

#### A. Write directly to disk
```java
ExcelGenerationUtil excelUtil = new ExcelGenerationUtil();
List<UserRecord> users = fetchUsersFromDatabase();

excelUtil.generateExcel(users, UserRecord.class, "users.xlsx");
```

#### B. Stream Excel to HTTP response in a Spring Boot Controller
Uses a streaming XML builder with a default sliding row window size of `500` rows.
```java
import io.github.prajyotsable.excel.ExcelGenerationUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
public class ExcelDownloadController {

    private final ExcelGenerationUtil excelUtil = new ExcelGenerationUtil();

    @GetMapping("/api/users/download/excel")
    public ResponseEntity<StreamingResponseBody> downloadExcel() {
        List<UserRecord> users = fetchUsers();

        StreamingResponseBody body = outputStream -> {
            excelUtil.streamExcelToResponse(users, UserRecord.class, outputStream);
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"users.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }
}
```

#### C. Custom ordered columns in Excel
```java
List<String> headers = List.of("id", "lastName", "age");
excelUtil.generateExcel(users, UserRecord.class, headers, "custom_users.xlsx");
```

---

## Best Practices & Performance Tuning

1. **Streaming vs. Byte Array**:
   - For small exports (< 10,000 rows), `generateExcelAsBytes()` is simple and allows you to set file size indicators.
   - For large exports (> 50,000 rows), always prefer `streamExcelToResponse()` or `streamCsvToResponse()` to avoid high JVM garbage collector pressure.
2. **POI Temp Files**:
   - `ExcelGenerationUtil` writes temporary XML fragments to disk to maintain its low memory profile. If you run in containerized environments (like Docker), ensure `/tmp` (or the default system temp directory) has enough write permissions and disk space.
3. **Avoid Rich Styling in Millions of Rows**:
   - Cell styles are extremely memory intensive in Excel files. The library includes a lightweight, pre-configured bold header style, but keeps data rows raw and unstyled to maximize write speeds.
