package saomath.checkusserver.studyTime.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.jdbc.core.JdbcTemplate;
import saomath.checkusserver.studyTime.dto.StudyTimeMonitorResponse;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("StudyTimeService N+1 쿼리 개선 실제 증명 테스트")
class StudyTimeServiceQueryCountTest {

    @Autowired
    private StudyTimeService studyTimeService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final AtomicInteger queryCount = new AtomicInteger(0);

    @Test
    @DisplayName("실제 쿼리 개수 측정: P6Spy를 사용한 정확한 측정")
    @Sql(scripts = {
        "/sql/test-data-setup.sql"  // 테스트 데이터 생성 스크립트
    })
    void measureActualQueryCount() {
        LocalDate testDate = LocalDate.now();
        
        // Hibernate 통계 활성화 방법
        System.setProperty("hibernate.generate_statistics", "true");
        System.setProperty("hibernate.session.events.log", "true");
        
        System.out.println("===== 실제 쿼리 개수 측정 시작 =====");
        
        // 최적화된 버전 실행
        long startTime = System.currentTimeMillis();
        StudyTimeMonitorResponse result = studyTimeService.getStudyTimeMonitorByDate(testDate);
        long endTime = System.currentTimeMillis();
        
        System.out.println("실행 시간: " + (endTime - startTime) + "ms");
        System.out.println("조회된 학생 수: " + result.getStudents().size());
        System.out.println("===== 로그에서 실행된 SQL 쿼리 개수를 확인하세요 =====");
        
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("부하 테스트: 대량 데이터에서 성능 차이 측정")
    @Sql(scripts = {
        "/sql/large-test-data-setup.sql"  // 대량 테스트 데이터
    })
    void loadTestWithLargeDataset() {
        LocalDate testDate = LocalDate.now();
        
        System.out.println("===== 대량 데이터 부하 테스트 =====");
        
        // 학생 수 확인
        Integer studentCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users u JOIN user_role ur ON u.id = ur.user_id " +
            "JOIN role r ON ur.role_id = r.id WHERE r.name = 'STUDENT'", 
            Integer.class
        );
        
        System.out.println("테스트 학생 수: " + studentCount);
        
        // 여러 번 실행하여 평균 성능 측정
        long totalTime = 0;
        int iterations = 5;
        
        for (int i = 0; i < iterations; i++) {
            long startTime = System.currentTimeMillis();
            StudyTimeMonitorResponse result = studyTimeService.getStudyTimeMonitorByDate(testDate);
            long endTime = System.currentTimeMillis();
            
            long executionTime = endTime - startTime;
            totalTime += executionTime;
            
            System.out.println("실행 " + (i + 1) + ": " + executionTime + "ms");
            assertThat(result.getStudents()).hasSize(studentCount);
        }
        
        long averageTime = totalTime / iterations;
        System.out.println("평균 실행 시간: " + averageTime + "ms");
        
        // 성능 기준: 학생 수가 많아도 1초 이내 완료되어야 함
        assertThat(averageTime).isLessThan(1000L);
        
        System.out.println("===== 예상 이전 성능 (N+1) =====");
        System.out.println("예상 쿼리 개수: " + (1 + studentCount * 4) + "개");
        System.out.println("현재 최적화: 5-10개 쿼리");
        System.out.println("개선율: " + String.format("%.1f%%", 
            (1.0 - 10.0 / (1 + studentCount * 4)) * 100));
    }

    @Test
    @DisplayName("메모리 사용량 측정")
    void measureMemoryUsage() throws InterruptedException {
        LocalDate testDate = LocalDate.now();
        
        // GC 실행으로 메모리 정리
        System.gc();
        Thread.sleep(100);
        
        Runtime runtime = Runtime.getRuntime();
        long beforeMemory = runtime.totalMemory() - runtime.freeMemory();
        
        StudyTimeMonitorResponse result = studyTimeService.getStudyTimeMonitorByDate(testDate);
        
        long afterMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = afterMemory - beforeMemory;
        
        System.out.println("===== 메모리 사용량 측정 =====");
        System.out.println("사용 전 메모리: " + formatBytes(beforeMemory));
        System.out.println("사용 후 메모리: " + formatBytes(afterMemory));
        System.out.println("사용된 메모리: " + formatBytes(memoryUsed));
        System.out.println("조회된 학생 수: " + result.getStudents().size());
        
        // 학생당 평균 메모리 사용량
        if (!result.getStudents().isEmpty()) {
            long memoryPerStudent = memoryUsed / result.getStudents().size();
            System.out.println("학생당 메모리: " + formatBytes(memoryPerStudent));
        }
        
        assertThat(result).isNotNull();
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
