package io.github.prajyotsable.excel;

import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExcelGenerationUtilTest {

    private ExcelGenerationUtil excelUtil;
    private List<UserRecord> testUsers;

    @BeforeEach
    void setUp() {
        excelUtil = new ExcelGenerationUtil();
        testUsers = TestDataGenerator.generateUsers(5);
    }

    @Test
    void testGenerateExcelToFile(@TempDir Path tempDir) throws IOException {
        Path excelFile = tempDir.resolve("users.xlsx");
        excelUtil.generateExcel(testUsers, UserRecord.class, excelFile.toString());

        assertTrue(Files.exists(excelFile));
        assertTrue(Files.size(excelFile) > 0);

        // Verify content by loading workbook
        try (Workbook workbook = WorkbookFactory.create(excelFile.toFile())) {
            Sheet sheet = workbook.getSheet("UserRecord");
            assertNotNull(sheet);
            assertEquals(6, sheet.getPhysicalNumberOfRows()); // Header + 5 records

            Row header = sheet.getRow(0);
            assertEquals("id", header.getCell(0).getStringCellValue());
            assertEquals("firstName", header.getCell(1).getStringCellValue());

            Row firstRow = sheet.getRow(1);
            assertEquals(1.0, firstRow.getCell(0).getNumericCellValue()); // id is numeric
            assertEquals("Vihaan", firstRow.getCell(1).getStringCellValue()); // firstName is Aarav? Wait, index check. Let's see: i=1 is "Vihaan" (1 % 5 = 1, FIRST_NAMES[1] = "Vihaan")
        }
    }

    @Test
    void testGenerateExcelAsBytes() throws IOException {
        byte[] bytes = excelUtil.generateExcelAsBytes(testUsers, UserRecord.class);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheet("UserRecord");
            assertNotNull(sheet);
            assertEquals(6, sheet.getPhysicalNumberOfRows());
        }
    }

    @Test
    void testGenerateExcelAsBase64() throws IOException {
        String base64 = excelUtil.generateExcelAsBase64(testUsers, UserRecord.class);
        assertNotNull(base64);

        byte[] bytes = Base64.getDecoder().decode(base64);
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheet("UserRecord");
            assertNotNull(sheet);
            assertEquals(6, sheet.getPhysicalNumberOfRows());
        }
    }

    @Test
    void testStreamExcelToResponse() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        excelUtil.streamExcelToResponse(testUsers, UserRecord.class, baos);

        byte[] bytes = baos.toByteArray();
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheet("UserRecord");
            assertNotNull(sheet);
            assertEquals(6, sheet.getPhysicalNumberOfRows());
        }
    }

    @Test
    void testGenerateExcelWithCustomHeaders() throws IOException {
        List<String> headers = List.of("id", "email", "nonExistentField");
        byte[] bytes = excelUtil.generateExcelAsBytes(testUsers, UserRecord.class, headers);

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheet("UserRecord");
            assertNotNull(sheet);
            assertEquals(6, sheet.getPhysicalNumberOfRows());

            Row headerRow = sheet.getRow(0);
            assertEquals("id", headerRow.getCell(0).getStringCellValue());
            assertEquals("email", headerRow.getCell(1).getStringCellValue());
            assertEquals("nonExistentField", headerRow.getCell(2).getStringCellValue());

            Row firstRow = sheet.getRow(1);
            assertEquals(1.0, firstRow.getCell(0).getNumericCellValue());
            assertTrue(firstRow.getCell(1).getStringCellValue().contains("@example.com"));
            // Cell 2 should be blank (CellType.BLANK or empty value)
            Cell blankCell = firstRow.getCell(2);
            assertTrue(blankCell == null || blankCell.getCellType() == CellType.BLANK || blankCell.getStringCellValue().isEmpty());
        }
    }

    @Test
    void testValidateEmptyRecords() {
        assertThrows(IllegalArgumentException.class, () ->
                excelUtil.generateExcelAsBytes(null, UserRecord.class)
        );
        assertThrows(IllegalArgumentException.class, () ->
                excelUtil.generateExcelAsBytes(List.of(), UserRecord.class)
        );
    }
}
