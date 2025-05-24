package saomath.checkusserver.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import saomath.checkusserver.entity.User;
import saomath.checkusserver.service.UserService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/public/users")
@Tag(name = "User", description = "사용자 관리 API")
public class UserController {

    private final UserService userService;
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 모든 사용자 목록을 조회합니다.
     * GET /api/public/users
     * @return 전체 사용자 목록
     */
    @Operation(summary = "모든 사용자 조회", description = "데이터베이스에 저장된 모든 사용자 정보를 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "성공", 
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = User.class))))
    })
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        logger.info("getAllUsers: 모든 사용자 목록 조회");
        List<User> users = userService.getAllUsers();
        logger.info("Total users found: {}", users.size());
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    /**
     * ID로 특정 사용자를 조회합니다.
     * GET /api/public/users/{id}
     * @param id 사용자 ID
     * @return 사용자 정보
     */
    @Operation(summary = "ID로 사용자 조회", description = "특정 ID의 사용자 정보를 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "성공", 
                    content = @Content(schema = @Schema(implementation = User.class))),
        @ApiResponse(responseCode = "404", description = "사용자 없음")
    })
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(
            @Parameter(description = "사용자 ID", required = true, example = "1") 
            @PathVariable("id") Long id) {
        logger.info("getUserById: id={}", id);
        Optional<User> user = userService.getUserById(id);
        if (user.isPresent()) {
            logger.info("User found: {}", user.get().getUsername());
            return new ResponseEntity<>(user.get(), HttpStatus.OK);
        } else {
            logger.warn("User not found: id={}", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * 사용자명으로 특정 사용자를 조회합니다.
     * GET /api/public/users/username/{username}
     * @param username 사용자명
     * @return 사용자 정보
     */
    @Operation(summary = "사용자명으로 사용자 조회", description = "특정 사용자명을 가진 사용자 정보를 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "성공", 
                    content = @Content(schema = @Schema(implementation = User.class))),
        @ApiResponse(responseCode = "404", description = "사용자 없음")
    })
    @GetMapping("/username/{username}")
    public ResponseEntity<User> getUserByUsername(
            @Parameter(description = "사용자명", required = true, example = "testuser") 
            @PathVariable("username") String username) {
        logger.info("getUserByUsername: username={}", username);
        User user = userService.getUserByUsername(username);
        if (user != null) {
            logger.info("User found: id={}", user.getId());
            return new ResponseEntity<>(user, HttpStatus.OK);
        } else {
            logger.warn("User not found: username={}", username);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}