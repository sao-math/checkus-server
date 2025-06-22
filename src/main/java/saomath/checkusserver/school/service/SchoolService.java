package saomath.checkusserver.school.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saomath.checkusserver.school.dto.SchoolRequest;
import saomath.checkusserver.school.dto.SchoolResponse;
import saomath.checkusserver.school.domain.School;
import saomath.checkusserver.common.exception.DuplicateResourceException;
import saomath.checkusserver.common.exception.ResourceNotFoundException;
import saomath.checkusserver.common.exception.BusinessException;
import saomath.checkusserver.school.repository.SchoolRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SchoolService {

    private final SchoolRepository schoolRepository;

    /**
     * 전체 학교 목록을 학생 수와 함께 조회합니다.
     * 
     * @return 학교 목록 (학생 수 포함)
     */
    public List<SchoolResponse> getAllSchools() {
        log.debug("전체 학교 목록 조회 (학생 수 포함)");

        List<Object[]> results = schoolRepository.findAllSchoolsWithStudentCount();
        
        log.info("조회된 학교 수: {}", results.size());

        return results.stream()
                .map(result -> new SchoolResponse(
                    (Long) result[0],    // school id
                    (String) result[1],  // school name
                    (Long) result[2]     // student count
                ))
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

        return new SchoolResponse(savedSchool.getId(), savedSchool.getName(), 0L);
    }

    /**
     * 학교를 삭제합니다.
     * 연결된 학생이 있는 경우 삭제할 수 없습니다.
     * 
     * @param schoolId 삭제할 학교 ID
     * @throws ResourceNotFoundException 학교를 찾을 수 없는 경우
     * @throws BusinessException 연결된 학생이 있어 삭제할 수 없는 경우
     */
    @Transactional
    public void deleteSchool(Long schoolId) {
        log.debug("학교 삭제 요청: schoolId={}", schoolId);

        // 학교 존재 확인
        if (!schoolRepository.existsById(schoolId)) {
            log.warn("존재하지 않는 학교 삭제 시도: schoolId={}", schoolId);
            throw new ResourceNotFoundException("학교를 찾을 수 없습니다: " + schoolId);
        }

        // 연결된 학생 수 확인
        Long studentCount = schoolRepository.countStudentsBySchoolId(schoolId);
        if (studentCount > 0) {
            log.warn("연결된 학생이 있는 학교 삭제 시도: schoolId={}, studentCount={}", schoolId, studentCount);
            throw new BusinessException("연결된 학생이 있어 학교를 삭제할 수 없습니다. 학생 수: " + studentCount);
        }

        // 학교 삭제
        schoolRepository.deleteById(schoolId);
        
        log.info("학교 삭제 완료: schoolId={}", schoolId);
    }
} 