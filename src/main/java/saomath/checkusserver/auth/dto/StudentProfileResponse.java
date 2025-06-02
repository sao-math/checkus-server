package saomath.checkusserver.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import saomath.checkusserver.dto.common.StudentProfileInfo;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "학생 프로필 정보")
public class StudentProfileResponse extends StudentProfileInfo {
    // 기본 학생 프로필 정보는 부모 클래스에서 상속
    // 필요하면 추가 인증 전용 필드를 여기에 추가 가능
}