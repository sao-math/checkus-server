package saomath.checkusserver.classroom.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import saomath.checkusserver.classroom.domain.StudentClass;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentClassRepository extends JpaRepository<StudentClass, StudentClass.StudentClassId> {
    
    @Query("SELECT sc FROM StudentClass sc WHERE sc.student.id = :studentId")
    List<StudentClass> findByStudentId(@Param("studentId") Long studentId);
    
    @Query("SELECT sc FROM StudentClass sc WHERE sc.classEntity.id = :classId")
    List<StudentClass> findByClassId(@Param("classId") Long classId);
    
    @Query("SELECT sc FROM StudentClass sc WHERE sc.student.id = :studentId AND sc.classEntity.id = :classId")
    Optional<StudentClass> findByStudentIdAndClassId(@Param("studentId") Long studentId, @Param("classId") Long classId);
}
