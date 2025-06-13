package saomath.checkusserver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saomath.checkusserver.dto.SchoolResponse;
import saomath.checkusserver.entity.School;
import saomath.checkusserver.repository.SchoolRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SchoolService {

    private final SchoolRepository schoolRepository;

    /**
     * 전체 학교 목록을 조회합니다.
     * 
     * @return 학교 목록
     */
    public List<SchoolResponse> getAllSchools() {
        log.debug("전체 학교 목록 조회");

        List<School> schools = schoolRepository.findAll();
        
        log.info("조회된 학교 수: {}", schools.size());

        return schools.stream()
                .map(school -> new SchoolResponse(school.getId(), school.getName()))
                .collect(Collectors.toList());
    }
} 