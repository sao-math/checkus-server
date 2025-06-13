package saomath.checkusserver.notification.service;

import saomath.checkusserver.notification.domain.AlimtalkTemplate;
import java.util.Map;

public interface AlimtalkService {
    
    /**
     * 알림톡 발송
     * @param phoneNumber 수신자 전화번호
     * @param template 알림톡 템플릿
     * @param variables 템플릿 변수
     * @return 발송 성공 여부
     */
    boolean sendAlimtalk(String phoneNumber, AlimtalkTemplate template, Map<String, String> variables);
    
    /**
     * 여러 사용자에게 알림톡 발송
     * @param phoneNumbers 수신자 전화번호 목록
     * @param template 알림톡 템플릿
     * @param variables 템플릿 변수
     * @return 발송 성공 건수
     */
    int sendBulkAlimtalk(String[] phoneNumbers, AlimtalkTemplate template, Map<String, String> variables);
}
