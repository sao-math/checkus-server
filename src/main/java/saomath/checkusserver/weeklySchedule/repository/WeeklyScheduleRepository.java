package saomath.checkusserver.weeklySchedule.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import saomath.checkusserver.weeklySchedule.domain.WeeklySchedule;

import java.util.List;

@Repository
public interface WeeklyScheduleRepository extends JpaRepository<WeeklySchedule, Long> {

    /**
     * 학생별 주간 시간표 조회
     */
    @Query("SELECT ws FROM WeeklySchedule ws " +
           "LEFT JOIN FETCH ws.student " +
           "LEFT JOIN FETCH ws.activity " +
           "WHERE ws.studentId = :studentId " +
           "ORDER BY ws.dayOfWeek, ws.startTime")
    List<WeeklySchedule> findByStudentIdWithDetails(@Param("studentId") Long studentId);

    /**
     * 학생의 특정 요일 시간표 조회
     */
    @Query("SELECT ws FROM WeeklySchedule ws " +
           "LEFT JOIN FETCH ws.activity " +
           "WHERE ws.studentId = :studentId AND ws.dayOfWeek = :dayOfWeek " +
           "ORDER BY ws.startTime")
    List<WeeklySchedule> findByStudentIdAndDayOfWeek(@Param("studentId") Long studentId, 
                                                     @Param("dayOfWeek") Integer dayOfWeek);

    /**
     * 학생의 시간 겹침 확인
     */
    @Query("SELECT COUNT(ws) FROM WeeklySchedule ws " +
           "WHERE ws.studentId = :studentId " +
           "AND ws.dayOfWeek = :dayOfWeek " +
           "AND ws.id != :excludeId " +
           "AND ((ws.startTime <= :startTime AND ws.endTime > :startTime) " +
           "OR (ws.startTime < :endTime AND ws.endTime >= :endTime) " +
           "OR (ws.startTime >= :startTime AND ws.endTime <= :endTime))")
    long countOverlappingSchedules(@Param("studentId") Long studentId,
                                   @Param("dayOfWeek") Integer dayOfWeek,
                                   @Param("startTime") java.time.LocalTime startTime,
                                   @Param("endTime") java.time.LocalTime endTime,
                                   @Param("excludeId") Long excludeId);

    /**
     * 학생별 시간표 전체 삭제
     */
    void deleteByStudentId(Long studentId);

    /**
     * 활동별 시간표 조회 (활동 삭제 시 참조 확인용)
     */
    List<WeeklySchedule> findByActivityId(Long activityId);
}
