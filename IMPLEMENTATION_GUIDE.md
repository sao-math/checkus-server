# CheckUS Server - 인증 시스템

학습 관리 시스템을 위한 Spring Boot 기반 백엔드 서버입니다.

## 주요 기능

### 인증 및 권한
- JWT 기반 인증 (Access Token + Refresh Token)
- 역할 기반 접근 제어 (RBAC)
- 도메인별 회원가입 분기
- 비밀번호 복잡도 검증
- 사용자명/전화번호 중복 확인

### 사용자 역할
- **학생 (STUDENT)**: 학습 활동 수행
- **교사 (TEACHER)**: 학생 관리, 과제 배정
- **학부모 (GUARDIAN)**: 자녀 학습 현황 모니터링
- **관리자 (ADMIN)**: 전체 시스템 관리

### API 엔드포인트

#### 인증 관련 (`/api/auth/`)
```
POST /register/student    - 학생 회원가입
POST /register/guardian   - 학부모 회원가입  
POST /register/teacher    - 교사 회원가입
POST /login              - 로그인
POST /refresh            - 토큰 리프레시
POST /logout             - 로그아웃
GET  /me                 - 내 정보 조회
GET  /check-username     - 사용자명 중복 확인
GET  /check-phone        - 전화번호 중복 확인
```

#### 관리자 전용 (`/api/admin/`)
```
GET  /role-requests      - 역할 승인 요청 목록
POST /approve-role       - 역할 승인
POST /suspend-role       - 역할 일시정지
GET  /user-roles/{id}    - 사용자 역할 조회
```

#### 공개 API (`/api/public/`)
```
GET  /health            - 서버 상태 확인
GET  /version           - API 버전 확인
```

## 기술 스택

- **Framework**: Spring Boot 3.4.5
- **언어**: Java 21
- **데이터베이스**: MySQL (운영), H2 (테스트)
- **보안**: Spring Security + JWT
- **문서화**: Swagger/OpenAPI 3
- **테스트**: JUnit 5, Mockito
- **빌드 도구**: Gradle

## 개발 환경 설정

### 1. 의존성 설치
```bash
./gradlew build
```

### 2. 데이터베이스 설정
MySQL 데이터베이스를 생성하고 `application-local.yml`에서 연결 정보를 설정합니다.

### 3. 애플리케이션 실행
```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

### 4. Swagger 문서 확인
http://localhost:8080/swagger-ui.html

## 테스트 실행

```bash
# 전체 테스트
./gradlew test

# 특정 테스트 클래스
./gradlew test --tests AuthServiceTest
```

## 환경별 설정

### Local (개발환경)
- 프로파일: `local`
- 데이터베이스: MySQL
- 보안: 느슨한 CORS 설정

### Test (테스트환경)  
- 프로파일: `test`
- 데이터베이스: H2 (in-memory)
- 디스코드 봇: 비활성화

### Production (운영환경)
- 프로파일: `prod`
- 데이터베이스: MySQL
- 보안: 강화된 설정

## 주요 검증 규칙

### 비밀번호
- 최소 8자 이상
- 영문자, 숫자, 특수문자 모두 포함

### 전화번호
- 형식: 010-0000-0000

### 사용자명
- 4-20자의 영문자, 숫자, 언더스코어만 허용

## 보안 설정

### CORS
- 허용 도메인: checkus.app, teacher.checkus.app
- 개발환경: localhost:3000, localhost:3001

### JWT
- Access Token: 1시간
- Refresh Token: 7일
- 자동 토큰 정리 스케줄러 포함

## 데이터베이스 스키마

주요 테이블:
- `users`: 사용자 기본 정보
- `student_profile`: 학생 상세 정보
- `role`, `user_role`: 역할 관리
- `refresh_token`: 리프레시 토큰 저장

## 배포

Docker를 사용한 배포 설정이 포함되어 있습니다.

```bash
# Docker 이미지 빌드
docker build -t checkus-server .

# 컨테이너 실행
docker run -p 8080:8080 checkus-server
```

## 라이선스

이 프로젝트는 private 프로젝트입니다.
