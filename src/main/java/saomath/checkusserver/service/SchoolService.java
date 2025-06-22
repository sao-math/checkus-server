package saomath.checkusserver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saomath.checkusserver.dto.SchoolRequest;
import saomath.checkusserver.dto.SchoolResponse;
import saomath.checkusserver.entity.School;
import saomath.checkusserver.common.exception.DuplicateResourceException;
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

    /**
     * 새로운 학교를 생성합니다.
     * 
     * @param schoolRequest 학교 생성 요청 정보
     * @return 생성된 학교 정보
     * @throws DuplicateResourceException 동일한 이름의 학교가 이미 존재하는 경우
     */
    @Transactional
    public SchoolResponse createSchool(SchoolRequest schoolRequest) {
        log.debug("학교 생성 요청: {}", schoolRequest.getName());

        // 중복 학교명 확인
        if (schoolRepository.existsByName(schoolRequest.getName())) {
            log.warn("중복된 학교명으로 생성 시도: {}", schoolRequest.getName());
            throw new DuplicateResourceException("이미 존재하는 학교명입니다: " + schoolRequest.getName());
        }

        // 새 학교 생성
        School school = School.builder()
                .name(schoolRequest.getName())
                .build();

        School savedSchool = schoolRepository.save(school);
        
        log.info("새 학교 생성 완료: {} (ID: {})", savedSchool.getName(), savedSchool.getId());

        return new SchoolResponse(savedSchool.getId(), savedSchool.getName());
    }
} 