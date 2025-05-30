package saomath.checkusserver.util;

import saomath.checkusserver.auth.dto.StudentRegisterRequest;
import saomath.checkusserver.auth.dto.GuardianRegisterRequest;
import saomath.checkusserver.auth.dto.TeacherRegisterRequest;
import saomath.checkusserver.entity.StudentProfile;

/**
 * 테스트용 데이터 생성 유틸리티 클래스
 */
public class TestDataFactory {
    
    private static int userCounter = 1;
    
    /**
     * 고유한 학생 회원가입 요청 데이터 생성
     */
    public static StudentRegisterRequest createStudentRegisterRequest() {
        return createStudentRegisterRequest("테스트 학생" + userCounter, StudentProfile.Gender.MALE);
    }
    
    /**
     * 커스텀 학생 회원가입 요청 데이터 생성
     */
    public static StudentRegisterRequest createStudentRegisterRequest(String name, StudentProfile.Gender gender) {
        StudentRegisterRequest request = new StudentRegisterRequest();
        request.setUsername("teststudent" + userCounter);
        request.setPassword("Test123!@#");
        request.setName(name);
        request.setPhoneNumber("010-" + String.format("%04d", userCounter) + "-" + String.format("%04d", userCounter));
        request.setSchoolName("테스트 고등학교" + userCounter);
        request.setGrade(11);
        request.setGender(gender);
        
        userCounter++;
        return request;
    }
    
    /**
     * 고유한 학부모 회원가입 요청 데이터 생성
     */
    public static GuardianRegisterRequest createGuardianRegisterRequest() {
        GuardianRegisterRequest request = new GuardianRegisterRequest();
        request.setUsername("testguardian" + userCounter);
        request.setPassword("Test123!@#");
        request.setName("테스트 학부모" + userCounter);
        request.setPhoneNumber("010-" + String.format("%04d", userCounter) + "-" + String.format("%04d", userCounter));
        
        userCounter++;
        return request;
    }
    
    /**
     * 고유한 교사 회원가입 요청 데이터 생성
     */
    public static TeacherRegisterRequest createTeacherRegisterRequest() {
        TeacherRegisterRequest request = new TeacherRegisterRequest();
        request.setUsername("testteacher" + userCounter);
        request.setPassword("Test123!@#");
        request.setName("테스트 교사" + userCounter);
        request.setPhoneNumber("010-" + String.format("%04d", userCounter) + "-" + String.format("%04d", userCounter));
        
        userCounter++;
        return request;
    }
    
    /**
     * 잘못된 비밀번호를 가진 학생 회원가입 요청 데이터 생성
     */
    public static StudentRegisterRequest createInvalidPasswordStudentRequest() {
        StudentRegisterRequest request = createStudentRegisterRequest();
        request.setPassword("weak"); // 약한 비밀번호
        return request;
    }
    
    /**
     * 잘못된 전화번호를 가진 학생 회원가입 요청 데이터 생성
     */
    public static StudentRegisterRequest createInvalidPhoneStudentRequest() {
        StudentRegisterRequest request = createStudentRegisterRequest();
        request.setPhoneNumber("01012345678"); // 하이픈 없는 형식
        return request;
    }
    
    /**
     * 테스트 카운터 리셋 (각 테스트 클래스 시작 전 호출)
     */
    public static void resetCounter() {
        userCounter = 1;
    }
}
