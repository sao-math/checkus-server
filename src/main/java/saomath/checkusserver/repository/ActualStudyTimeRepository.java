package saomath.checkusserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import saomath.checkusserver.entity.ActualStudyTime;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActualStudyTimeRepository extends JpaRepository<ActualStudyTime, Long> {
    
    List<ActualStudyTime> findByStudentId(Long studentId);
    
    List<ActualStudyTime> findByStudentIdAndStartTimeBetween(
            Long studentId, 
            LocalDateTime startDate, 
            LocalDateTime endDate
    );
    
    List<ActualStudyTime> findByAssignedStudyTimeId(Long assignedStudyTimeId);
    
    List<ActualStudyTime> findBySource(String source);
    
    @Query("SELECT ast FROM ActualStudyTime ast WHERE ast.studentId = :studentId " +
           "AND ast.startTime >= :fromDate AND ast.startTime < :toDate " +
           "ORDER BY ast.startTime")
    List<ActualStudyTime> findByStudentIdAndDateRange(
            @Param("studentId") Long studentId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );
    
    @Query("SELECT SUM(TIMESTAMPDIFF(MINUTE, ast.startTime, ast.endTime)) " +
           "FROM ActualStudyTime ast WHERE ast.studentId = :studentId " +
           "AND ast.startTime >= :fromDate AND ast.startTime < :toDate")
    Long calculateTotalStudyMinutes(
            @Param("studentId") Long studentId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );
}
