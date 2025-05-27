package saomath.checkusserver.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class LoginResponse {
    private Long userId;
    private String username;
    private String name;
    private List<String> roles;
    private String accessToken;
    private String refreshToken;
    private final String tokenType = "Bearer";
}
