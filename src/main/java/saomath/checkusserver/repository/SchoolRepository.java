package saomath.checkusserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import saomath.checkusserver.entity.School;

@Repository
public interface SchoolRepository extends JpaRepository<School, Long> {
    School findByName(String name);
}