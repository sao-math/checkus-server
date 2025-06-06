package saomath.checkusserver.util;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class JwtTestUtilsTest {

    @Autowired
    private JwtTestUtils jwtTestUtils;

    @Test
    void generateStudentToken_Success() {
        // Given
        Long userId = 1L;
        String username = "student1";

        // When
        String token = jwtTestUtils.generateStudentToken(userId, username);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
    }

    @Test
    void generateTeacherToken_Success() {
        // Given
        Long userId = 2L;
        String username = "teacher1";

        // When
        String token = jwtTestUtils.generateTeacherToken(userId, username);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
    }

    @Test
    void generateAdminToken_Success() {
        // Given
        Long userId = 3L;
        String username = "admin1";

        // When
        String token = jwtTestUtils.generateAdminToken(userId, username);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
    }

    @Test
    void generateCustomToken_Success() {
        // Given
        Long userId = 4L;
        String username = "custom1";
        List<String> roles = List.of("STUDENT", "TEACHER");

        // When
        String token = jwtTestUtils.generateCustomToken(userId, username, roles);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
    }

    @Test
    void toBearerToken_Success() {
        // Given
        String token = "test.jwt.token";

        // When
        String bearerToken = jwtTestUtils.toBearerToken(token);

        // Then
        assertThat(bearerToken).isEqualTo("Bearer test.jwt.token");
    }
}
