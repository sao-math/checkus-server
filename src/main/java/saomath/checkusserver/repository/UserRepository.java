package saomath.checkusserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import saomath.checkusserver.entity.User;
import saomath.checkusserver.entity.StudentProfile;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByPhoneNumber(String phoneNumber);
    Optional<User> findByPhoneNumber(String phoneNumber);
    
    // 디스코드 ID로 사용자 조회
    Optional<User> findByDiscordId(String discordId);
    
    // 학생 필터링을 위한 복합 쿼리
    @Query("SELECT DISTINCT u FROM User u " +
           "JOIN UserRole ur ON u.id = ur.user.id " +
           "JOIN Role r ON ur.role.id = r.id " +
           "LEFT JOIN StudentProfile sp ON u.id = sp.user.id " +
           "LEFT JOIN StudentClass sc ON u.id = sc.student.id " +
           "WHERE r.name = 'STUDENT' " +
           "AND ur.status = 'ACTIVE' " +
           "AND (:classId IS NULL OR sc.classEntity.id = :classId) " +
           "AND (:grade IS NULL OR sp.grade = :grade) " +
           "AND (:status IS NULL OR sp.status = :status) " +
           "AND (:schoolId IS NULL OR sp.school.id = :schoolId)")
    List<User> findStudentsWithFilters(
            @Param("classId") Long classId,
            @Param("grade") Integer grade, 
            @Param("status") StudentProfile.StudentStatus status,
            @Param("schoolId") Long schoolId
    );
    
    // 모든 학생 조회 (역할 기반)
    @Query("SELECT DISTINCT u FROM User u " +
           "JOIN UserRole ur ON u.id = ur.user.id " +
           "JOIN Role r ON ur.role.id = r.id " +
           "WHERE r.name = 'STUDENT' AND ur.status = 'ACTIVE'")
    List<User> findAllStudents();
}