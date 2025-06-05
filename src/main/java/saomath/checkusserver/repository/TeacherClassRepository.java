package saomath.checkusserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import saomath.checkusserver.entity.TeacherClass;

import java.util.List;

@Repository
public interface TeacherClassRepository extends JpaRepository<TeacherClass, TeacherClass.TeacherClassId> {
    
    @Query("SELECT tc FROM TeacherClass tc WHERE tc.teacher.id = :teacherId")
    List<TeacherClass> findByTeacherId(@Param("teacherId") Long teacherId);
    
    @Query("SELECT tc FROM TeacherClass tc WHERE tc.classEntity.id = :classId")
    List<TeacherClass> findByClassId(@Param("classId") Long classId);
}
