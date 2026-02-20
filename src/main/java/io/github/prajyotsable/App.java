package io.github.prajyotsable;

import io.github.prajyotsable.excel.ExcelGenerationUtil;
import io.github.prajyotsable.excel.TestDataGenerator;
import io.github.prajyotsable.excel.UserRecord;

import java.io.IOException;
import java.util.Date;
import java.util.List;

public class App {

    public static void main(String[] args) throws IOException {

        // Generate 10 000 sample records
        List<UserRecord> users = TestDataGenerator.generateUsers(1000000);


        ExcelGenerationUtil util = new ExcelGenerationUtil();
        long start = System.nanoTime();

        System.out.println("Excel generation started at: " + new Date());

        util.generateExcel(users, UserRecord.class, "users.xlsx");

        long end = System.nanoTime();

        System.out.println("Excel generation finished at: " + new Date());

        long durationInMillis = (end - start) / 1_000_000;

        System.out.println("Excel generated in: " + durationInMillis + " ms");

    }
}
