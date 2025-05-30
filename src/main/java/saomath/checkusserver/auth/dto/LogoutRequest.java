package saomath.checkusserver.auth.dto;

import lombok.Data;

@Data
public class LogoutRequest {
    private String refreshToken; // 선택사항 - 있으면 해당 토큰을 폐기
}
