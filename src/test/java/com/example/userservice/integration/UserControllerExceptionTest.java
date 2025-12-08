package com.example.userservice.integration;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.example.userservice.dto.UserRequestDTO;
import com.example.userservice.dto.UserResponseDTO;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.service.CacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class UserControllerExceptionTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @Autowired private CacheService cacheService;

    @Autowired private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        // Очищаем кэш и базу данных перед каждым тестом
        cacheService.evictAllUserCaches();
        userRepository.deleteAll();
    }

    private String generateUniqueEmail() {
        return "user." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    @Test
    void getUserById_ShouldReturnNotFound_WhenUserNotExists() throws Exception {
        // Act & Assert
        mockMvc
                .perform(get("/api/users/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(containsString("User not found")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void updateUser_ShouldReturnNotFound_WhenUserNotExists() throws Exception {
        // Arrange
        UserRequestDTO requestDTO = new UserRequestDTO();
        requestDTO.setName("Updated");
        requestDTO.setSurname("Name");
        requestDTO.setEmail(generateUniqueEmail());

        // Act & Assert
        mockMvc
                .perform(
                        put("/api/users/{id}", 999L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(containsString("User not found")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void deleteUser_ShouldReturnNotFound_WhenUserNotExists() throws Exception {
        // Act & Assert
        mockMvc
                .perform(delete("/api/users/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(containsString("User not found")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void activateUser_ShouldReturnNotFound_WhenUserNotExists() throws Exception {
        // Act & Assert - используем существующий ID, который точно не существует
        mockMvc
                .perform(patch("/api/users/{id}/activate", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(containsString("User not found")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void deactivateUser_ShouldReturnNotFound_WhenUserNotExists() throws Exception {
        // Act & Assert - используем существующий ID, который точно не существует
        mockMvc
                .perform(patch("/api/users/{id}/deactivate", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(containsString("User not found")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void getUserWithCardsById_ShouldReturnNotFound_WhenUserNotExists() throws Exception {
        // Act & Assert
        mockMvc
                .perform(get("/api/users/{id}/with-cards", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(containsString("User not found")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void getUserCards_ShouldReturnNotFound_WhenUserNotExists() throws Exception {
        // Act & Assert - используем существующий ID, который точно не существует
        mockMvc
                .perform(get("/api/users/{userId}/cards", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(containsString("User not found")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // ========== DuplicateEmailException Tests ==========

    @Test
    void createUser_ShouldReturnConflict_WhenEmailDuplicate() throws Exception {
        // Arrange
        String email = generateUniqueEmail();
        UserRequestDTO requestDTO1 = new UserRequestDTO();
        requestDTO1.setName("First");
        requestDTO1.setSurname("User");
        requestDTO1.setEmail(email);

        UserRequestDTO requestDTO2 = new UserRequestDTO();
        requestDTO2.setName("Second");
        requestDTO2.setSurname("User");
        requestDTO2.setEmail(email);

        // Create first user
        mockMvc
                .perform(
                        post("/api/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDTO1)))
                .andExpect(status().isCreated());

        // Act & Assert - try to create second user with same email
        mockMvc
                .perform(
                        post("/api/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDTO2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(containsString("already exists")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void updateUser_ShouldReturnConflict_WhenEmailDuplicate() throws Exception {
        // Arrange - create two users
        String email1 = generateUniqueEmail();
        String email2 = generateUniqueEmail();

        UserRequestDTO user1 = new UserRequestDTO();
        user1.setName("User1");
        user1.setSurname("Test");
        user1.setEmail(email1);

        UserRequestDTO user2 = new UserRequestDTO();
        user2.setName("User2");
        user2.setSurname("Test");
        user2.setEmail(email2);

        String response1 =
                mockMvc
                        .perform(
                                post("/api/users")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(user1)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        mockMvc
                .perform(
                        post("/api/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(user2)))
                .andExpect(status().isCreated());

        UserResponseDTO createdUser1 = objectMapper.readValue(response1, UserResponseDTO.class);
        Long userId1 = createdUser1.getId();

        // Try to update user1 with user2's email
        UserRequestDTO updateRequest = new UserRequestDTO();
        updateRequest.setName("User1");
        updateRequest.setSurname("Test");
        updateRequest.setEmail(email2);

        // Act & Assert
        mockMvc
                .perform(
                        put("/api/users/{id}", userId1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(containsString("already exists")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // ========== Validation Exception Tests ==========

    @Test
    void createUser_ShouldReturnBadRequest_WhenNameMissing() throws Exception {
        // Arrange
        UserRequestDTO requestDTO = new UserRequestDTO();
        requestDTO.setSurname("Test");
        requestDTO.setEmail(generateUniqueEmail());

        // Act & Assert
        mockMvc
                .perform(
                        post("/api/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").exists());
    }

    @Test
    void createUser_ShouldReturnBadRequest_WhenSurnameMissing() throws Exception {
        // Arrange
        UserRequestDTO requestDTO = new UserRequestDTO();
        requestDTO.setName("Test");
        requestDTO.setEmail(generateUniqueEmail());

        // Act & Assert
        mockMvc
                .perform(
                        post("/api/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.surname").exists());
    }

    @Test
    void createUser_ShouldReturnBadRequest_WhenEmailInvalid() throws Exception {
        // Arrange
        UserRequestDTO requestDTO = new UserRequestDTO();
        requestDTO.setName("Test");
        requestDTO.setSurname("Test");
        requestDTO.setEmail("invalid-email");

        // Act & Assert
        mockMvc
                .perform(
                        post("/api/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").exists());
    }

    @Test
    void createUser_ShouldReturnBadRequest_WhenNameEmpty() throws Exception {
        // Arrange
        UserRequestDTO requestDTO = new UserRequestDTO();
        requestDTO.setName("");
        requestDTO.setSurname("Test");
        requestDTO.setEmail(generateUniqueEmail());

        // Act & Assert
        mockMvc
                .perform(
                        post("/api/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_ShouldReturnBadRequest_WhenBirthDateInFuture() throws Exception {
        // Arrange
        UserRequestDTO requestDTO = new UserRequestDTO();
        requestDTO.setName("Test");
        requestDTO.setSurname("Test");
        requestDTO.setEmail(generateUniqueEmail());
        requestDTO.setBirthDate(LocalDate.now().plusDays(1));

        // Act & Assert
        mockMvc
                .perform(
                        post("/api/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.birthDate").exists());
    }

    @Test
    void createUser_ShouldReturnBadRequest_WhenNameTooLong() throws Exception {
        // Arrange
        UserRequestDTO requestDTO = new UserRequestDTO();
        requestDTO.setName("A".repeat(256)); // Exceeds max length
        requestDTO.setSurname("Test");
        requestDTO.setEmail(generateUniqueEmail());

        // Act & Assert
        mockMvc
                .perform(
                        post("/api/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").exists());
    }

    @Test
    void createUser_ShouldReturnBadRequest_WhenEmailTooLong() throws Exception {
        // Arrange
        UserRequestDTO requestDTO = new UserRequestDTO();
        requestDTO.setName("Test");
        requestDTO.setSurname("Test");
        requestDTO.setEmail("a".repeat(250) + "@example.com"); // Exceeds max length

        // Act & Assert
        mockMvc
                .perform(
                        post("/api/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").exists());
    }

    @Test
    void updateUser_ShouldReturnBadRequest_WhenValidationFails() throws Exception {
        // Arrange - create a user first
        UserRequestDTO createRequest = new UserRequestDTO();
        createRequest.setName("Original");
        createRequest.setSurname("Name");
        createRequest.setEmail(generateUniqueEmail());

        String response =
                mockMvc
                        .perform(
                                post("/api/users")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(createRequest)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        UserResponseDTO createdUser = objectMapper.readValue(response, UserResponseDTO.class);
        Long userId = createdUser.getId();

        // Test missing name in update
        UserRequestDTO updateRequest = new UserRequestDTO();
        updateRequest.setSurname("Updated");
        updateRequest.setEmail(generateUniqueEmail());

        // Act & Assert
        mockMvc
                .perform(
                        put("/api/users/{id}", userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").exists());
    }

    // ========== Invalid Path Parameter Tests ==========

    @Test
    void getUserById_ShouldReturnBadRequest_WhenInvalidId() throws Exception {
        // Act & Assert - negative ID
        mockMvc.perform(get("/api/users/{id}", -1L)).andExpect(status().isBadRequest());

        // Act & Assert - zero ID
        mockMvc.perform(get("/api/users/{id}", 0L)).andExpect(status().isBadRequest());
    }

    @Test
    void updateUser_ShouldReturnBadRequest_WhenInvalidId() throws Exception {
        // Arrange
        UserRequestDTO requestDTO = new UserRequestDTO();
        requestDTO.setName("Test");
        requestDTO.setSurname("Test");
        requestDTO.setEmail(generateUniqueEmail());

        // Act & Assert - negative ID
        mockMvc
                .perform(
                        put("/api/users/{id}", -1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteUser_ShouldReturnBadRequest_WhenInvalidId() throws Exception {
        // Act & Assert - negative ID
        mockMvc.perform(delete("/api/users/{id}", -1L)).andExpect(status().isBadRequest());

        // Act & Assert - zero ID
        mockMvc.perform(delete("/api/users/{id}", 0L)).andExpect(status().isBadRequest());
    }

    @Test
    void activateUser_ShouldReturnBadRequest_WhenInvalidId() throws Exception {
        // Act & Assert - negative ID
        mockMvc.perform(patch("/api/users/{id}/activate", -1L)).andExpect(status().isBadRequest());
    }

    @Test
    void deactivateUser_ShouldReturnBadRequest_WhenInvalidId() throws Exception {
        // Act & Assert - negative ID
        mockMvc.perform(patch("/api/users/{id}/deactivate", -1L)).andExpect(status().isBadRequest());
    }

    @Test
    void getUserWithCardsById_ShouldReturnBadRequest_WhenInvalidId() throws Exception {
        // Act & Assert - negative ID
        mockMvc.perform(get("/api/users/{id}/with-cards", -1L)).andExpect(status().isBadRequest());
    }

    @Test
    void getUserCards_ShouldReturnBadRequest_WhenInvalidId() throws Exception {
        // Act & Assert - negative ID
        mockMvc.perform(get("/api/users/{userId}/cards", -1L)).andExpect(status().isBadRequest());
    }
}
