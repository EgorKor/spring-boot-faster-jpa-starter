package ru.korovin.packages.fasterjpa.testProject.dto;

import ru.korovin.packages.fasterjpa.testProject.model.DirectionProfileChoice;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
public class EducationProgramDto {
    private Long id;
    private String name;
    private List<DirectionProfileChoice> profiles;
}
