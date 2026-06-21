package io.github.prajyotsable.excel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvGenerationUtilTest {

    private CsvGenerationUtil csvUtil;
    private List<UserRecord> testUsers;

    @BeforeEach
    void setUp() {
        csvUtil = new CsvGenerationUtil();
        testUsers = TestDataGenerator.generateUsers(5);
    }

    @Test
    void testGenerateCsvToFile(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("users.csv");
        csvUtil.generateCsvToFile(testUsers, UserRecord.class, csvFile.toString());

        assertTrue(Files.exists(csvFile));
        List<String> lines = Files.readAllLines(csvFile);
        assertEquals(6, lines.size()); // Header + 5 records
        assertTrue(lines.get(0).contains("id,firstName,lastName,email,age,createdDate"));
        assertTrue(lines.get(1).contains("Vihaan,Patil,vihaan.patil1@example.com"));
    }

    @Test
    void testGenerateCsvAsBytes() throws IOException {
        byte[] bytes = csvUtil.generateCsvAsBytes(testUsers, UserRecord.class);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        String content = new String(bytes);
        String[] lines = content.split("\r?\n");
        assertEquals(6, lines.length);
        assertEquals("id,firstName,lastName,email,age,createdDate", lines[0]);
    }

    @Test
    void testGenerateCsvAsBase64() throws IOException {
        String base64 = csvUtil.generateCsvAsBase64(testUsers, UserRecord.class);
        assertNotNull(base64);
        assertDoesNotThrow(() -> Base64.getDecoder().decode(base64));

        byte[] decodedBytes = Base64.getDecoder().decode(base64);
        String content = new String(decodedBytes);
        String[] lines = content.split("\r?\n");
        assertEquals(6, lines.length);
    }

    @Test
    void testStreamCsvToResponse() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        csvUtil.streamCsvToResponse(testUsers, UserRecord.class, baos);

        byte[] bytes = baos.toByteArray();
        assertTrue(bytes.length > 0);
        String content = new String(bytes);
        String[] lines = content.split("\r?\n");
        assertEquals(6, lines.length);
    }

    @Test
    void testGenerateCsvWithCustomHeaders() throws IOException {
        List<String> headers = List.of("id", "email", "invalidField");
        byte[] bytes = csvUtil.generateCsvAsBytes(testUsers, UserRecord.class, headers);

        String content = new String(bytes);
        String[] lines = content.split("\r?\n");
        assertEquals(6, lines.length);
        assertEquals("id,email,invalidField", lines[0]);

        // Second line should contain id and email, but invalidField should be blank
        String[] row = lines[1].split(",");
        assertEquals("1", row[0]);
        assertTrue(row[1].contains("@example.com"));
        // Since invalidField doesn't exist, it should be blank (3rd column)
        // If there are exactly 3 elements, it might split differently, let's verify there are 2 commas
        int commas = 0;
        for (char c : lines[1].toCharArray()) {
            if (c == ',') commas++;
        }
        assertEquals(2, commas);
    }

    @Test
    void testValidateEmptyRecords() {
        assertThrows(IllegalArgumentException.class, () ->
                csvUtil.generateCsvAsBytes(null, UserRecord.class)
        );
        assertThrows(IllegalArgumentException.class, () ->
                csvUtil.generateCsvAsBytes(List.of(), UserRecord.class)
        );
    }
}
