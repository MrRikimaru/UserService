package com.example.userservice.service;

import com.example.userservice.dto.PaymentCardResponseDTO;
import com.example.userservice.dto.UserRequestDTO;
import com.example.userservice.dto.UserResponseDTO;
import com.example.userservice.dto.UserWithCardsResponseDTO;
import com.example.userservice.entity.User;
import com.example.userservice.exception.DuplicateEmailException;
import com.example.userservice.exception.UserNotFoundException;
import com.example.userservice.mapper.PaymentCardMapper;
import com.example.userservice.mapper.UserMapper;
import com.example.userservice.repository.PaymentCardRepository;
import com.example.userservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentCardRepository paymentCardRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PaymentCardMapper paymentCardMapper;

    @InjectMocks
    private UserService userService;

    @Test
    void createUser_ShouldReturnUserResponseDTO_WhenValidInput() {
        // Arrange
        UserRequestDTO requestDTO = new UserRequestDTO();
        requestDTO.setEmail("test@example.com");
        requestDTO.setName("John");
        requestDTO.setSurname("Doe");

        User user = new User();
        user.setId(1L);
        UserResponseDTO responseDTO = new UserResponseDTO();
        responseDTO.setId(1L);

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userMapper.toEntity(any(UserRequestDTO.class))).thenReturn(user);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDTO(any(User.class))).thenReturn(responseDTO);

        // Act
        UserResponseDTO result = userService.createUser(requestDTO);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository).save(user);
    }

    @Test
    void createUser_ShouldThrowDuplicateEmailException_WhenEmailExists() {
        // Arrange
        UserRequestDTO requestDTO = new UserRequestDTO();
        requestDTO.setEmail("existing@example.com");

        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateEmailException.class, () -> userService.createUser(requestDTO));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getUserById_ShouldReturnUserResponseDTO_WhenUserExists() {
        // Arrange
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        UserResponseDTO responseDTO = new UserResponseDTO();
        responseDTO.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toDTO(any(User.class))).thenReturn(responseDTO);

        // Act
        UserResponseDTO result = userService.getUserById(userId);

        // Assert
        assertNotNull(result);
        assertEquals(userId, result.getId());
        verify(userRepository).findById(userId);
    }

    @Test
    void getUserById_ShouldThrowUserNotFoundException_WhenUserNotExists() {
        // Arrange
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> userService.getUserById(userId));
    }

    @Test
    void getUserWithCardsById_ShouldReturnUserWithCards_WhenUserExists() {
        // Arrange
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setName("John");
        user.setSurname("Doe");
        user.setBirthDate(LocalDate.of(1990, 1, 1));
        user.setEmail("john.doe@example.com");
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(paymentCardRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
        // Убрал ненужное заглушение для paymentCardMapper.toDTO

        // Act
        UserWithCardsResponseDTO result = userService.getUserWithCardsById(userId);

        // Assert
        assertNotNull(result);
        assertEquals(userId, result.getId());
        assertEquals("John", result.getName());
        assertEquals("Doe", result.getSurname());
        assertEquals("john.doe@example.com", result.getEmail());
        assertNotNull(result.getPaymentCards());
        assertTrue(result.getPaymentCards().isEmpty());

        verify(userRepository).findById(userId);
        verify(paymentCardRepository).findByUserId(userId);
        // Проверяем, что mapper не вызывался, так как список карт пустой
        verify(paymentCardMapper, never()).toDTO(any());
    }

    @Test
    void getAllUsers_ShouldReturnPageOfUsers() {
        // Arrange
        Pageable pageable = Pageable.unpaged();
        User user = new User();
        UserResponseDTO responseDTO = new UserResponseDTO();
        Page<User> userPage = new PageImpl<>(List.of(user));

        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(userPage);
        when(userMapper.toDTO(any(User.class))).thenReturn(responseDTO);

        // Act
        Page<UserResponseDTO> result = userService.getAllUsers("John", "Doe", true, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(userRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void updateUser_ShouldReturnUpdatedUser_WhenUserExists() {
        // Arrange
        Long userId = 1L;
        UserRequestDTO requestDTO = new UserRequestDTO();
        requestDTO.setName("Updated");
        requestDTO.setSurname("Name");
        requestDTO.setEmail("updated@example.com");

        User existingUser = new User();
        existingUser.setId(userId);
        User updatedUser = new User();
        updatedUser.setId(userId);
        updatedUser.setName("Updated");
        UserResponseDTO responseDTO = new UserResponseDTO();
        responseDTO.setId(userId);
        responseDTO.setName("Updated");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        when(userMapper.toDTO(any(User.class))).thenReturn(responseDTO);

        // Act
        UserResponseDTO result = userService.updateUser(userId, requestDTO);

        // Assert
        assertNotNull(result);
        assertEquals("Updated", result.getName());
        verify(userRepository).findById(userId);
        verify(userRepository).save(existingUser);
    }

    @Test
    void activateUser_ShouldCallRepository() {
        // Arrange
        Long userId = 1L;

        // Act
        userService.activateUser(userId);

        // Assert
        verify(userRepository).updateActiveStatus(userId, true);
    }

    @Test
    void deactivateUser_ShouldCallRepository() {
        // Arrange
        Long userId = 1L;

        // Act
        userService.deactivateUser(userId);

        // Assert
        verify(userRepository).updateActiveStatus(userId, false);
    }

    @Test
    void getUserWithCardsById_ShouldReturnUserWithCardsList_WhenUserHasCards() {
        // Arrange
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setName("John");
        user.setSurname("Doe");
        user.setBirthDate(LocalDate.of(1990, 1, 1));
        user.setEmail("john.doe@example.com");
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        // Создаем реальную карту для entity
        com.example.userservice.entity.PaymentCard card = new com.example.userservice.entity.PaymentCard();
        card.setId(1L);
        card.setNumber("1234567890123456");
        card.setHolder("John Doe");

        PaymentCardResponseDTO cardResponseDTO = new PaymentCardResponseDTO();
        cardResponseDTO.setId(1L);
        cardResponseDTO.setNumber("1234567890123456");
        cardResponseDTO.setHolder("John Doe");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(paymentCardRepository.findByUserId(userId)).thenReturn(List.of(card));
        when(paymentCardMapper.toDTO(card)).thenReturn(cardResponseDTO);

        // Act
        UserWithCardsResponseDTO result = userService.getUserWithCardsById(userId);

        // Assert
        assertNotNull(result);
        assertEquals(userId, result.getId());
        assertEquals("John", result.getName());
        assertEquals("Doe", result.getSurname());
        assertNotNull(result.getPaymentCards());
        assertEquals(1, result.getPaymentCards().size());
        assertEquals("1234567890123456", result.getPaymentCards().get(0).getNumber());
        assertEquals("John Doe", result.getPaymentCards().get(0).getHolder());

        verify(userRepository).findById(userId);
        verify(paymentCardRepository).findByUserId(userId);
        verify(paymentCardMapper).toDTO(card);
    }

    @Test
    void getUserWithCardsById_ShouldThrowUserNotFoundException_WhenUserNotExists() {
        // Arrange
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> userService.getUserWithCardsById(userId));
        verify(userRepository).findById(userId);
        verify(paymentCardRepository, never()).findByUserId(anyLong());
    }

    @Test
    void getActiveUsersBornBefore_ShouldReturnFilteredUsers() {
        // Arrange
        Pageable pageable = Pageable.unpaged();
        LocalDate birthDate = LocalDate.of(2000, 1, 1);
        User user = new User();
        UserResponseDTO responseDTO = new UserResponseDTO();
        Page<User> userPage = new PageImpl<>(List.of(user));

        when(userRepository.findActiveUsersBornBefore(any(LocalDate.class), eq(true), any(Pageable.class)))
                .thenReturn(userPage);
        when(userMapper.toDTO(any(User.class))).thenReturn(responseDTO);

        // Act
        Page<UserResponseDTO> result = userService.getActiveUsersBornBefore(birthDate, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(userRepository).findActiveUsersBornBefore(birthDate, true, pageable);
    }
}