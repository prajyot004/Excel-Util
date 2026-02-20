package io.github.prajyotsable.excel;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UserRecord {

    private final Long id;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final Integer age;
    private final LocalDate createdDate;

}