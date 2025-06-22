package saomath.checkusserver.notification.domain;

/**
 * 알림 전송 방법 열거형
 */
public enum DeliveryMethod {
    ALIMTALK("alimtalk", "카카오톡 알림톡"),
    DISCORD("discord", "디스코드");
    
    private final String value;
    private final String displayName;
    
    DeliveryMethod(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }
    
    public String getValue() {
        return value;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 문자열로부터 DeliveryMethod 찾기
     */
    public static DeliveryMethod fromValue(String value) {
        if (value == null) return null;
        
        for (DeliveryMethod method : values()) {
            if (method.value.equalsIgnoreCase(value)) {
                return method;
            }
        }
        return null;
    }
    
    /**
     * 유효한 전송 방법인지 확인
     */
    public static boolean isValid(String value) {
        return fromValue(value) != null;
    }
} 