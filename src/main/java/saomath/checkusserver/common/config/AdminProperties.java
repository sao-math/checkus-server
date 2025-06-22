package saomath.checkusserver.common.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;

/**
 * 초기 관리자 계정 설정을 위한 Properties 클래스
 * 환경변수를 통해 안전하게 관리자 정보를 주입받습니다.
 */
@Data
@ConfigurationProperties(prefix = "app.admin")
public class AdminProperties {

    private String username;
    private char[] password;  // 보안을 위해 char[] 사용
    private String name;

    //TODO 비밀번호 유효성 검사 클래스 만들어서 재사용

    /**
     * 비밀번호 유효성 검증
     */
    @PostConstruct
    public void validateProperties() {
        if (password != null && password.length < 8) {
            throw new IllegalArgumentException("관리자 비밀번호는 최소 8자 이상이어야 합니다.");
        }
    }

    /**
     * 비밀번호를 String으로 반환 (임시 사용용)
     */
    public String getPasswordAsString() {
        return password != null ? new String(password) : null;
    }

    /**
     * 사용 후 비밀번호를 메모리에서 안전하게 제거
     */
    public void clearPassword() {
        if (password != null) {
            Arrays.fill(password, '\0');  // 메모리 덮어쓰기
            password = null;
        }
    }

    /**
     * toString에서 비밀번호 숨김 처리
     */
    @Override
    public String toString() {
        return "AdminProperties{" +
                "username='" + username + '\'' +
                ", password='***'" +
                ", name='" + name + '\'' +
                '}';
    }
}
