package saomath.checkusserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GuardianResponse {
    private Long id;
    private String name;
    private String phoneNumber;
    private String relationship; // 부/모/기타
}
