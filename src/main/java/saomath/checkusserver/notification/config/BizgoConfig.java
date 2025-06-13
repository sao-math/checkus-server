package saomath.checkusserver.notification.config;

import net.infobank.client.service.auth.AuthService;
import net.infobank.client.InfobankClient;
import net.infobank.client.core.HttpConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class BizgoConfig {
    
    private final BizgoProperties bizgoProperties;
    
    @Bean
    public InfobankClient infobankClient() {
        HttpConfig httpConfig = HttpConfig.builder()
                .baseUrl(bizgoProperties.getApi().getBaseUrl())
                .build();
        
        return InfobankClient.builder()
                .clientId(bizgoProperties.getApi().getClientId())
                .password(bizgoProperties.getApi().getClientPassword())
                .httpConfig(httpConfig)
                .build();
    }
    
    @Bean
    public AuthService authService() {
        HttpConfig httpConfig = HttpConfig.builder()
                .baseUrl(bizgoProperties.getApi().getBaseUrl())
                .build();
                
        return new AuthService(
                httpConfig,
                null,
                bizgoProperties.getApi().getClientId(),
                bizgoProperties.getApi().getClientPassword()
        );
    }
}
