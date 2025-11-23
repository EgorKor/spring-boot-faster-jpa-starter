package ru.korovin.packages.fasterjpa.tests.utils.dto;

import lombok.Data;

@Data
public class EmployeeDto {
    private Long id;
    private String name;
    private Long departmentId;
}
