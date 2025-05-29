package saomath.checkusserver.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        Server localServer = new Server();
        localServer.setUrl("http://localhost:8080");
        localServer.setDescription("Local Server");

        Contact contact = new Contact()
                .name("CheckUS Team")
                .email("yunjeongiya@gmail.com")
                .url("https://github.com/sao-math");

        License mitLicense = new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT");

        Info info = new Info()
                .title("CheckUS API 문서")
                .version("v1.0.0")
                .description("CheckUS 애플리케이션의 RESTful API 문서입니다.")
                .contact(contact)
                .license(mitLicense);

        // SecurityScheme 정의
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .name("Authorization")
                .in(SecurityScheme.In.HEADER);

        return new OpenAPI()
                .servers(List.of(localServer))
                .info(info)
                // .addSecurityItem(securityRequirement) <- 전역 보안 요구사항 제거
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", securityScheme));
    }
}