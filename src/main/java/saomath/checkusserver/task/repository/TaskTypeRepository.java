package saomath.checkusserver.task.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import saomath.checkusserver.task.domain.TaskType;

@Repository
public interface TaskTypeRepository extends JpaRepository<TaskType, Long> {
    TaskType findByName(String name);
}