-- 기본 역할 데이터
INSERT INTO role (name) VALUES ('STUDENT');
INSERT INTO role (name) VALUES ('TEACHER');
INSERT INTO role (name) VALUES ('TUTOR');
INSERT INTO role (name) VALUES ('GUARDIAN');
INSERT INTO role (name) VALUES ('ADMIN');

-- 기본 권한 데이터
INSERT INTO permission (name) VALUES ('READ_STUDENT_INFO');
INSERT INTO permission (name) VALUES ('WRITE_STUDENT_INFO');
INSERT INTO permission (name) VALUES ('ASSIGN_TASK');
INSERT INTO permission (name) VALUES ('VIEW_SCHEDULE');
INSERT INTO permission (name) VALUES ('MANAGE_USERS');
INSERT INTO permission (name) VALUES ('APPROVE_ROLES');

-- 역할-권한 매핑
-- 학생 권한
INSERT INTO role_permission (role_id, permission_id) 
SELECT r.id, p.id FROM role r, permission p 
WHERE r.name = 'STUDENT' AND p.name = 'VIEW_SCHEDULE';

-- 교사 권한
INSERT INTO role_permission (role_id, permission_id) 
SELECT r.id, p.id FROM role r, permission p 
WHERE r.name = 'TEACHER' AND p.name IN ('READ_STUDENT_INFO', 'WRITE_STUDENT_INFO', 'ASSIGN_TASK', 'VIEW_SCHEDULE');

-- 관리자 권한 (모든 권한)
INSERT INTO role_permission (role_id, permission_id) 
SELECT r.id, p.id FROM role r, permission p 
WHERE r.name = 'ADMIN';

-- 테스트 학교 데이터
INSERT INTO school (name) VALUES ('리플랜고등학교');
INSERT INTO school (name) VALUES ('사오중학교');
INSERT INTO school (name) VALUES ('테스트고등학교');

-- 과제 타입 데이터
INSERT INTO task_type (name) VALUES ('개념');
INSERT INTO task_type (name) VALUES ('테스트');
INSERT INTO task_type (name) VALUES ('과제');
INSERT INTO task_type (name) VALUES ('숙제');

-- 개발환경 테스트 관리자 사용자 (비밀번호: Admin123!@#)
INSERT INTO users (username, name, phone_number, password, discord_id)
VALUES ('admin', '관리자', '010-0000-0000', '$2a$10$example.hashed.password.here', null);

-- 관리자 역할 할당 (활성화 상태)
INSERT INTO user_role (user_id, role_id, status)
SELECT u.id, r.id, 'ACTIVE'
FROM users u, role r
WHERE u.username = 'admin' AND r.name = 'ADMIN';

-- 알림 타입 데이터
INSERT INTO notification_type (name, description) VALUES
    ('STUDY_START_10MIN_BEFORE', '공부 시작 10분 전 알림'),
    ('STUDY_START_TIME', '공부 시작 시간 알림'),
    ('STUDY_ROOM_ENTRY', '스터디룸 입장 알림'),
    ('NO_ATTENDANCE_ALERT', '미접속 알림'),
    ('DAILY_TASKS_MORNING', '오늘의 할일 알림 (아침)'),
    ('INCOMPLETE_TASKS_MORNING', '전날 미완료 할일 알림 (아침)'),
    ('INCOMPLETE_TASKS_EVENING', '전날 미완료 할일 알림 (저녁)');