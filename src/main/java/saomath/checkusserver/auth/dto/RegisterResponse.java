package saomath.checkusserver.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegisterResponse {
    private Long userId;
    private String username;
    private String message;
}
