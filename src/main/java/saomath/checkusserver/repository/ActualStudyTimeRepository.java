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
    
    List<ActualStudyTime> findByAssignedStudyTimeId(Long assignedStudyTimeId);
    
    @Query("SELECT ast FROM ActualStudyTime ast WHERE ast.studentId = :studentId " +
           "AND ast.startTime >= :fromDate AND ast.startTime < :toDate " +
           "ORDER BY ast.startTime")
    List<ActualStudyTime> findByStudentIdAndDateRange(
            @Param("studentId") Long studentId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );
    
    List<ActualStudyTime> findByStudentIdAndAssignedStudyTimeIdIsNull(Long studentId);
    
    boolean existsByAssignedStudyTimeId(Long assignedStudyTimeId);
    
    // 진행 중인 공부 시간 조회 (종료 시간이 null인 것)
    List<ActualStudyTime> findByStudentIdAndEndTimeIsNullOrderByStartTimeDesc(Long studentId);
    
    // 할당된 시간 이전에 시작되어 아직 진행중이며 아직 할당되지 않은 세션들 조회
    List<ActualStudyTime> findByStudentIdAndStartTimeBeforeAndEndTimeIsNullAndAssignedStudyTimeIdIsNull(
            Long studentId, 
            LocalDateTime beforeTime
    );
    
    // 특정 시간 범위 내에 시작되어 아직 할당되지 않은 모든 세션들 조회 (진행중 + 종료된 세션 모두)
    @Query("SELECT ast FROM ActualStudyTime ast WHERE ast.studentId = :studentId " +
           "AND ast.startTime BETWEEN :startTime AND :endTime " +
           "AND ast.assignedStudyTimeId IS NULL " +
           "ORDER BY ast.startTime ASC")
    List<ActualStudyTime> findByStudentIdAndStartTimeBetweenAndAssignedStudyTimeIdIsNull(
            @Param("studentId") Long studentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
