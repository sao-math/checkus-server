-- 테스트용 기본 데이터 생성
-- test-data-setup.sql

-- 역할 생성
INSERT INTO role (id, name) VALUES (1, 'STUDENT') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO role (id, name) VALUES (2, 'TEACHER') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO role (id, name) VALUES (3, 'GUARDIAN') ON DUPLICATE KEY UPDATE name=name;

-- 학교 생성
INSERT INTO school (id, name) VALUES (1, '테스트 중학교') ON DUPLICATE KEY UPDATE name=name;

-- 활동 생성
INSERT INTO activity (id, name, is_study_assignable) VALUES (1, '수학', true) ON DUPLICATE KEY UPDATE name=name;
INSERT INTO activity (id, name, is_study_assignable) VALUES (2, '영어', true) ON DUPLICATE KEY UPDATE name=name;

-- 학생 50명 생성
INSERT INTO users (id, username, name, phone_number, discord_id) VALUES 
(101, 'student001', '김학생01', '010-1001-0001', 'discord001') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO users (id, username, name, phone_number, discord_id) VALUES 
(102, 'student002', '김학생02', '010-1001-0002', 'discord002') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO users (id, username, name, phone_number, discord_id) VALUES 
(103, 'student003', '김학생03', '010-1001-0003', 'discord003') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO users (id, username, name, phone_number, discord_id) VALUES 
(104, 'student004', '김학생04', '010-1001-0004', 'discord004') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO users (id, username, name, phone_number, discord_id) VALUES 
(105, 'student005', '김학생05', '010-1001-0005', 'discord005') ON DUPLICATE KEY UPDATE name=name;

-- 학생 10명만 우선 생성 (더 많은 학생은 large-test-data-setup.sql에서)

-- 보호자 생성
INSERT INTO users (id, username, name, phone_number) VALUES 
(201, 'parent001', '김부모01', '010-2001-0001') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO users (id, username, name, phone_number) VALUES 
(202, 'parent002', '김부모02', '010-2001-0002') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO users (id, username, name, phone_number) VALUES 
(203, 'parent003', '김부모03', '010-2001-0003') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO users (id, username, name, phone_number) VALUES 
(204, 'parent004', '김부모04', '010-2001-0004') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO users (id, username, name, phone_number) VALUES 
(205, 'parent005', '김부모05', '010-2001-0005') ON DUPLICATE KEY UPDATE name=name;

-- 사용자 역할 할당
INSERT INTO user_role (user_id, role_id, status) VALUES (101, 1, 'ACTIVE') ON DUPLICATE KEY UPDATE status=status;
INSERT INTO user_role (user_id, role_id, status) VALUES (102, 1, 'ACTIVE') ON DUPLICATE KEY UPDATE status=status;
INSERT INTO user_role (user_id, role_id, status) VALUES (103, 1, 'ACTIVE') ON DUPLICATE KEY UPDATE status=status;
INSERT INTO user_role (user_id, role_id, status) VALUES (104, 1, 'ACTIVE') ON DUPLICATE KEY UPDATE status=status;
INSERT INTO user_role (user_id, role_id, status) VALUES (105, 1, 'ACTIVE') ON DUPLICATE KEY UPDATE status=status;

INSERT INTO user_role (user_id, role_id, status) VALUES (201, 3, 'ACTIVE') ON DUPLICATE KEY UPDATE status=status;
INSERT INTO user_role (user_id, role_id, status) VALUES (202, 3, 'ACTIVE') ON DUPLICATE KEY UPDATE status=status;
INSERT INTO user_role (user_id, role_id, status) VALUES (203, 3, 'ACTIVE') ON DUPLICATE KEY UPDATE status=status;
INSERT INTO user_role (user_id, role_id, status) VALUES (204, 3, 'ACTIVE') ON DUPLICATE KEY UPDATE status=status;
INSERT INTO user_role (user_id, role_id, status) VALUES (205, 3, 'ACTIVE') ON DUPLICATE KEY UPDATE status=status;

-- 학생 프로필 생성
INSERT INTO student_profile (user_id, status, school_id, grade, gender) VALUES 
(101, 'ACTIVE', 1, 1, 'MALE') ON DUPLICATE KEY UPDATE status=status;
INSERT INTO student_profile (user_id, status, school_id, grade, gender) VALUES 
(102, 'ACTIVE', 1, 1, 'FEMALE') ON DUPLICATE KEY UPDATE status=status;
INSERT INTO student_profile (user_id, status, school_id, grade, gender) VALUES 
(103, 'ACTIVE', 1, 2, 'MALE') ON DUPLICATE KEY UPDATE status=status;
INSERT INTO student_profile (user_id, status, school_id, grade, gender) VALUES 
(104, 'ACTIVE', 1, 2, 'FEMALE') ON DUPLICATE KEY UPDATE status=status;
INSERT INTO student_profile (user_id, status, school_id, grade, gender) VALUES 
(105, 'ACTIVE', 1, 3, 'MALE') ON DUPLICATE KEY UPDATE status=status;

-- 학생-보호자 관계 설정
INSERT INTO student_guardian (student_id, guardian_id, relationship) VALUES 
(101, 201, '부') ON DUPLICATE KEY UPDATE relationship=relationship;
INSERT INTO student_guardian (student_id, guardian_id, relationship) VALUES 
(102, 202, '모') ON DUPLICATE KEY UPDATE relationship=relationship;
INSERT INTO student_guardian (student_id, guardian_id, relationship) VALUES 
(103, 203, '부') ON DUPLICATE KEY UPDATE relationship=relationship;
INSERT INTO student_guardian (student_id, guardian_id, relationship) VALUES 
(104, 204, '모') ON DUPLICATE KEY UPDATE relationship=relationship;
INSERT INTO student_guardian (student_id, guardian_id, relationship) VALUES 
(105, 205, '부') ON DUPLICATE KEY UPDATE relationship=relationship;

-- 할당된 공부시간 생성 (오늘 날짜 기준)
INSERT INTO assigned_study_time (id, student_id, activity_id, title, start_time, end_time, assigned_by)
VALUES (1001, 101, 1, '수학 공부', NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 1 HOUR, 1)
ON DUPLICATE KEY UPDATE title=title;

INSERT INTO assigned_study_time (id, student_id, activity_id, title, start_time, end_time, assigned_by)
VALUES (1002, 102, 2, '영어 공부', NOW() + INTERVAL 1 HOUR, NOW() + INTERVAL 2 HOUR, 1)
ON DUPLICATE KEY UPDATE title=title;

-- 실제 접속 기록 생성
INSERT INTO actual_study_time (id, student_id, assigned_study_time_id, start_time, end_time, source)
VALUES (2001, 101, 1001, NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 1 HOUR, 'discord')
ON DUPLICATE KEY UPDATE source=source;

-- 할당되지 않은 접속 기록
INSERT INTO actual_study_time (id, student_id, assigned_study_time_id, start_time, end_time, source)
VALUES (2002, 103, NULL, NOW() - INTERVAL 3 HOUR, NOW() - INTERVAL 2 HOUR, 'discord')
ON DUPLICATE KEY UPDATE source=source;
