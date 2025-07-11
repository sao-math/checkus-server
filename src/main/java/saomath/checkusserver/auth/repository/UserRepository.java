package saomath.checkusserver.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import saomath.checkusserver.auth.domain.User;
import saomath.checkusserver.auth.domain.UserRole;
import saomath.checkusserver.user.domain.StudentProfile;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // 논리삭제된 사용자 제외 조회
    @Query("SELECT u FROM User u WHERE u.username = :username AND u.deletedAt IS NULL")
    Optional<User> findByUsername(@Param("username") String username);
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.username = :username AND u.deletedAt IS NULL")
    boolean existsByUsername(@Param("username") String username);
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.phoneNumber = :phoneNumber AND u.deletedAt IS NULL")
    boolean existsByPhoneNumber(@Param("phoneNumber") String phoneNumber);
    @Query("SELECT u FROM User u WHERE u.phoneNumber = :phoneNumber AND u.deletedAt IS NULL")
    Optional<User> findByPhoneNumber(@Param("phoneNumber") String phoneNumber);
    
    // 디스코드 ID로 사용자 조회 (논리삭제된 사용자 제외)
    @Query("SELECT u FROM User u WHERE u.discordId = :discordId AND u.deletedAt IS NULL")
    Optional<User> findByDiscordId(@Param("discordId") String discordId);
    
    // 학생 필터링을 위한 복합 쿼리 (논리삭제된 사용자 제외)
    @Query("SELECT DISTINCT u FROM User u " +
           "JOIN UserRole ur ON u.id = ur.user.id " +
           "JOIN Role r ON ur.role.id = r.id " +
           "LEFT JOIN StudentProfile sp ON u.id = sp.user.id " +
           "LEFT JOIN StudentClass sc ON u.id = sc.student.id " +
           "WHERE r.name = 'STUDENT' " +
           "AND ur.status = 'ACTIVE' " +
           "AND u.deletedAt IS NULL " +
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
    
    // 모든 학생 조회 (역할 기반, 논리삭제된 사용자 제외)
    @Query("SELECT DISTINCT u FROM User u " +
           "JOIN UserRole ur ON u.id = ur.user.id " +
           "JOIN Role r ON ur.role.id = r.id " +
           "WHERE r.name = 'STUDENT' AND ur.status = 'ACTIVE' AND u.deletedAt IS NULL")
    List<User> findAllStudents();

    // 모든 재원 중인 학생 조회 (스터디 모니터링용, 논리삭제된 사용자 제외)
    @Query("SELECT DISTINCT u FROM User u " +
           "JOIN UserRole ur ON u.id = ur.user.id " +
           "JOIN Role r ON ur.role.id = r.id " +
           "LEFT JOIN StudentProfile sp ON u.id = sp.user.id " +
           "WHERE r.name = 'STUDENT' AND ur.status = 'ACTIVE' " +
           "AND sp.status = 'ENROLLED' AND u.deletedAt IS NULL")
    List<User> findAllEnrolledStudents();

    // 교사 상태별 조회 (논리삭제된 사용자 제외)
    @Query("SELECT DISTINCT u FROM User u " +
           "JOIN UserRole ur ON u.id = ur.user.id " +
           "JOIN Role r ON ur.role.id = r.id " +
           "WHERE r.name = 'TEACHER' AND ur.status = :status AND u.deletedAt IS NULL " +
           "ORDER BY u.name ASC")
    List<User> findTeachersByStatus(@Param("status") UserRole.RoleStatus status);

    // 모든 활성화된 교사 조회 (논리삭제된 사용자 제외)
    @Query("SELECT DISTINCT u FROM User u " +
           "JOIN UserRole ur ON u.id = ur.user.id " +
           "JOIN Role r ON ur.role.id = r.id " +
           "WHERE r.name = 'TEACHER' AND ur.status = 'ACTIVE' AND u.deletedAt IS NULL " +
           "ORDER BY u.name ASC")
    List<User> findAllActiveTeachers();

    // 논리삭제된 사용자 제외하고 ID로 조회
    @Query("SELECT u FROM User u WHERE u.id = :id AND u.deletedAt IS NULL")
    Optional<User> findByIdAndNotDeleted(@Param("id") Long id);
}