package com.example.userservice.integration;

import com.example.userservice.dto.UserRequestDTO;
import com.example.userservice.dto.UserResponseDTO;
import com.example.userservice.service.CacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CacheService cacheService;

    @BeforeEach
    void setUp() {
        // Очищаем кэш перед каждым тестом
        cacheService.evictAllUserCaches();
    }

    private String generateUniqueEmail() {
        return "user." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    @Test
    @Order(1)
    void createUser_ShouldReturnCreatedUser_WhenValidInput() throws Exception {
        // Arrange
        UserRequestDTO requestDTO = new UserRequestDTO();
        requestDTO.setName("Integration");
        requestDTO.setSurname("Test");
        requestDTO.setEmail(generateUniqueEmail());
        requestDTO.setBirthDate(LocalDate.of(1990, 1, 1));

        // Act
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/users", requestDTO, String.class);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());

        UserResponseDTO responseBody = objectMapper.readValue(response.getBody(), UserResponseDTO.class);
        assertNotNull(responseBody);
        assertEquals("Integration", responseBody.getName());
        assertEquals("Test", responseBody.getSurname());
    }

    @Test
    @Order(2)
    void getUserById_ShouldReturnUser_WhenUserExists() throws Exception {
        // Arrange - сначала создаем пользователя
        UserRequestDTO createRequest = new UserRequestDTO();
        createRequest.setName("Get");
        createRequest.setSurname("User");
        createRequest.setEmail(generateUniqueEmail());

        ResponseEntity<String> createResponse = restTemplate.postForEntity(
                "/api/users", createRequest, String.class);
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());

        UserResponseDTO createdUser = objectMapper.readValue(createResponse.getBody(), UserResponseDTO.class);
        Long userId = createdUser.getId();

        // Act
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/users/" + userId, String.class);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        UserResponseDTO responseBody = objectMapper.readValue(response.getBody(), UserResponseDTO.class);
        assertNotNull(responseBody);
        assertEquals(userId, responseBody.getId());
        assertEquals("Get", responseBody.getName());
    }

    @Test
    @Order(3)
    void getUserById_ShouldReturnNotFound_WhenUserNotExists() {
        // Act
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/users/999", String.class);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @Order(4)
    void updateUser_ShouldReturnUpdatedUser_WhenValidInput() throws Exception {
        // Arrange - создаем пользователя
        UserRequestDTO createRequest = new UserRequestDTO();
        createRequest.setName("Original");
        createRequest.setSurname("Name");
        createRequest.setEmail(generateUniqueEmail());

        ResponseEntity<String> createResponse = restTemplate.postForEntity(
                "/api/users", createRequest, String.class);
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());

        UserResponseDTO createdUser = objectMapper.readValue(createResponse.getBody(), UserResponseDTO.class);
        Long userId = createdUser.getId();

        // Подготавливаем данные для обновления
        UserRequestDTO updateRequest = new UserRequestDTO();
        updateRequest.setName("Updated");
        updateRequest.setSurname("Name");
        updateRequest.setEmail(generateUniqueEmail());

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/users/" + userId,
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest),
                String.class);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        UserResponseDTO responseBody = objectMapper.readValue(response.getBody(), UserResponseDTO.class);
        assertNotNull(responseBody);
        assertEquals("Updated", responseBody.getName());
    }

    @Test
    @Order(5)
    void activateUser_ShouldActivateUser() throws Exception {
        // Arrange - создаем неактивного пользователя
        UserRequestDTO createRequest = new UserRequestDTO();
        createRequest.setName("Activate");
        createRequest.setSurname("Test");
        createRequest.setEmail(generateUniqueEmail());
        createRequest.setActive(false);

        ResponseEntity<String> createResponse = restTemplate.postForEntity(
                "/api/users", createRequest, String.class);
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());

        UserResponseDTO createdUser = objectMapper.readValue(createResponse.getBody(), UserResponseDTO.class);
        Long userId = createdUser.getId();

        // Act
        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/users/" + userId + "/activate",
                HttpMethod.PATCH,
                null,
                Void.class);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @Order(6)
    void getAllUsers_ShouldReturnPageOfUsers() throws Exception {
        // Arrange - создаем тестового пользователя
        UserRequestDTO createRequest = new UserRequestDTO();
        createRequest.setName("Page");
        createRequest.setSurname("Test");
        createRequest.setEmail(generateUniqueEmail());

        ResponseEntity<String> createResponse = restTemplate.postForEntity(
                "/api/users", createRequest, String.class);
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());

        // Act
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/users?page=0&size=10", String.class);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}