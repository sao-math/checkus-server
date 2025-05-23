CREATE TABLE `user` (
  `id` bigint AUTO_INCREMENT PRIMARY KEY,
  `username` varchar(50) UNIQUE,
  `name` varchar(255),
  `phone_number` varchar(20),
  `password` varchar(255),
  `discord_id` varchar(100),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `student_profile` (
  `user_id` bigint PRIMARY KEY COMMENT '학생 역할의 User만 허용',
  `status` varchar(20) COMMENT 'ENUM: INQUIRY(문의), COUNSELING_SCHEDULED(상담예약), ENROLLED(재원), WAITING(대기), WITHDRAWN(퇴원), UNREGISTERED(미등록)',
  `school_id` bigint,
  `grade` integer,
  `gender` varchar(20) COMMENT 'ENUM: MALE(남), FEMALE(여), OTHER(기타)'
);

CREATE TABLE `school` (
  `id` bigint AUTO_INCREMENT PRIMARY KEY,
  `name` varchar(255) UNIQUE NOT NULL
);

CREATE TABLE `role` (
  `id` bigint AUTO_INCREMENT PRIMARY KEY,
  `name` varchar(255) UNIQUE COMMENT '학생, 교사, 보호자 등의 역할'
);

CREATE TABLE `user_role` (
  `user_id` bigint,
  `role_id` bigint,
  PRIMARY KEY (`user_id`, `role_id`)
);

CREATE TABLE `permission` (
  `id` bigint AUTO_INCREMENT PRIMARY KEY,
  `name` varchar(255) UNIQUE
);

CREATE TABLE `role_permission` (
  `role_id` bigint,
  `permission_id` bigint,
  PRIMARY KEY (`role_id`, `permission_id`)
);

CREATE TABLE `student_guardian` (
  `student_id` bigint COMMENT '학생 역할의 User만 허용',
  `guardian_id` bigint COMMENT '보호자 역할의 User만 허용',
  `relationship` varchar(20) COMMENT 'ENUM: FATHER(부), MOTHER(모), OTHER(기타)',
  PRIMARY KEY (`student_id`, `guardian_id`)
);

CREATE TABLE `class` (
  `id` bigint AUTO_INCREMENT PRIMARY KEY,
  `name` varchar(255) NOT NULL
);

CREATE TABLE `teacher_class` (
  `teacher_id` bigint COMMENT '교사 역할의 User만 허용',
  `class_id` bigint,
  PRIMARY KEY (`teacher_id`, `class_id`)
);

CREATE TABLE `student_class` (
  `student_id` bigint COMMENT '학생 역할의 User만 허용',
  `class_id` bigint,
  PRIMARY KEY (`student_id`, `class_id`)
);

CREATE TABLE `weekly_schedule` (
  `id` bigint AUTO_INCREMENT PRIMARY KEY,
  `student_id` bigint COMMENT '학생 역할의 User만 허용',
  `day_of_week` integer COMMENT '0-6: 월-일',
  `start_time` time,
  `end_time` time,
  `activity` varchar(255)
);

CREATE TABLE `assigned_study_time` (
  `id` bigint AUTO_INCREMENT PRIMARY KEY,
  `student_id` bigint COMMENT '학생 역할의 User만 허용',
  `start_time` timestamp,
  `end_time` timestamp,
  `activity` varchar(255),
  `assigned_by` bigint COMMENT '교사 역할의 User만 허용'
);

CREATE TABLE `actual_study_time` (
  `id` bigint AUTO_INCREMENT PRIMARY KEY,
  `student_id` bigint COMMENT '학생 역할의 User만 허용',
  `start_time` timestamp,
  `end_time` timestamp,
  `activity` varchar(255),
  `source` varchar(255) COMMENT '기록 출처(예: 디스코드)'
);

CREATE TABLE `task_type` (
  `id` bigint AUTO_INCREMENT PRIMARY KEY,
  `name` varchar(255) NOT NULL
);

CREATE TABLE `task` (
  `id` bigint AUTO_INCREMENT PRIMARY KEY,
  `type_id` bigint,
  `parent_id` bigint,
  `title` varchar(255) NOT NULL,
  `description` text,
  `is_leaf` boolean DEFAULT false COMMENT '실제 과제인지 여부'
);

CREATE TABLE `material` (
  `id` bigint AUTO_INCREMENT PRIMARY KEY,
  `task_id` bigint,
  `title` varchar(255) NOT NULL,
  `is_video` boolean,
  `completion_condition` varchar(255)
);

CREATE TABLE `assigned_task` (
  `id` bigint AUTO_INCREMENT PRIMARY KEY,
  `task_id` bigint COMMENT '원본 과제와의 논리적 연결',
  `student_id` bigint COMMENT '학생 역할의 User만 허용',
  `teacher_id` bigint COMMENT '교사 역할의 User만 허용',
  `title` varchar(255) NOT NULL,
  `description` text,
  `assigned_date` timestamp DEFAULT (now()),
  `due_date` timestamp
);

ALTER TABLE `student_profile` ADD FOREIGN KEY (`user_id`) REFERENCES `user` (`id`);

ALTER TABLE `student_profile` ADD FOREIGN KEY (`school_id`) REFERENCES `school` (`id`);

ALTER TABLE `user_role` ADD FOREIGN KEY (`user_id`) REFERENCES `user` (`id`);

ALTER TABLE `user_role` ADD FOREIGN KEY (`role_id`) REFERENCES `role` (`id`);

ALTER TABLE `role_permission` ADD FOREIGN KEY (`role_id`) REFERENCES `role` (`id`);

ALTER TABLE `role_permission` ADD FOREIGN KEY (`permission_id`) REFERENCES `permission` (`id`);

ALTER TABLE `student_guardian` ADD FOREIGN KEY (`student_id`) REFERENCES `user` (`id`);

ALTER TABLE `student_guardian` ADD FOREIGN KEY (`guardian_id`) REFERENCES `user` (`id`);

ALTER TABLE `teacher_class` ADD FOREIGN KEY (`teacher_id`) REFERENCES `user` (`id`);

ALTER TABLE `teacher_class` ADD FOREIGN KEY (`class_id`) REFERENCES `class` (`id`);

ALTER TABLE `student_class` ADD FOREIGN KEY (`student_id`) REFERENCES `user` (`id`);

ALTER TABLE `student_class` ADD FOREIGN KEY (`class_id`) REFERENCES `class` (`id`);

ALTER TABLE `weekly_schedule` ADD FOREIGN KEY (`student_id`) REFERENCES `user` (`id`);

ALTER TABLE `assigned_study_time` ADD FOREIGN KEY (`student_id`) REFERENCES `user` (`id`);

ALTER TABLE `assigned_study_time` ADD FOREIGN KEY (`assigned_by`) REFERENCES `user` (`id`);

ALTER TABLE `actual_study_time` ADD FOREIGN KEY (`student_id`) REFERENCES `user` (`id`);

ALTER TABLE `task` ADD FOREIGN KEY (`type_id`) REFERENCES `task_type` (`id`);

ALTER TABLE `task` ADD FOREIGN KEY (`parent_id`) REFERENCES `task` (`id`);

ALTER TABLE `material` ADD FOREIGN KEY (`task_id`) REFERENCES `task` (`id`);

ALTER TABLE `assigned_task` ADD FOREIGN KEY (`task_id`) REFERENCES `task` (`id`);

ALTER TABLE `assigned_task` ADD FOREIGN KEY (`student_id`) REFERENCES `user` (`id`);

ALTER TABLE `assigned_task` ADD FOREIGN KEY (`teacher_id`) REFERENCES `user` (`id`);