package saomath.checkusserver.school.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchoolResponse {
    private Long id;
    private String name;
    private Long studentCount;
} 