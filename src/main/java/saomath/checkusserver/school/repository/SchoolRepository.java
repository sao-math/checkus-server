package saomath.checkusserver.school.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;  // ✅ 이 import 추가
import org.springframework.stereotype.Repository;
import saomath.checkusserver.school.domain.School;
import saomath.checkusserver.user.domain.StudentProfile;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchoolRepository extends JpaRepository<School, Long> {
    Optional<School> findByName(String name);
    boolean existsByName(String name);

    @Query("SELECT s.id, s.name, COUNT(sp.userId) " +
            "FROM School s LEFT JOIN StudentProfile sp ON s.id = sp.school.id " +
            "GROUP BY s.id, s.name " +
            "ORDER BY s.name")
    List<Object[]> findAllSchoolsWithStudentCount();

    @Query("SELECT COUNT(sp.userId) " +
            "FROM StudentProfile sp " +
            "WHERE sp.school.id = :schoolId")
    Long countStudentsBySchoolId(@Param("schoolId") Long schoolId);  // ✅ @Param 추가
}