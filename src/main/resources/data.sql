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

-- 테스트 학교 데이터
INSERT INTO school (name) VALUES ('리플랜고등학교');
INSERT INTO school (name) VALUES ('사오중학교');

-- 과제 타입 데이터
INSERT INTO task_type (name) VALUES ('개념');
INSERT INTO task_type (name) VALUES ('테스트');

-- 테스트 사용자 (개발 환경에서만)
INSERT INTO users (username, name, phone_number, password)
VALUES ('testuser', '테스트 사용자', '010-1234-5678', 'password123');

-- 알림 타입 데이터
INSERT INTO notification_type (name, description) VALUES
                                                      ('STUDY_START_10MIN_BEFORE', '공부 시작 10분 전 알림'),
                                                      ('STUDY_START_TIME', '공부 시작 시간 알림'),
                                                      ('STUDY_ROOM_ENTRY', '스터디룸 입장 알림'),
                                                      ('NO_ATTENDANCE_ALERT', '미접속 알림'),
                                                      ('DAILY_TASKS_MORNING', '오늘의 할일 알림 (아침)'),
                                                      ('INCOMPLETE_TASKS_MORNING', '전날 미완료 할일 알림 (아침)'),
                                                      ('INCOMPLETE_TASKS_EVENING', '전날 미완료 할일 알림 (저녁)');