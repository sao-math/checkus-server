package saomath.checkusserver.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {
    private Long id;
    private String username;
    private String name;
    private String phoneNumber;
    private String discordId;
    private List<String> roles;
    private LocalDateTime createdAt;
}
