CREATE TABLE users (
                       id bigint AUTO_INCREMENT PRIMARY KEY,
                       username varchar(50) UNIQUE,
                       name varchar(255),
                       phone_number varchar(20),
                       password varchar(255),
                       discord_id varchar(100),
                       created_at timestamp DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE student_profile (
                                 user_id bigint PRIMARY KEY,
                                 status varchar(20),
                                 school_id bigint,
                                 grade integer,
                                 gender varchar(20)
);

CREATE TABLE school (
                        id bigint AUTO_INCREMENT PRIMARY KEY,
                        name varchar(255) UNIQUE NOT NULL
);

CREATE TABLE role (
                      id bigint AUTO_INCREMENT PRIMARY KEY,
                      name varchar(255) UNIQUE
);

CREATE TABLE user_role (
                           user_id bigint,
                           role_id bigint,
                           PRIMARY KEY (user_id, role_id)
);

CREATE TABLE permission (
                            id bigint AUTO_INCREMENT PRIMARY KEY,
                            name varchar(255) UNIQUE
);

CREATE TABLE role_permission (
                                 role_id bigint,
                                 permission_id bigint,
                                 PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE student_guardian (
                                  student_id bigint,
                                  guardian_id bigint,
                                  relationship varchar(20),
                                  PRIMARY KEY (student_id, guardian_id)
);

CREATE TABLE class (
                       id bigint AUTO_INCREMENT PRIMARY KEY,
                       name varchar(255) NOT NULL
);

CREATE TABLE teacher_class (
                               teacher_id bigint,
                               class_id bigint,
                               PRIMARY KEY (teacher_id, class_id)
);

CREATE TABLE student_class (
                               student_id bigint,
                               class_id bigint,
                               PRIMARY KEY (student_id, class_id)
);

CREATE TABLE activity (
                          id bigint AUTO_INCREMENT PRIMARY KEY,
                          name varchar(255) NOT NULL,
                          is_study_assignable boolean DEFAULT false
);

CREATE TABLE weekly_schedule (
                                 id bigint AUTO_INCREMENT PRIMARY KEY,
                                 student_id bigint,
                                 activity_id bigint,
                                 day_of_week integer,
                                 start_time time,
                                 end_time time,
                                 FOREIGN KEY (activity_id) REFERENCES activity(id)
);

CREATE TABLE assigned_study_time (
                                     id bigint AUTO_INCREMENT PRIMARY KEY,
                                     student_id bigint,
                                     activity_id bigint,
                                     start_time timestamp,
                                     end_time timestamp,
                                     assigned_by bigint,
                                     FOREIGN KEY (activity_id) REFERENCES activity(id)
);

CREATE TABLE actual_study_time (
                                   id bigint AUTO_INCREMENT PRIMARY KEY,
                                   student_id bigint,
                                   assigned_study_time_id bigint,
                                   start_time timestamp,
                                   end_time timestamp,
                                   source varchar(255),
                                   FOREIGN KEY (assigned_study_time_id) REFERENCES assigned_study_time(id)
);

CREATE TABLE task_type (
                           id bigint AUTO_INCREMENT PRIMARY KEY,
                           name varchar(255) NOT NULL
);

CREATE TABLE task (
                      id bigint AUTO_INCREMENT PRIMARY KEY,
                      type_id bigint,
                      parent_id bigint,
                      title varchar(255) NOT NULL,
                      description text,
                      is_leaf boolean DEFAULT false
);

CREATE TABLE material (
                          id bigint AUTO_INCREMENT PRIMARY KEY,
                          task_id bigint,
                          title varchar(255) NOT NULL,
                          is_video boolean,
                          completion_condition varchar(255)
);

CREATE TABLE assigned_task (
                               id bigint AUTO_INCREMENT PRIMARY KEY,
                               task_id bigint,
                               student_id bigint,
                               teacher_id bigint,
                               title varchar(255) NOT NULL,
                               description text,
                               assigned_date timestamp DEFAULT CURRENT_TIMESTAMP,
                               due_date timestamp
);

-- Foreign Key 제약조건들
ALTER TABLE student_profile ADD FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE student_profile ADD FOREIGN KEY (school_id) REFERENCES school (id);
ALTER TABLE user_role ADD FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE user_role ADD FOREIGN KEY (role_id) REFERENCES role (id);
ALTER TABLE role_permission ADD FOREIGN KEY (role_id) REFERENCES role (id);
ALTER TABLE role_permission ADD FOREIGN KEY (permission_id) REFERENCES permission (id);
ALTER TABLE student_guardian ADD FOREIGN KEY (student_id) REFERENCES users (id);
ALTER TABLE student_guardian ADD FOREIGN KEY (guardian_id) REFERENCES users (id);
ALTER TABLE teacher_class ADD FOREIGN KEY (teacher_id) REFERENCES users (id);
ALTER TABLE teacher_class ADD FOREIGN KEY (class_id) REFERENCES class (id);
ALTER TABLE student_class ADD FOREIGN KEY (student_id) REFERENCES users (id);
ALTER TABLE student_class ADD FOREIGN KEY (class_id) REFERENCES class (id);
ALTER TABLE weekly_schedule ADD FOREIGN KEY (student_id) REFERENCES users (id);
ALTER TABLE assigned_study_time ADD FOREIGN KEY (student_id) REFERENCES users (id);
ALTER TABLE assigned_study_time ADD FOREIGN KEY (assigned_by) REFERENCES users (id);
ALTER TABLE actual_study_time ADD FOREIGN KEY (student_id) REFERENCES users (id);
ALTER TABLE task ADD FOREIGN KEY (type_id) REFERENCES task_type (id);
ALTER TABLE task ADD FOREIGN KEY (parent_id) REFERENCES task (id);
ALTER TABLE material ADD FOREIGN KEY (task_id) REFERENCES task (id);
ALTER TABLE assigned_task ADD FOREIGN KEY (task_id) REFERENCES task (id);
ALTER TABLE assigned_task ADD FOREIGN KEY (student_id) REFERENCES users (id);
ALTER TABLE assigned_task ADD FOREIGN KEY (teacher_id) REFERENCES users (id);