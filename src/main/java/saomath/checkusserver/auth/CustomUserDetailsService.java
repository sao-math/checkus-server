package saomath.checkusserver.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saomath.checkusserver.entity.User;
import saomath.checkusserver.entity.UserRole;
import saomath.checkusserver.repository.UserRepository;
import saomath.checkusserver.repository.UserRoleRepository;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));

        // 활성화된 역할만 가져오기
        List<UserRole> userRoles = userRoleRepository.findByUserIdAndStatus(
                user.getId(), UserRole.RoleStatus.ACTIVE);

        Collection<? extends GrantedAuthority> authorities = userRoles.stream()
                .map(userRole -> new SimpleGrantedAuthority("ROLE_" + userRole.getRole().getName()))
                .collect(Collectors.toList());

        return new CustomUserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getName(),
                authorities,
                true, // enabled
                true, // accountNonExpired
                true, // credentialsNonExpired
                true  // accountNonLocked
        );
    }

    /**
     * 사용자 ID로 UserDetails 로드
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + id));

        List<UserRole> userRoles = userRoleRepository.findByUserIdAndStatus(
                user.getId(), UserRole.RoleStatus.ACTIVE);

        Collection<? extends GrantedAuthority> authorities = userRoles.stream()
                .map(userRole -> new SimpleGrantedAuthority("ROLE_" + userRole.getRole().getName()))
                .collect(Collectors.toList());

        return new CustomUserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getName(),
                authorities,
                true, true, true, true
        );
    }
}
