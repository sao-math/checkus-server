package saomath.checkusserver.notification.service;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 알림 대상자 조회 서비스 인터페이스
 */
public interface NotificationTargetService {
    
    /**
     * 특정 시간의 공부 일정 대상자 조회
     */
    List<StudyTarget> getStudyTargetsForTime(LocalDateTime targetTime);
    
    /**
     * 오늘의 할일 대상자 조회
     */
    List<TaskTarget> getTodayTaskTargets();
    
    /**
     * 어제 미완료 할일 대상자 조회
     */
    List<TaskTarget> getYesterdayIncompleteTaskTargets();
    
    /**
     * 미접속 대상자 조회 (공부 시작 시간 기준)
     */
    List<NoShowTarget> getNoShowTargets(LocalDateTime startTime);
    
    /**
     * 공부 일정 알림 대상
     */
    @Getter
    @Builder
    class StudyTarget {
        private Long studentId;
        private String studentName;
        private String studentPhone;
        private String parentPhone;
        private String activityName;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private boolean parentNotificationEnabled;
        private boolean studentNotificationEnabled;
        
        public String getFormattedStartTime() {
            return String.format("%02d:%02d", startTime.getHour(), startTime.getMinute());
        }
        
        public String getFormattedEndTime() {
            return String.format("%02d:%02d", endTime.getHour(), endTime.getMinute());
        }
    }
    
    /**
     * 할일 알림 대상
     */
    @Getter
    @Builder
    class TaskTarget {
        private Long studentId;
        private String studentName;
        private String studentPhone;
        private String parentPhone;
        private int taskCount;
        private List<String> taskTitles;
        private boolean parentNotificationEnabled;
        private boolean studentNotificationEnabled;
        
        public String getTaskListString() {
            if (taskTitles == null || taskTitles.isEmpty()) {
                return "";
            }
            
            // 최대 5개까지만 표시
            int displayCount = Math.min(taskTitles.size(), 5);
            StringBuilder sb = new StringBuilder();
            
            for (int i = 0; i < displayCount; i++) {
                sb.append("• ").append(taskTitles.get(i)).append("\n");
            }
            
            if (taskTitles.size() > 5) {
                sb.append("... 외 ").append(taskTitles.size() - 5).append("개");
            }
            
            return sb.toString().trim();
        }
    }
    
    /**
     * 미접속 알림 대상
     */
    @Getter
    @Builder
    class NoShowTarget {
        private Long studentId;
        private String studentName;
        private String studentPhone;
        private String parentPhone;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private boolean parentNotificationEnabled;
        private boolean studentNotificationEnabled;
        
        public String getFormattedStartTime() {
            return String.format("%02d:%02d", startTime.getHour(), startTime.getMinute());
        }
        
        public String getFormattedEndTime() {
            return String.format("%02d:%02d", endTime.getHour(), endTime.getMinute());
        }
    }
}
