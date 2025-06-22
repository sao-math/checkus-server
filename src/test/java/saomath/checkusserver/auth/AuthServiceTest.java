package saomath.checkusserver.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import saomath.checkusserver.auth.domain.User;
import saomath.checkusserver.auth.domain.UserRole;
import saomath.checkusserver.auth.dto.StudentRegisterRequest;
import saomath.checkusserver.auth.dto.RegisterResponse;
import saomath.checkusserver.auth.jwt.JwtTokenProvider;
import saomath.checkusserver.auth.repository.UserRepository;
import saomath.checkusserver.auth.service.AuthService;
import saomath.checkusserver.auth.service.RefreshTokenService;
import saomath.checkusserver.entity.*;
import saomath.checkusserver.common.exception.DuplicateResourceException;
import saomath.checkusserver.repository.*;
import saomath.checkusserver.service.UserRoleService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 테스트")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private StudentProfileRepository studentProfileRepository;
    
    @Mock
    private SchoolRepository schoolRepository;
    
    @Mock
    private UserRoleService userRoleService;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    
    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("학생 회원가입 성공")
    void registerStudent_Success() {
        // Given
        StudentRegisterRequest request = new StudentRegisterRequest();
        request.setUsername("testuser");
        request.setPassword("Test123!@#");
        request.setName("테스트 사용자");
        request.setPhoneNumber("010-1234-5678");
        request.setSchoolName("테스트 학교");
        request.setGrade(10);
        request.setGender(StudentProfile.Gender.MALE);

        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");

        School mockSchool = new School();
        mockSchool.setId(1L);
        mockSchool.setName("테스트 학교");

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("010-1234-5678")).thenReturn(false);
        when(passwordEncoder.encode("Test123!@#")).thenReturn("encoded_password");
        when(schoolRepository.findByName("테스트 학교")).thenReturn(Optional.of(mockSchool));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(studentProfileRepository.save(any(StudentProfile.class))).thenReturn(new StudentProfile());
        when(userRoleService.assignRole(any(User.class), eq(RoleConstants.STUDENT), eq(UserRole.RoleStatus.PENDING)))
                .thenReturn(new UserRole());

        // When
        RegisterResponse response = authService.registerStudent(request);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getUserId());
        assertEquals("testuser", response.getUsername());
        assertTrue(response.getMessage().contains("학생 회원가입이 완료되었습니다"));

        verify(userRepository).existsByUsername("testuser");
        verify(userRepository).existsByPhoneNumber("010-1234-5678");
        verify(userRepository).save(any(User.class));
        verify(studentProfileRepository).save(any(StudentProfile.class));
        verify(userRoleService).assignRole(any(User.class), eq(RoleConstants.STUDENT), eq(UserRole.RoleStatus.PENDING));
    }

    @Test
    @DisplayName("학생 회원가입 실패 - 사용자명 중복")
    void registerStudent_Fail_DuplicateUsername() {
        // Given
        StudentRegisterRequest request = new StudentRegisterRequest();
        request.setUsername("testuser");
        request.setPassword("Test123!@#");
        request.setName("테스트 사용자");
        request.setPhoneNumber("010-1234-5678");
        request.setSchoolName("테스트 학교");
        request.setGrade(10);
        request.setGender(StudentProfile.Gender.MALE);

        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        // When & Then
        assertThrows(DuplicateResourceException.class, () -> {
            authService.registerStudent(request);
        });

        verify(userRepository).existsByUsername("testuser");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("사용자명 중복 확인")
    void isUsernameExists_Test() {
        // Given
        when(userRepository.existsByUsername("testuser")).thenReturn(true);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);

        // When & Then
        assertTrue(authService.isUsernameExists("testuser"));
        assertFalse(authService.isUsernameExists("newuser"));
    }

    @Test
    @DisplayName("전화번호 중복 확인")
    void isPhoneNumberExists_Test() {
        // Given
        when(userRepository.existsByPhoneNumber("010-1234-5678")).thenReturn(true);
        when(userRepository.existsByPhoneNumber("010-9999-9999")).thenReturn(false);

        // When & Then
        assertTrue(authService.isPhoneNumberExists("010-1234-5678"));
        assertFalse(authService.isPhoneNumberExists("010-9999-9999"));
    }
}
