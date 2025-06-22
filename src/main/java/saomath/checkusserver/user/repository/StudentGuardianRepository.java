package saomath.checkusserver.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import saomath.checkusserver.user.domain.StudentGuardian;

import java.util.List;

@Repository
public interface StudentGuardianRepository extends JpaRepository<StudentGuardian, StudentGuardian.StudentGuardianId> {
    
    @Query("SELECT sg FROM StudentGuardian sg WHERE sg.student.id = :studentId")
    List<StudentGuardian> findByStudentId(@Param("studentId") Long studentId);
    
    @Query("SELECT sg FROM StudentGuardian sg WHERE sg.guardian.id = :guardianId")
    List<StudentGuardian> findByGuardianId(@Param("guardianId") Long guardianId);
    
    // 배치 조회: 여러 학생의 보호자 정보를 한 번에 조회
    @Query("SELECT sg FROM StudentGuardian sg " +
           "LEFT JOIN FETCH sg.student " +
           "LEFT JOIN FETCH sg.guardian " +
           "WHERE sg.student.id IN :studentIds")
    List<StudentGuardian> findByStudentIds(@Param("studentIds") List<Long> studentIds);
}
