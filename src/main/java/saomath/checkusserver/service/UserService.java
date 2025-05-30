package saomath.checkusserver.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import saomath.checkusserver.entity.User;
import saomath.checkusserver.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 모든 사용자 목록을 조회합니다.
     * @return 전체 사용자 목록
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * 사용자 ID로 특정 사용자를 조회합니다.
     * @param id 사용자 ID
     * @return 사용자 정보 (Optional)
     */
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
}