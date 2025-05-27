package saomath.checkusserver.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import saomath.checkusserver.auth.dto.ApiResponse;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
@Tag(name = "Health", description = "헬스체크 API")
public class HealthController {

    @Operation(summary = "서버 상태 확인", description = "서버가 정상적으로 동작하는지 확인합니다.")
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("timestamp", LocalDateTime.now());
        healthInfo.put("service", "CheckUS Server");
        healthInfo.put("version", "1.0.0");
        
        return ResponseEntity.ok(ApiResponse.success("서버가 정상적으로 동작 중입니다.", healthInfo));
    }

    @Operation(summary = "API 버전 확인", description = "현재 API 버전 정보를 반환합니다.")
    @GetMapping("/version")
    public ResponseEntity<ApiResponse<Map<String, String>>> getVersion() {
        Map<String, String> versionInfo = new HashMap<>();
        versionInfo.put("version", "1.0.0");
        versionInfo.put("build", "2024.12.20");
        versionInfo.put("api", "v1");
        
        return ResponseEntity.ok(ApiResponse.success(versionInfo));
    }
}
