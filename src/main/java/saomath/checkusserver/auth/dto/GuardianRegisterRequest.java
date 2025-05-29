package saomath.checkusserver.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "학부모 회원가입 요청 DTO")
public class GuardianRegisterRequest extends BaseRegisterRequest {
    // 학부모는 기본 정보만 필요
    // 추후 자녀 연동은 별도 기능으로 구현
}
