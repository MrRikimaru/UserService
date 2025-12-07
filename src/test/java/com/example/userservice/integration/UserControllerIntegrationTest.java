package com.example.userservice.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class UserControllerIntegrationTest extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private CacheService cacheService;

  @Autowired private UserRepository userRepository;

  @BeforeEach
  void setUp() {
    cacheService.evictAllUserCaches();
    userRepository.deleteAll();
  }

  private String generateUniqueEmail() {
    return "user." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
  }

  @Test
  void createUser_ShouldReturnCreatedUser_WhenValidInput() throws Exception {
    // Arrange
    UserRequestDTO requestDTO = new UserRequestDTO();
    requestDTO.setName("Integration");
    requestDTO.setSurname("Test");
    requestDTO.setEmail(generateUniqueEmail());
    requestDTO.setBirthDate(LocalDate.of(1990, 1, 1));

    // Act & Assert
    mockMvc
        .perform(
            post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.name").value("Integration"))
        .andExpect(jsonPath("$.surname").value("Test"))
        .andExpect(jsonPath("$.email").exists());
  }

  @Test
  void getUserById_ShouldReturnUser_WhenUserExists() throws Exception {
    // Arrange
    UserRequestDTO createRequest = new UserRequestDTO();
    createRequest.setName("Get");
    createRequest.setSurname("User");
    createRequest.setEmail(generateUniqueEmail());

    String createResponse =
        mockMvc
            .perform(
                post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    UserResponseDTO createdUser = objectMapper.readValue(createResponse, UserResponseDTO.class);
    Long userId = createdUser.getId();

    // Act & Assert
    mockMvc
        .perform(get("/api/users/{id}", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(userId))
        .andExpect(jsonPath("$.name").value("Get"))
        .andExpect(jsonPath("$.surname").value("User"));
  }

  @Test
  void updateUser_ShouldReturnUpdatedUser_WhenValidInput() throws Exception {
    // Arrange
    UserRequestDTO createRequest = new UserRequestDTO();
    createRequest.setName("Original");
    createRequest.setSurname("Name");
    createRequest.setEmail(generateUniqueEmail());

    String createResponse =
        mockMvc
            .perform(
                post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    UserResponseDTO createdUser = objectMapper.readValue(createResponse, UserResponseDTO.class);
    Long userId = createdUser.getId();

    UserRequestDTO updateRequest = new UserRequestDTO();
    updateRequest.setName("Updated");
    updateRequest.setSurname("Name");
    updateRequest.setEmail(generateUniqueEmail());

    // Act & Assert
    mockMvc
        .perform(
            put("/api/users/{id}", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(userId))
        .andExpect(jsonPath("$.name").value("Updated"));
  }

  @Test
  void activateUser_ShouldActivateUser() throws Exception {
    // Arrange
    UserRequestDTO createRequest = new UserRequestDTO();
    createRequest.setName("Activate");
    createRequest.setSurname("Test");
    createRequest.setEmail(generateUniqueEmail());
    createRequest.setActive(false);

    String createResponse =
        mockMvc
            .perform(
                post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    UserResponseDTO createdUser = objectMapper.readValue(createResponse, UserResponseDTO.class);
    Long userId = createdUser.getId();

    // Act & Assert
    mockMvc.perform(patch("/api/users/{id}/activate", userId)).andExpect(status().isOk());
  }

  @Test
  void getAllUsers_ShouldReturnPageOfUsers() throws Exception {
    // Arrange
    UserRequestDTO createRequest = new UserRequestDTO();
    createRequest.setName("Page");
    createRequest.setSurname("Test");
    createRequest.setEmail(generateUniqueEmail());

    mockMvc
        .perform(
            post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated());

    // Act & Assert
    mockMvc
        .perform(get("/api/users").param("page", "0").param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.totalElements").exists());
  }

  @Test
  void deactivateUser_ShouldDeactivateUser() throws Exception {
    // Arrange
    UserRequestDTO createRequest = new UserRequestDTO();
    createRequest.setName("Deactivate");
    createRequest.setSurname("Test");
    createRequest.setEmail(generateUniqueEmail());
    createRequest.setActive(true);

    String createResponse =
        mockMvc
            .perform(
                post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    UserResponseDTO createdUser = objectMapper.readValue(createResponse, UserResponseDTO.class);
    Long userId = createdUser.getId();

    // Act & Assert
    mockMvc.perform(patch("/api/users/{id}/deactivate", userId)).andExpect(status().isOk());
  }

  @Test
  void deleteUser_ShouldDeleteUser() throws Exception {
    // Arrange
    UserRequestDTO createRequest = new UserRequestDTO();
    createRequest.setName("Delete");
    createRequest.setSurname("Test");
    createRequest.setEmail(generateUniqueEmail());

    String createResponse =
        mockMvc
            .perform(
                post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    UserResponseDTO createdUser = objectMapper.readValue(createResponse, UserResponseDTO.class);
    Long userId = createdUser.getId();

    // Act & Assert
    mockMvc.perform(delete("/api/users/{id}", userId)).andExpect(status().isNoContent());

    // Verify user is deleted
    mockMvc.perform(get("/api/users/{id}", userId)).andExpect(status().isNotFound());
  }

  @Test
  void getUserWithCardsById_ShouldReturnUserWithCards() throws Exception {
    // Arrange
    UserRequestDTO createRequest = new UserRequestDTO();
    createRequest.setName("WithCards");
    createRequest.setSurname("Test");
    createRequest.setEmail(generateUniqueEmail());

    String createResponse =
        mockMvc
            .perform(
                post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    UserResponseDTO createdUser = objectMapper.readValue(createResponse, UserResponseDTO.class);
    Long userId = createdUser.getId();

    // Act & Assert
    mockMvc
        .perform(get("/api/users/{id}/with-cards", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(userId))
        .andExpect(jsonPath("$.paymentCards").isArray());
  }

  @Test
  void getUserCards_ShouldReturnUserCards() throws Exception {
    // Arrange
    UserRequestDTO createRequest = new UserRequestDTO();
    createRequest.setName("Cards");
    createRequest.setSurname("Test");
    createRequest.setEmail(generateUniqueEmail());

    String createResponse =
        mockMvc
            .perform(
                post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    UserResponseDTO createdUser = objectMapper.readValue(createResponse, UserResponseDTO.class);
    Long userId = createdUser.getId();

    // Act & Assert
    mockMvc
        .perform(get("/api/users/{userId}/cards", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
  }
}
