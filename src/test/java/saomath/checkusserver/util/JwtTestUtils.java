package saomath.checkusserver.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import saomath.checkusserver.auth.jwt.JwtTokenProvider;

import java.util.List;

/**
 * 테스트용 JWT 토큰 생성 유틸리티
 */
@Component
public class JwtTestUtils {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    /**
     * 학생용 테스트 JWT 토큰 생성
     */
    public String generateStudentToken(Long userId, String username) {
        return jwtTokenProvider.generateAccessToken(
                userId, 
                username, 
                List.of("STUDENT")
        );
    }

    /**
     * 선생님용 테스트 JWT 토큰 생성
     */
    public String generateTeacherToken(Long userId, String username) {
        return jwtTokenProvider.generateAccessToken(
                userId, 
                username, 
                List.of("TEACHER")
        );
    }

    /**
     * 관리자용 테스트 JWT 토큰 생성
     */
    public String generateAdminToken(Long userId, String username) {
        return jwtTokenProvider.generateAccessToken(
                userId, 
                username, 
                List.of("ADMIN")
        );
    }

    /**
     * 사용자 정의 권한을 가진 테스트 JWT 토큰 생성
     */
    public String generateCustomToken(Long userId, String username, List<String> roles) {
        return jwtTokenProvider.generateAccessToken(userId, username, roles);
    }

    /**
     * Authorization 헤더용 Bearer 토큰 형식으로 변환
     */
    public String toBearerToken(String token) {
        return "Bearer " + token;
    }
}
