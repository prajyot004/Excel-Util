package io.github.prajyotsable.excel;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class TestDataGenerator {

    private static final String[] FIRST_NAMES = {
            "Aarav", "Vihaan", "Aditya", "Ishaan", "Krishna"
    };

    private static final String[] LAST_NAMES = {
            "Sharma", "Patil", "Rao", "Mehta", "Gupta"
    };

    private TestDataGenerator() {
        // prevent instantiation
    }

    public static List<UserRecord> generateUsers(int count) {

        if (count <= 0) {
            throw new IllegalArgumentException("Count must be positive");
        }

        List<UserRecord> users = new ArrayList<>(count);

        for (int i = 1; i <= count; i++) {

            String firstName = FIRST_NAMES[i % FIRST_NAMES.length];
            String lastName = LAST_NAMES[i % LAST_NAMES.length];

            String email = firstName.toLowerCase() + "." +
                    lastName.toLowerCase() + i + "@example.com";

            int age = ThreadLocalRandom.current().nextInt(18, 60);

            UserRecord user = new UserRecord(
                    (long) i,
                    firstName,
                    lastName,
                    email,
                    age,
                    LocalDate.now().minusDays(i)
            );

            users.add(user);
        }

        return users;
    }
}