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
    
    List<AssignedStudyTime> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);
    
    // 현재 시간에 진행 중인 공부 시간 조회 (출석 확인용)
    @Query("SELECT ast FROM AssignedStudyTime ast WHERE ast.studentId = :studentId " +
           "AND :currentTime BETWEEN ast.startTime AND ast.endTime")
    List<AssignedStudyTime> findCurrentStudyTimes(
            @Param("studentId") Long studentId,
            @Param("currentTime") LocalDateTime currentTime
    );
    
    // 특정 시간대에 시작하는 공부 시간 조회 (알림용)
    @Query("SELECT ast FROM AssignedStudyTime ast WHERE " +
           "ast.startTime BETWEEN :fromTime AND :toTime " +
           "ORDER BY ast.startTime")
    List<AssignedStudyTime> findStartingBetween(
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime
    );
    
    // 시작했으나 출석하지 않은 공부 시간 조회 (미접속 알림용)
    @Query("SELECT ast FROM AssignedStudyTime ast WHERE " +
           "ast.startTime BETWEEN :fromTime AND :toTime " +
           "AND NOT EXISTS (SELECT 1 FROM ActualStudyTime actual " +
           "WHERE actual.assignedStudyTimeId = ast.id)")
    List<AssignedStudyTime> findStartedWithoutAttendance(
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime
    );
    
    // 정확히 할당된 시간 범위 내에 접속한 경우 조회 (startTime <= 접속시간 <= endTime)
    @Query("SELECT ast FROM AssignedStudyTime ast WHERE ast.studentId = :studentId " +
           "AND :accessTime BETWEEN ast.startTime AND ast.endTime " +
           "ORDER BY ast.startTime")
    List<AssignedStudyTime> findByStudentIdAndTimeRange(
            @Param("studentId") Long studentId,
            @Param("accessTime") LocalDateTime accessTime
    );
}
