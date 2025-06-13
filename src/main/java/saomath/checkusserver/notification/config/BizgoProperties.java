package saomath.checkusserver.notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "bizgo")
@Getter
@Setter
public class BizgoProperties {
    
    private Api api = new Api();
    private String senderKey;
    
    @Getter
    @Setter
    public static class Api {
        private String baseUrl = "https://omni.ibapi.kr";
        private String clientId;
        private String clientPassword;
    }
}
