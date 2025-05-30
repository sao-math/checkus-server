package saomath.checkusserver.auth.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TeacherRegisterRequest extends BaseRegisterRequest {
    // 교사는 기본 정보만 필요
    // 추후 담당 반 배정은 관리자가 별도로 처리
}
