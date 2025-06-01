package saomath.checkusserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import saomath.checkusserver.entity.AssignedStudyTime;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AssignedStudyTimeRepository extends JpaRepository<AssignedStudyTime, Long> {
    
    List<AssignedStudyTime> findByStudentIdAndStartTimeBetween(
            Long studentId, 
            LocalDateTime startDate, 
            LocalDateTime endDate
    );
    
    @Query("SELECT ast FROM AssignedStudyTime ast WHERE ast.studentId = :studentId " +
           "AND ast.startTime <= :endTime AND ast.endTime >= :startTime")
    List<AssignedStudyTime> findOverlappingStudyTimes(
            @Param("studentId") Long studentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
    
    @Query("SELECT ast FROM AssignedStudyTime ast WHERE " +
           "ast.startTime BETWEEN :tenMinutesBefore AND :tenMinutesAfter " +
           "ORDER BY ast.startTime")
    List<AssignedStudyTime> findUpcomingStudyTimesV2(
            @Param("tenMinutesBefore") LocalDateTime tenMinutesBefore,
            @Param("now") LocalDateTime now,
            @Param("tenMinutesAfter") LocalDateTime tenMinutesAfter
    );
}
