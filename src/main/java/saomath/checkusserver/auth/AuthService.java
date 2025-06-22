package saomath.checkusserver.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saomath.checkusserver.auth.dto.*;
import saomath.checkusserver.auth.jwt.JwtTokenProvider;
import saomath.checkusserver.entity.*;
import saomath.checkusserver.common.exception.AuthenticationException;
import saomath.checkusserver.common.exception.BusinessException;
import saomath.checkusserver.common.exception.DuplicateResourceException;
import saomath.checkusserver.repository.*;
import saomath.checkusserver.service.UserRoleService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final SchoolRepository schoolRepository;
    private final UserRoleService userRoleService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    private void checkDuplicateUsernameAndPhoneNumber(String username, String phoneNumber) {
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("이미 사용 중인 사용자명입니다: " + username);
        }
        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new DuplicateResourceException("이미 등록된 전화번호입니다: " + phoneNumber);
        }

    }

    /**
     * 학생 회원가입
     */
    @Transactional
    public RegisterResponse registerStudent(StudentRegisterRequest request) {
        validateRegisterRequest(request);

        checkDuplicateUsernameAndPhoneNumber(request.getUsername(), request.getPhoneNumber());

        // 학교 조회 또는 생성
        School school = getOrCreateSchool(request.getSchoolName());

        // 사용자 생성
        User user = createUser(request);
        
        // 학생 프로필 생성 (사용자 저장 전에 생성)
        StudentProfile studentProfile = new StudentProfile();
        studentProfile.setUser(user);
        studentProfile.setStatus(StudentProfile.StudentStatus.UNREGISTERED);
        studentProfile.setSchool(school);
        studentProfile.setGrade(request.getGrade());
        studentProfile.setGender(request.getGender());
        
        // 사용자 저장 (이때 ID가 생성됨)
        User savedUser = userRepository.save(user);
        
        // StudentProfile에 userId 설정 후 저장
        studentProfile.setUser(savedUser); //TODO id만 불러오게 리팩토링
        studentProfileRepository.save(studentProfile);

        // 학생 역할 할당 (승인 필요)
        userRoleService.assignRole(savedUser, RoleConstants.STUDENT, UserRole.RoleStatus.PENDING);

        log.info("학생 회원가입 완료: {}", savedUser.getUsername());
        
        return new RegisterResponse(savedUser.getId(), savedUser.getUsername(), 
                                  "학생 회원가입이 완료되었습니다. 관리자 승인을 기다려주세요.");
    }

    /**
     * 학부모 회원가입
     */
    @Transactional
    public RegisterResponse registerGuardian(GuardianRegisterRequest request) {
        validateRegisterRequest(request);
        checkDuplicateUsernameAndPhoneNumber(request.getUsername(), request.getPhoneNumber());

        // 사용자 생성
        User user = createUser(request);
        User savedUser = userRepository.save(user);

        // 학부모 역할 할당 (승인 필요)
        userRoleService.assignRole(savedUser, RoleConstants.GUARDIAN, UserRole.RoleStatus.PENDING);

        log.info("학부모 회원가입 완료: {}", savedUser.getUsername());
        
        return new RegisterResponse(savedUser.getId(), savedUser.getUsername(), 
                                  "학부모 회원가입이 완료되었습니다. 관리자 승인을 기다려주세요.");
    }

    /**
     * 교사 회원가입
     */
    @Transactional
    public RegisterResponse registerTeacher(TeacherRegisterRequest request) {
        validateRegisterRequest(request);

        checkDuplicateUsernameAndPhoneNumber(request.getUsername(), request.getPhoneNumber());

        // 사용자 생성
        User user = createUser(request);
        User savedUser = userRepository.save(user);

        // 교사 역할 할당 (승인 필요)
        userRoleService.assignRole(savedUser, RoleConstants.TEACHER, UserRole.RoleStatus.PENDING);

        log.info("교사 회원가입 완료: {}", savedUser.getUsername());
        
        return new RegisterResponse(savedUser.getId(), savedUser.getUsername(), 
                                  "교사 회원가입이 완료되었습니다. 관리자 승인을 기다려주세요.");
    }

    /**
     * 로그인
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        try {
            // 사용자 인증
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();

            // 활성화된 역할 조회
            List<String> roles = userRoleService.getActiveRoles(userPrincipal.getId());
            
            if (roles.isEmpty()) {
                throw new AuthenticationException("승인되지 않은 계정입니다. 관리자 승인을 기다려주세요.");
            }

            // JWT 토큰 생성
            String accessToken = jwtTokenProvider.generateAccessToken(userPrincipal.getId(), userPrincipal.getUsername(), roles);
            String refreshToken = jwtTokenProvider.generateRefreshToken(userPrincipal.getUsername());

            // 리프레시 토큰 저장
            refreshTokenService.saveRefreshToken(refreshToken, userPrincipal.getId());

            log.info("로그인 성공: {} (역할: {})", userPrincipal.getUsername(), roles);

            return new LoginResponse(
                userPrincipal.getId(),
                userPrincipal.getUsername(),
                userPrincipal.getName(),
                roles,
                accessToken,
                refreshToken
            );

        } catch (AuthenticationException e) {
            // 이미 AuthenticationException인 경우 그대로 던지기
            log.error("로그인 실패: {}", request.getUsername(), e);
            throw e;
        } catch (org.springframework.security.core.AuthenticationException e) {
            // Spring Security 인증 실패 (비밀번호 오류 등)
            log.error("로그인 실패: {}", request.getUsername(), e);
            throw new AuthenticationException("로그인에 실패했습니다. 사용자명과 비밀번호를 확인해주세요.");
        } catch (Exception e) {
            // 기타 예외
            log.error("로그인 실패: {}", request.getUsername(), e);
            throw new AuthenticationException("로그인 처리 중 오류가 발생했습니다.");
        }
    }

    /**
     * 토큰 리프레시
     */
    @Transactional
    public TokenRefreshResponse refreshToken(TokenRefreshRequest request) {
        // 리프레시 토큰 검증
        RefreshToken refreshToken = refreshTokenService.validateRefreshToken(request.getRefreshToken());
        
        // 사용자 조회
        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다."));

        // 활성화된 역할 조회
        List<String> roles = userRoleService.getActiveRoles(user.getId());
        
        if (roles.isEmpty()) {
            throw new AuthenticationException("승인되지 않은 계정입니다.");
        }

        // 새 액세스 토큰 생성
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), roles);

        log.info("토큰 리프레시 성공: {}", user.getUsername());

        return new TokenRefreshResponse(newAccessToken, request.getRefreshToken());
    }

    /**
     * 로그아웃
     */
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null) {
            refreshTokenService.revokeRefreshToken(refreshToken);
        }
        SecurityContextHolder.clearContext();
        log.info("로그아웃 완료");
    }

    /**
     * 사용자명 중복 확인
     */
    @Transactional(readOnly = true)
    public boolean isUsernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * 전화번호 중복 확인
     */
    @Transactional(readOnly = true)
    public boolean isPhoneNumberExists(String phoneNumber) {
        return userRepository.existsByPhoneNumber(phoneNumber);
    }

    // === Private Helper Methods ===

    private void validateRegisterRequest(BaseRegisterRequest request) {
        if (!ValidationUtils.isValidUsername(request.getUsername())) {
            throw new BusinessException(ValidationUtils.getUsernameRequirements());
        }
        if (!ValidationUtils.isValidPassword(request.getPassword())) {
            throw new BusinessException(ValidationUtils.getPasswordRequirements());
        }
        if (!ValidationUtils.isValidPhoneNumber(request.getPhoneNumber())) {
            throw new BusinessException(ValidationUtils.getPhoneNumberRequirements());
        }
    }

    private User createUser(BaseRegisterRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setName(request.getName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setDiscordId(request.getDiscordId());
        return user;
    }

    private School getOrCreateSchool(String schoolName) {
        return schoolRepository.findByName(schoolName)
                .orElseGet(() -> {
                    School newSchool = new School();
                    newSchool.setName(schoolName);
                    return schoolRepository.save(newSchool);
                });
    }
}
