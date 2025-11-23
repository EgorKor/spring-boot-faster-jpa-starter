package ru.korovin.packages.fasterjpa.tests.utils.dto;

import lombok.Data;

import java.util.List;

@Data
public class DepartmentDto {
    private Long id;
    private String name;
    private List<EmployeeDto> employees;
}
