package saomath.checkusserver.common.util;

import jakarta.servlet.http.HttpServletRequest;

public class DomainUtils {
    
    public static String getUserTypeFromRequest(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        String referer = request.getHeader("Referer");
        String host = request.getHeader("Host");
        
        // Origin 헤더가 있으면 사용
        String domain = origin != null ? origin : referer;
        
        if (domain == null && host != null) {
            // Host 헤더로 도메인 추정
            domain = "https://" + host;
        }
        
        if (domain != null) { //todo enum으로 관리
            if (domain.contains("teacher.checkus.app")) {
                return "TEACHER";
            } else if (domain.contains("checkus.app")) {
                return "STUDENT"; // 학생 또는 학부모 (회원가입 시 구분)
            } else if (domain.contains("localhost:3001")) {
                return "TEACHER"; // 개발환경 교사용
            } else if (domain.contains("localhost:3000")) {
                return "STUDENT"; // 개발환경 학생/학부모용
            }
        }
        
        return "UNKNOWN";
    }
    
    public static boolean isTeacherDomain(HttpServletRequest request) {
        return "TEACHER".equals(getUserTypeFromRequest(request));
    }
    
    public static boolean isStudentDomain(HttpServletRequest request) {
        return "STUDENT".equals(getUserTypeFromRequest(request));
    }
}
