# 알림 시스템 리팩토링 노트

## 현재 상황 분석 (2024-06-16)

### ✅ 동작하는 부분들
- `UnifiedNotificationScheduler`: 스케줄러가 정상 동작 중
- `AlimtalkTemplate` enum: 템플릿 정의 및 관리
- `DirectAlimtalkService`: 알림톡 발송 기능
- `DiscordNotificationService`: 디스코드 DM 발송 기능
- 각종 스케줄링된 알림 (10분전, 시작시간, 미접속 등)

### 🚨 문제점들
1. **DB 스키마와 Entity 불일치**
   - 스키마에 정의된 알림 관련 테이블들에 대응하는 Entity 없음
   - `notification_type`, `notification_setting`, `scheduled_notification`, `notification_history`

2. **하드코딩된 알림 설정**
   - `NotificationPreferenceServiceImpl`에서 모든 사용자에게 알림톡/디스코드 활성화
   - 실제 사용자 개별 설정 반영 불가

3. **사용하지 않는 테이블들**
   - `scheduled_notification`: 현재 즉시 발송 방식 사용
   - `notification_history`: 로깅만 하고 DB 저장 안함
   - `notification_type`: `AlimtalkTemplate` enum이 동일 역할

## 해결 방향

### 즉시 수정 (배포용)
- `NotificationSetting` Entity만 생성
- `notification_type_id` 대신 `template_name` (string) 사용
- 현재 하드코딩된 설정을 DB 기반으로 변경

### 장기 개선 계획
1. **불필요한 테이블 정리**
   - `notification_type` 테이블 제거 검토
   - `scheduled_notification` 사용 여부 결정
   - `notification_history` 활용 방안 검토

2. **템플릿 코드 정리**
   - 기능명세서의 템플릿 코드와 `AlimtalkTemplate` enum 일치
   - D0004 (스터디룸 입장 완료) 추가

3. **API 추가**
   - 사용자별 알림 설정 관리 API
   - 알림 발송 테스트 API

## 템플릿 정리 필요사항

### 기능명세서에 정의된 템플릿
- D0001: 공부시작 10분전 알림
- D0002: 공부시작 알림  
- D0003: 미입장 알림
- D0004: 스터디룸 입장 완료 (코드에 없음)
- S0001: 학습 알림(아침) - 오늘의 할일
- S0002: 학습 알림(저녁) - 미완료 과제

### 코드에만 있는 추가 템플릿
- EARLY_LEAVE (E0001): 조기퇴장 알림
- LATE_ARRIVAL (L0001): 늦은입장 알림

## 현재 알림 발송 플로우

1. **스케줄러가 시간 체크** (`UnifiedNotificationScheduler`)
2. **대상자 조회** (`NotificationTargetService`)
3. **사용자 알림 설정 조회** (`NotificationPreferenceService`) ← 여기가 하드코딩됨
4. **멀티채널 발송** (`MultiChannelNotificationService`)
5. **각 채널별 전송** (`DirectAlimtalkService`, `DiscordNotificationService`)

## 주의사항

### 건드리면 안 되는 부분들
- `UnifiedNotificationScheduler`의 cron 스케줄링 로직
- `AlimtalkTemplate` enum 구조 (템플릿 내용 수정은 OK)
- 각 채널별 발송 서비스 (`DirectAlimtalkService`, `DiscordNotificationService`)
- 현재 동작하는 스케줄링 시간들

### 수정해도 되는 부분들
- `NotificationPreferenceServiceImpl` 내부 로직
- 사용자 알림 설정 관련 부분
- 템플릿 메시지 내용

## 참고사항

### 현재 알림 발송 시간
- 매분: 10분 전 알림, 시작 시간 알림
- 매 5분: 미접속 체크 (15분 후)
- 매일 08:00: 오늘의 할일 알림
- 매일 08:30: 전날 미완료 할일 알림 (아침)
- 매일 20:00: 전날 미완료 할일 알림 (저녁)

### 알림 채널
- 알림톡: 전화번호 기반, 학부모 주요 채널
- 디스코드: discord_id 기반, 학생 주요 채널

### 기능명세서 링크된 내용
- 알림 설정: 항목별 토글 가능
- 시간 설정: 10분 전 알림 등
- 다중 채널: 알림톡 + 디스코드 동시 발송

---

**작성일**: 2024-06-16  
**작성자**: Claude  
**목적**: 배포 전 최소 수정 및 향후 개선 가이드
