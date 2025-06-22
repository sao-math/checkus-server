package saomath.checkusserver.school.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import saomath.checkusserver.common.exception.BusinessException;
import saomath.checkusserver.common.exception.DuplicateResourceException;
import saomath.checkusserver.common.exception.ResourceNotFoundException;
import saomath.checkusserver.school.domain.School;
import saomath.checkusserver.school.dto.SchoolRequest;
import saomath.checkusserver.school.dto.SchoolResponse;
import saomath.checkusserver.school.repository.SchoolRepository;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SchoolService 테스트")
class SchoolServiceTest {

    @Mock
    private SchoolRepository schoolRepository;

    @InjectMocks
    private SchoolService schoolService;

    private School testSchool1;
    private School testSchool2;

    @BeforeEach
    void setUp() {
        testSchool1 = School.builder()
                .id(1L)
                .name("이현중학교")
                .build();

        testSchool2 = School.builder()
                .id(2L)
                .name("손곡중학교")
                .build();
    }

    @Test
    @DisplayName("전체 학교 목록 조회 - 학생 수 포함")
    void getAllSchools_ShouldReturnSchoolsWithStudentCount() {
        // given
        List<Object[]> mockResults = Arrays.asList(
                new Object[]{1L, "이현중학교", 15L},
                new Object[]{2L, "손곡중학교", 8L}
        );
        given(schoolRepository.findAllSchoolsWithStudentCount()).willReturn(mockResults);

        // when
        List<SchoolResponse> result = schoolService.getAllSchools();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).satisfies(school -> {
            assertThat(school.getId()).isEqualTo(1L);
            assertThat(school.getName()).isEqualTo("이현중학교");
            assertThat(school.getStudentCount()).isEqualTo(15L);
        });
        assertThat(result.get(1)).satisfies(school -> {
            assertThat(school.getId()).isEqualTo(2L);
            assertThat(school.getName()).isEqualTo("손곡중학교");
            assertThat(school.getStudentCount()).isEqualTo(8L);
        });
        verify(schoolRepository).findAllSchoolsWithStudentCount();
    }

    @Test
    @DisplayName("학교 생성 성공")
    void createSchool_ShouldSucceed_WhenValidRequest() {
        // given
        SchoolRequest request = new SchoolRequest("새로운중학교");
        School savedSchool = School.builder()
                .id(3L)
                .name("새로운중학교")
                .build();

        given(schoolRepository.existsByName("새로운중학교")).willReturn(false);
        given(schoolRepository.save(any(School.class))).willReturn(savedSchool);

        // when
        SchoolResponse result = schoolService.createSchool(request);

        // then
        assertThat(result.getId()).isEqualTo(3L);
        assertThat(result.getName()).isEqualTo("새로운중학교");
        assertThat(result.getStudentCount()).isEqualTo(0L);
        verify(schoolRepository).existsByName("새로운중학교");
        verify(schoolRepository).save(any(School.class));
    }

    @Test
    @DisplayName("학교 생성 실패 - 중복된 학교명")
    void createSchool_ShouldThrowException_WhenDuplicateName() {
        // given
        SchoolRequest request = new SchoolRequest("이현중학교");
        given(schoolRepository.existsByName("이현중학교")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> schoolService.createSchool(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("이미 존재하는 학교명입니다: 이현중학교");
        verify(schoolRepository).existsByName("이현중학교");
        verify(schoolRepository, never()).save(any(School.class));
    }

    @Test
    @DisplayName("학교 삭제 성공 - 연결된 학생이 없는 경우")
    void deleteSchool_ShouldSucceed_WhenNoStudentsConnected() {
        // given
        Long schoolId = 1L;
        given(schoolRepository.existsById(schoolId)).willReturn(true);
        given(schoolRepository.countStudentsBySchoolId(schoolId)).willReturn(0L);

        // when
        assertThatCode(() -> schoolService.deleteSchool(schoolId))
                .doesNotThrowAnyException();

        // then
        verify(schoolRepository).existsById(schoolId);
        verify(schoolRepository).countStudentsBySchoolId(schoolId);
        verify(schoolRepository).deleteById(schoolId);
    }

    @Test
    @DisplayName("학교 삭제 실패 - 학교가 존재하지 않는 경우")
    void deleteSchool_ShouldThrowException_WhenSchoolNotFound() {
        // given
        Long schoolId = 999L;
        given(schoolRepository.existsById(schoolId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> schoolService.deleteSchool(schoolId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("학교를 찾을 수 없습니다: " + schoolId);
        verify(schoolRepository).existsById(schoolId);
        verify(schoolRepository, never()).countStudentsBySchoolId(any());
        verify(schoolRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("학교 삭제 실패 - 연결된 학생이 있는 경우")
    void deleteSchool_ShouldThrowException_WhenStudentsConnected() {
        // given
        Long schoolId = 1L;
        given(schoolRepository.existsById(schoolId)).willReturn(true);
        given(schoolRepository.countStudentsBySchoolId(schoolId)).willReturn(5L);

        // when & then
        assertThatThrownBy(() -> schoolService.deleteSchool(schoolId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("연결된 학생이 있어 학교를 삭제할 수 없습니다. 학생 수: 5");
        verify(schoolRepository).existsById(schoolId);
        verify(schoolRepository).countStudentsBySchoolId(schoolId);
        verify(schoolRepository, never()).deleteById(any());
    }
}