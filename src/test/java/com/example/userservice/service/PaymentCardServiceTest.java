package com.example.userservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.userservice.dto.PaymentCardRequestDTO;
import com.example.userservice.dto.PaymentCardResponseDTO;
import com.example.userservice.entity.PaymentCard;
import com.example.userservice.entity.User;
import com.example.userservice.exception.CardLimitExceededException;
import com.example.userservice.exception.DuplicateCardNumberException;
import com.example.userservice.exception.PaymentCardNotFoundException;
import com.example.userservice.mapper.PaymentCardMapper;
import com.example.userservice.repository.PaymentCardRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class PaymentCardServiceTest {

  @Mock private PaymentCardRepository paymentCardRepository;

  @Mock private UserService userService;

  @Mock private PaymentCardMapper paymentCardMapper;

  @Mock private CacheService cacheService;

  @InjectMocks private PaymentCardService paymentCardService;

  private User testUser;
  private PaymentCard testCard;
  private PaymentCardResponseDTO testCardResponseDTO;
  private PaymentCardRequestDTO testCardRequestDTO;

  @BeforeEach
  void setUp() {
    // Initialize test user
    testUser = new User();
    testUser.setId(1L);
    testUser.setName("Test");
    testUser.setSurname("User");
    testUser.setEmail("test@example.com");
    testUser.setActive(true);
    testUser.setPaymentCards(new ArrayList<>());

    // Initialize test card
    testCard = new PaymentCard();
    testCard.setId(1L);
    testCard.setUser(testUser);
    testCard.setNumber("4111111111111111");
    testCard.setHolder("TEST HOLDER");
    testCard.setExpirationDate(LocalDate.now().plusYears(2));
    testCard.setActive(true);

    // Initialize test DTOs
    testCardResponseDTO = new PaymentCardResponseDTO();
    testCardResponseDTO.setId(1L);
    testCardResponseDTO.setUserId(1L);
    testCardResponseDTO.setNumber("4111111111111111");
    testCardResponseDTO.setHolder("TEST HOLDER");
    testCardResponseDTO.setExpirationDate(LocalDate.now().plusYears(2));
    testCardResponseDTO.setActive(true);

    testCardRequestDTO = new PaymentCardRequestDTO();
    testCardRequestDTO.setNumber("4111111111111111");
    testCardRequestDTO.setHolder("TEST HOLDER");
    testCardRequestDTO.setExpirationDate(LocalDate.now().plusYears(2));
    testCardRequestDTO.setActive(true);
  }

  @Test
  void createCard_ShouldReturnPaymentCardResponseDTO_WhenValidInput() {
    // Arrange
    Long userId = 1L;

    when(userService.getUserEntityById(userId)).thenReturn(testUser);
    when(paymentCardRepository.countCardsByUserId(userId)).thenReturn(3);
    when(paymentCardMapper.toEntity(any(PaymentCardRequestDTO.class))).thenReturn(testCard);
    when(paymentCardRepository.save(any(PaymentCard.class))).thenReturn(testCard);
    when(paymentCardMapper.toDTO(any(PaymentCard.class))).thenReturn(testCardResponseDTO);

    // Act
    PaymentCardResponseDTO result = paymentCardService.createCard(testCardRequestDTO, userId);

    // Assert
    assertNotNull(result);
    assertEquals(1L, result.getId());
    verify(userService).getUserEntityById(userId);
    verify(paymentCardRepository).countCardsByUserId(userId);
    verify(paymentCardRepository).save(testCard);
  }

  @Test
  void createCard_ShouldThrowCardLimitExceededException_WhenUserHas5Cards() {
    // Arrange
    Long userId = 1L;

    when(userService.getUserEntityById(userId)).thenReturn(testUser);
    when(paymentCardRepository.countCardsByUserId(userId)).thenReturn(5);

    // Act & Assert
    assertThrows(
        CardLimitExceededException.class,
        () -> paymentCardService.createCard(testCardRequestDTO, userId));
    verify(paymentCardRepository, never()).save(any(PaymentCard.class));
  }

  @Test
  void getCardById_ShouldReturnPaymentCardResponseDTO_WhenCardExists() {
    // Arrange
    Long cardId = 1L;

    when(paymentCardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
    when(paymentCardMapper.toDTO(any(PaymentCard.class))).thenReturn(testCardResponseDTO);

    // Act
    PaymentCardResponseDTO result = paymentCardService.getCardById(cardId);

    // Assert
    assertNotNull(result);
    assertEquals(cardId, result.getId());
    verify(paymentCardRepository).findById(cardId);
  }

  @Test
  void getCardById_ShouldThrowPaymentCardNotFoundException_WhenCardNotExists() {
    // Arrange
    Long cardId = 999L;
    when(paymentCardRepository.findById(cardId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(PaymentCardNotFoundException.class, () -> paymentCardService.getCardById(cardId));
  }

  @Test
  @SuppressWarnings("unchecked")
  void getAllCards_ShouldReturnPageOfCards() {
    // Arrange
    Pageable pageable = Pageable.unpaged();
    Page<PaymentCard> cardPage = new PageImpl<>(List.of(testCard));

    when(paymentCardRepository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(cardPage);
    when(paymentCardMapper.toDTO(any(PaymentCard.class))).thenReturn(testCardResponseDTO);

    // Act
    Page<PaymentCardResponseDTO> result =
        paymentCardService.getAllCards("TEST", true, 1L, pageable);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
    verify(paymentCardRepository).findAll(any(Specification.class), eq(pageable));
  }

  @Test
  void updateCard_ShouldReturnUpdatedCard_WhenCardExists() {
    // Arrange
    Long cardId = 1L;
    PaymentCardRequestDTO updateRequest = new PaymentCardRequestDTO();
    updateRequest.setNumber("5111111111111111");
    updateRequest.setHolder("UPDATED HOLDER");
    updateRequest.setExpirationDate(LocalDate.now().plusYears(3));

    PaymentCard updatedCard = new PaymentCard();
    updatedCard.setId(cardId);
    updatedCard.setUser(testUser);
    updatedCard.setNumber("5111111111111111");
    updatedCard.setHolder("UPDATED HOLDER");
    updatedCard.setExpirationDate(LocalDate.now().plusYears(3));
    updatedCard.setActive(true);

    PaymentCardResponseDTO updatedResponseDTO = new PaymentCardResponseDTO();
    updatedResponseDTO.setId(cardId);
    updatedResponseDTO.setUserId(1L);
    updatedResponseDTO.setNumber("5111111111111111");
    updatedResponseDTO.setHolder("UPDATED HOLDER");
    updatedResponseDTO.setExpirationDate(LocalDate.now().plusYears(3));
    updatedResponseDTO.setActive(true);

    when(paymentCardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
    when(paymentCardRepository.findByNumber(anyString())).thenReturn(Optional.empty());
    when(paymentCardRepository.save(any(PaymentCard.class))).thenReturn(updatedCard);
    when(paymentCardMapper.toDTO(any(PaymentCard.class))).thenReturn(updatedResponseDTO);

    // Act
    PaymentCardResponseDTO result = paymentCardService.updateCard(cardId, updateRequest);

    // Assert
    assertNotNull(result);
    assertEquals("UPDATED HOLDER", result.getHolder());
    verify(paymentCardRepository).findById(cardId);
    verify(paymentCardRepository).save(any(PaymentCard.class));
  }

  @Test
  void updateCard_ShouldThrowDuplicateCardNumberException_WhenNumberExists() {
    // Arrange
    Long cardId = 1L;
    PaymentCardRequestDTO updateRequest = new PaymentCardRequestDTO();
    updateRequest.setNumber("EXISTING_NUMBER");
    updateRequest.setHolder("TEST HOLDER");
    updateRequest.setExpirationDate(LocalDate.now().plusYears(2));

    PaymentCard existingCardWithSameNumber = new PaymentCard();
    existingCardWithSameNumber.setId(2L); // Different ID
    existingCardWithSameNumber.setNumber("EXISTING_NUMBER");

    when(paymentCardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
    when(paymentCardRepository.findByNumber("EXISTING_NUMBER"))
        .thenReturn(Optional.of(existingCardWithSameNumber));

    // Act & Assert
    assertThrows(
        DuplicateCardNumberException.class,
        () -> paymentCardService.updateCard(cardId, updateRequest));
    verify(paymentCardRepository, never()).save(any(PaymentCard.class));
  }

  @Test
  void activateCard_ShouldUpdateStatusAndEvictCache() {
    // Arrange
    Long cardId = 1L;

    when(paymentCardRepository.findById(cardId)).thenReturn(Optional.of(testCard));

    // Act
    paymentCardService.activateCard(cardId);

    // Assert
    verify(paymentCardRepository).updateActiveStatus(cardId, true);
    verify(cacheService).evictUserCaches(testUser.getId());
  }

  @Test
  void getCardByUserAndId_ShouldReturnCard_WhenCardExistsForUser() {
    // Arrange
    Long userId = 1L;
    Long cardId = 1L;

    when(paymentCardRepository.findByIdAndUserId(cardId, userId)).thenReturn(Optional.of(testCard));
    when(paymentCardMapper.toDTO(any(PaymentCard.class))).thenReturn(testCardResponseDTO);

    // Act
    PaymentCardResponseDTO result = paymentCardService.getCardByUserAndId(userId, cardId);

    // Assert
    assertNotNull(result);
    assertEquals(cardId, result.getId());
    verify(paymentCardRepository).findByIdAndUserId(cardId, userId);
  }

  @Test
  void deactivateCard_ShouldUpdateStatusAndEvictCache() {
    // Arrange
    Long cardId = 1L;

    when(paymentCardRepository.findById(cardId)).thenReturn(Optional.of(testCard));

    // Act
    paymentCardService.deactivateCard(cardId);

    // Assert
    verify(paymentCardRepository).updateActiveStatus(cardId, false);
    verify(cacheService).evictUserCaches(testUser.getId());
  }

  @Test
  void deleteCard_ShouldDeleteCardAndEvictCache() {
    // Arrange
    Long cardId = 1L;

    when(paymentCardRepository.findById(cardId)).thenReturn(Optional.of(testCard));

    // Act
    paymentCardService.deleteCard(cardId);

    // Assert
    verify(paymentCardRepository).deleteById(cardId);
    verify(cacheService).evictUserCaches(testUser.getId());
  }

  @Test
  void deleteCard_ShouldThrowPaymentCardNotFoundException_WhenCardNotExists() {
    // Arrange
    Long cardId = 999L;
    when(paymentCardRepository.findById(cardId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(PaymentCardNotFoundException.class, () -> paymentCardService.deleteCard(cardId));
    verify(paymentCardRepository, never()).deleteById(anyLong());
    verify(cacheService, never()).evictUserCaches(anyLong());
  }

  @Test
  void getCardByNumber_ShouldReturnCard_WhenCardExists() {
    // Arrange
    String cardNumber = "4111111111111111";

    when(paymentCardRepository.findByNumber(cardNumber)).thenReturn(Optional.of(testCard));
    when(paymentCardMapper.toDTO(any(PaymentCard.class))).thenReturn(testCardResponseDTO);

    // Act
    PaymentCardResponseDTO result = paymentCardService.getCardByNumber(cardNumber);

    // Assert
    assertNotNull(result);
    assertEquals(cardNumber, result.getNumber());
    verify(paymentCardRepository).findByNumber(cardNumber);
  }

  @Test
  void getCardByNumber_ShouldThrowPaymentCardNotFoundException_WhenCardNotExists() {
    // Arrange
    String cardNumber = "9999999999999999";
    when(paymentCardRepository.findByNumber(cardNumber)).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(
        PaymentCardNotFoundException.class, () -> paymentCardService.getCardByNumber(cardNumber));
  }

  @Test
  void updateCard_ShouldThrowIllegalArgumentException_WhenExpirationDateInPast() {
    // Arrange
    Long cardId = 1L;
    PaymentCardRequestDTO invalidRequest = new PaymentCardRequestDTO();
    invalidRequest.setNumber("4111111111111111");
    invalidRequest.setHolder("TEST HOLDER");
    invalidRequest.setExpirationDate(LocalDate.now().minusDays(1)); // Past date

    when(paymentCardRepository.findById(cardId)).thenReturn(Optional.of(testCard));

    // Act & Assert
    assertThrows(
        IllegalArgumentException.class,
        () -> paymentCardService.updateCard(cardId, invalidRequest));
    verify(paymentCardRepository, never()).save(any(PaymentCard.class));
  }

  @Test
  void getAllCardsByUserId_ShouldReturnPageOfCards() {
    // Arrange
    Long userId = 1L;
    Pageable pageable = Pageable.unpaged();
    Page<PaymentCard> cardPage = new PageImpl<>(List.of(testCard));

    when(paymentCardRepository.findByUserId(userId, pageable)).thenReturn(cardPage);
    when(paymentCardMapper.toDTO(any(PaymentCard.class))).thenReturn(testCardResponseDTO);

    // Act
    Page<PaymentCardResponseDTO> result = paymentCardService.getAllCardsByUserId(userId, pageable);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
    verify(paymentCardRepository).findByUserId(userId, pageable);
  }

  @Test
  void getActiveCardsByUserId_ShouldReturnPageOfActiveCards() {
    // Arrange
    Long userId = 1L;
    Pageable pageable = Pageable.unpaged();
    Page<PaymentCard> cardPage = new PageImpl<>(List.of(testCard));

    when(paymentCardRepository.findByUserIdAndActiveStatus(userId, true, pageable))
        .thenReturn(cardPage);
    when(paymentCardMapper.toDTO(any(PaymentCard.class))).thenReturn(testCardResponseDTO);

    // Act
    Page<PaymentCardResponseDTO> result =
        paymentCardService.getActiveCardsByUserId(userId, pageable);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
    verify(paymentCardRepository).findByUserIdAndActiveStatus(userId, true, pageable);
  }

  @Test
  void getActiveCards_ShouldReturnPageOfActiveCards() {
    // Arrange
    Pageable pageable = Pageable.unpaged();
    Page<PaymentCard> cardPage = new PageImpl<>(List.of(testCard));

    when(paymentCardRepository.findByActiveTrue(pageable)).thenReturn(cardPage);
    when(paymentCardMapper.toDTO(any(PaymentCard.class))).thenReturn(testCardResponseDTO);

    // Act
    Page<PaymentCardResponseDTO> result = paymentCardService.getActiveCards(pageable);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
    verify(paymentCardRepository).findByActiveTrue(pageable);
  }

  @Test
  void updateCard_ShouldNotThrowException_WhenNumberNotChanged() {
    // Arrange
    Long cardId = 1L;
    PaymentCardRequestDTO updateRequest = new PaymentCardRequestDTO();
    updateRequest.setNumber("4111111111111111"); // Same number as original
    updateRequest.setHolder("UPDATED HOLDER");
    updateRequest.setExpirationDate(LocalDate.now().plusYears(3));

    PaymentCard updatedCard = new PaymentCard();
    updatedCard.setId(cardId);
    updatedCard.setUser(testUser);
    updatedCard.setNumber("4111111111111111");
    updatedCard.setHolder("UPDATED HOLDER");
    updatedCard.setExpirationDate(LocalDate.now().plusYears(3));
    updatedCard.setActive(true);

    PaymentCardResponseDTO updatedResponseDTO = new PaymentCardResponseDTO();
    updatedResponseDTO.setId(cardId);
    updatedResponseDTO.setUserId(1L);
    updatedResponseDTO.setNumber("4111111111111111");
    updatedResponseDTO.setHolder("UPDATED HOLDER");
    updatedResponseDTO.setExpirationDate(LocalDate.now().plusYears(3));
    updatedResponseDTO.setActive(true);

    when(paymentCardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
    when(paymentCardRepository.save(any(PaymentCard.class))).thenReturn(updatedCard);
    when(paymentCardMapper.toDTO(any(PaymentCard.class))).thenReturn(updatedResponseDTO);

    // Act
    PaymentCardResponseDTO result = paymentCardService.updateCard(cardId, updateRequest);

    // Assert
    assertNotNull(result);
    assertEquals("UPDATED HOLDER", result.getHolder());
    // Should not check for duplicates when number doesn't change
    verify(paymentCardRepository, never()).findByNumber(anyString());
    verify(paymentCardRepository).save(any(PaymentCard.class));
  }
}
