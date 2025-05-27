package saomath.checkusserver.auth.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class GuardianRegisterRequest extends BaseRegisterRequest {
    // 학부모는 기본 정보만 필요
    // 추후 자녀 연동은 별도 기능으로 구현
}
