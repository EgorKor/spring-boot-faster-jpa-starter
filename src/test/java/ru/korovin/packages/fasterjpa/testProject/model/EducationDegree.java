package ru.korovin.packages.fasterjpa.testProject.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class EducationDegree {
    @Id
    private Integer code;
    private String name;
}
