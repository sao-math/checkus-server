package saomath.checkusserver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saomath.checkusserver.entity.Role;
import saomath.checkusserver.entity.User;
import saomath.checkusserver.entity.UserRole;
import saomath.checkusserver.exception.BusinessException;
import saomath.checkusserver.repository.RoleRepository;
import saomath.checkusserver.repository.UserRepository;
import saomath.checkusserver.repository.UserRoleRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRoleService {

    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    /**
     * 사용자에게 역할 할당
     */
    @Transactional
    public UserRole assignRole(User user, String roleName, UserRole.RoleStatus status) {
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new BusinessException("존재하지 않는 역할입니다: " + roleName));

        // 이미 할당된 역할인지 확인
        boolean exists = userRoleRepository.findByUserIdAndRoleName(user.getId(), roleName).isPresent();
        if (exists) {
            throw new BusinessException("이미 할당된 역할입니다: " + roleName);
        }

        UserRole userRole = new UserRole();
        UserRole.UserRoleId userRoleId = new UserRole.UserRoleId();
        userRoleId.setUserId(user.getId());
        userRoleId.setRoleId(role.getId());
        
        userRole.setId(userRoleId);
        userRole.setUser(user);
        userRole.setRole(role);
        userRole.setStatus(status);

        UserRole savedUserRole = userRoleRepository.save(userRole);
        log.info("사용자 {}에게 역할 {} 할당 완료 (상태: {})", user.getUsername(), roleName, status);
        
        return savedUserRole;
    }

    /**
     * 사용자의 활성화된 역할 목록 조회
     */
    @Transactional(readOnly = true)
    public List<String> getActiveRoles(Long userId) {
        return userRoleRepository.findByUserIdAndStatus(userId, UserRole.RoleStatus.ACTIVE)
                .stream()
                .map(userRole -> userRole.getRole().getName())
                .collect(Collectors.toList());
    }

    /**
     * 사용자의 모든 역할 조회 (상태 무관)
     */
    @Transactional(readOnly = true)
    public List<UserRole> getAllUserRoles(Long userId) {
        return userRoleRepository.findByUserId(userId);
    }

    /**
     * 역할 상태 변경
     */
    @Transactional
    public void updateRoleStatus(Long userId, String roleName, UserRole.RoleStatus newStatus) {
        UserRole userRole = userRoleRepository.findByUserIdAndRoleName(userId, roleName)
                .orElseThrow(() -> new BusinessException("사용자에게 할당되지 않은 역할입니다: " + roleName));

        userRole.setStatus(newStatus);
        userRoleRepository.save(userRole);
        
        log.info("사용자 ID {}의 역할 {} 상태를 {}로 변경", userId, roleName, newStatus);
    }

    /**
     * 역할 승인 (PENDING -> ACTIVE)
     */
    @Transactional
    public void approveRole(Long userId, String roleName) {
        updateRoleStatus(userId, roleName, UserRole.RoleStatus.ACTIVE);
    }

    /**
     * 역할 일시정지
     */
    @Transactional
    public void suspendRole(Long userId, String roleName) {
        updateRoleStatus(userId, roleName, UserRole.RoleStatus.SUSPENDED);
    }

    /**
     * 특정 역할의 승인 대기 중인 사용자 목록 조회
     */
    @Transactional(readOnly = true)
    public List<UserRole> getPendingRoleRequests(String roleName) {
        return userRoleRepository.findByRoleNameAndStatus(roleName, UserRole.RoleStatus.PENDING);
    }
    
    /**
     * 최적화된 DTO 직접 조회 - 특정 역할의 승인 대기 중인 사용자 목록
     */
    @Transactional(readOnly = true)
    public List<saomath.checkusserver.auth.dto.UserRoleResponse> getPendingRoleRequestsOptimized(String roleName) {
        return userRoleRepository.findUserRoleResponsesByRoleNameAndStatus(roleName, UserRole.RoleStatus.PENDING);
    }
    
    /**
     * 관리자가 사용자에게 역할을 직접 할당 (바로 ACTIVE 상태)
     */
    @Transactional
    public UserRole assignRoleDirectly(Long userId, String roleName) {
        // User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("존재하지 않는 사용자입니다: " + userId));
        
        // 직접 ACTIVE 상태로 역할 할당
        UserRole userRole = assignRole(user, roleName, UserRole.RoleStatus.ACTIVE);
        
        log.info("관리자가 사용자 {}에게 {} 역할을 직접 할당했습니다.", user.getUsername(), roleName);
        return userRole;
    }

    /**
     * 사용자가 특정 역할을 가지고 있는지 확인 (활성 상태만)
     */
    @Transactional(readOnly = true)
    public boolean hasActiveRole(Long userId, String roleName) {
        return userRoleRepository.findByUserIdAndRoleName(userId, roleName)
                .map(userRole -> userRole.getStatus() == UserRole.RoleStatus.ACTIVE)
                .orElse(false);
    }
}
