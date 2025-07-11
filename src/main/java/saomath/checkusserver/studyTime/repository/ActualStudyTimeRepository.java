package saomath.checkusserver.studyTime.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import saomath.checkusserver.studyTime.domain.ActualStudyTime;

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
    

    
    // 특정 날짜 범위에서 할당되지 않은 실제 접속 기록 조회
    @Query("SELECT ast FROM ActualStudyTime ast WHERE ast.studentId = :studentId " +
           "AND ast.startTime >= :startDate AND ast.startTime <= :endDate " +
           "AND ast.assignedStudyTimeId IS NULL " +
           "ORDER BY ast.startTime")
    List<ActualStudyTime> findByStudentIdAndDateRangeAndAssignedStudyTimeIdIsNull(
            @Param("studentId") Long studentId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    
    // 배치 조회: 여러 학생의 실제 접속 기록을 한 번에 조회
    @Query("SELECT ast FROM ActualStudyTime ast WHERE ast.studentId IN :studentIds " +
           "AND ast.startTime >= :startDate AND ast.startTime <= :endDate " +
           "ORDER BY ast.studentId, ast.startTime")
    List<ActualStudyTime> findByStudentIdsAndDateRange(
            @Param("studentIds") List<Long> studentIds,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    
    // 배치 조회: 여러 학생의 할당되지 않은 실제 접속 기록을 한 번에 조회
    @Query("SELECT ast FROM ActualStudyTime ast WHERE ast.studentId IN :studentIds " +
           "AND ast.startTime >= :startDate AND ast.startTime <= :endDate " +
           "AND ast.assignedStudyTimeId IS NULL " +
           "ORDER BY ast.studentId, ast.startTime")
    List<ActualStudyTime> findByStudentIdsAndDateRangeAndAssignedStudyTimeIdIsNull(
            @Param("studentIds") List<Long> studentIds,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    
    // 배치 조회: 여러 할당 ID에 연결된 실제 접속 기록을 한 번에 조회
    @Query("SELECT ast FROM ActualStudyTime ast WHERE ast.assignedStudyTimeId IN :assignedStudyTimeIds " +
           "ORDER BY ast.assignedStudyTimeId, ast.startTime")
    List<ActualStudyTime> findByAssignedStudyTimeIds(@Param("assignedStudyTimeIds") List<Long> assignedStudyTimeIds);
}
