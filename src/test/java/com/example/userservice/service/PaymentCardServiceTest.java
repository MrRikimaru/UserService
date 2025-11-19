package com.example.userservice.service;

import com.example.userservice.dto.PaymentCardRequestDTO;
import com.example.userservice.dto.PaymentCardResponseDTO;
import com.example.userservice.entity.PaymentCard;
import com.example.userservice.entity.User;
import com.example.userservice.exception.CardLimitExceededException;
import com.example.userservice.exception.DuplicateCardNumberException;
import com.example.userservice.exception.PaymentCardNotFoundException;
import com.example.userservice.mapper.PaymentCardMapper;
import com.example.userservice.repository.PaymentCardRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentCardServiceTest {

    @Mock
    private PaymentCardRepository paymentCardRepository;

    @Mock
    private UserService userService;

    @Mock
    private PaymentCardMapper paymentCardMapper;

    @Mock
    private CacheService cacheService;

    @InjectMocks
    private PaymentCardService paymentCardService;

    @Test
    void createCard_ShouldReturnPaymentCardResponseDTO_WhenValidInput() {
        // Arrange
        Long userId = 1L;
        PaymentCardRequestDTO requestDTO = new PaymentCardRequestDTO();
        requestDTO.setNumber("1234567890123456");
        requestDTO.setHolder("John Doe");
        requestDTO.setExpirationDate(LocalDate.now().plusYears(2));

        User user = new User();
        user.setId(userId);
        PaymentCard card = new PaymentCard();
        card.setId(1L);
        PaymentCardResponseDTO responseDTO = new PaymentCardResponseDTO();
        responseDTO.setId(1L);

        when(userService.getUserEntityById(userId)).thenReturn(user);
        when(paymentCardRepository.countCardsByUserId(userId)).thenReturn(3);
        when(paymentCardMapper.toEntity(any(PaymentCardRequestDTO.class))).thenReturn(card);
        when(paymentCardRepository.save(any(PaymentCard.class))).thenReturn(card);
        when(paymentCardMapper.toDTO(any(PaymentCard.class))).thenReturn(responseDTO);

        // Act
        PaymentCardResponseDTO result = paymentCardService.createCard(requestDTO, userId);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(userService).getUserEntityById(userId);
        verify(paymentCardRepository).countCardsByUserId(userId);
        verify(paymentCardRepository).save(card);
    }

    @Test
    void createCard_ShouldThrowCardLimitExceededException_WhenUserHas5Cards() {
        // Arrange
        Long userId = 1L;
        PaymentCardRequestDTO requestDTO = new PaymentCardRequestDTO();
        User user = new User();
        user.setId(userId);

        when(userService.getUserEntityById(userId)).thenReturn(user);
        when(paymentCardRepository.countCardsByUserId(userId)).thenReturn(5);

        // Act & Assert
        assertThrows(CardLimitExceededException.class,
                () -> paymentCardService.createCard(requestDTO, userId));
        verify(paymentCardRepository, never()).save(any(PaymentCard.class));
    }

    @Test
    void getCardById_ShouldReturnPaymentCardResponseDTO_WhenCardExists() {
        // Arrange
        Long cardId = 1L;
        PaymentCard card = new PaymentCard();
        card.setId(cardId);
        PaymentCardResponseDTO responseDTO = new PaymentCardResponseDTO();
        responseDTO.setId(cardId);

        when(paymentCardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(paymentCardMapper.toDTO(any(PaymentCard.class))).thenReturn(responseDTO);

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
        assertThrows(PaymentCardNotFoundException.class,
                () -> paymentCardService.getCardById(cardId));
    }

    @Test
    void getAllCards_ShouldReturnPageOfCards() {
        // Arrange
        Pageable pageable = Pageable.unpaged();
        PaymentCard card = new PaymentCard();
        PaymentCardResponseDTO responseDTO = new PaymentCardResponseDTO();
        Page<PaymentCard> cardPage = new PageImpl<>(List.of(card));

        when(paymentCardRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(cardPage);
        when(paymentCardMapper.toDTO(any(PaymentCard.class))).thenReturn(responseDTO);

        // Act
        Page<PaymentCardResponseDTO> result = paymentCardService.getAllCards("John", true, 1L, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(paymentCardRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void updateCard_ShouldReturnUpdatedCard_WhenCardExists() {
        // Arrange
        Long cardId = 1L;
        PaymentCardRequestDTO requestDTO = new PaymentCardRequestDTO();
        requestDTO.setNumber("1234567890123456");
        requestDTO.setHolder("Updated Holder");

        PaymentCard existingCard = new PaymentCard();
        existingCard.setId(cardId);
        existingCard.setNumber("old-number");
        User user = new User();
        user.setId(1L);
        existingCard.setUser(user);

        PaymentCard updatedCard = new PaymentCard();
        updatedCard.setId(cardId);
        PaymentCardResponseDTO responseDTO = new PaymentCardResponseDTO();
        responseDTO.setId(cardId);

        when(paymentCardRepository.findById(cardId)).thenReturn(Optional.of(existingCard));
        when(paymentCardRepository.findByNumber(anyString())).thenReturn(Optional.empty());
        when(paymentCardRepository.save(any(PaymentCard.class))).thenReturn(updatedCard);
        when(paymentCardMapper.toDTO(any(PaymentCard.class))).thenReturn(responseDTO);

        // Act
        PaymentCardResponseDTO result = paymentCardService.updateCard(cardId, requestDTO);

        // Assert
        assertNotNull(result);
        assertEquals(cardId, result.getId());
        verify(paymentCardRepository).findById(cardId);
        verify(paymentCardRepository).save(existingCard);
    }

    @Test
    void updateCard_ShouldThrowDuplicateCardNumberException_WhenNumberExists() {
        // Arrange
        Long cardId = 1L;
        PaymentCardRequestDTO requestDTO = new PaymentCardRequestDTO();
        requestDTO.setNumber("existing-number");

        PaymentCard existingCard = new PaymentCard();
        existingCard.setId(cardId);
        existingCard.setNumber("old-number");

        PaymentCard duplicateCard = new PaymentCard();
        duplicateCard.setId(2L); // different ID

        when(paymentCardRepository.findById(cardId)).thenReturn(Optional.of(existingCard));
        when(paymentCardRepository.findByNumber("existing-number")).thenReturn(Optional.of(duplicateCard));

        // Act & Assert
        assertThrows(DuplicateCardNumberException.class,
                () -> paymentCardService.updateCard(cardId, requestDTO));
        verify(paymentCardRepository, never()).save(any(PaymentCard.class));
    }

    @Test
    void activateCard_ShouldUpdateStatusAndEvictCache() {
        // Arrange
        Long cardId = 1L;
        PaymentCard card = new PaymentCard();
        User user = new User();
        user.setId(1L);
        card.setUser(user);

        when(paymentCardRepository.findById(cardId)).thenReturn(Optional.of(card));

        // Act
        paymentCardService.activateCard(cardId);

        // Assert
        verify(paymentCardRepository).updateActiveStatus(cardId, true);
        verify(cacheService).evictUserCaches(1L);
    }

    @Test
    void getCardByUserAndId_ShouldReturnCard_WhenCardExistsForUser() {
        // Arrange
        Long userId = 1L;
        Long cardId = 1L;
        PaymentCard card = new PaymentCard();
        card.setId(cardId);
        PaymentCardResponseDTO responseDTO = new PaymentCardResponseDTO();
        responseDTO.setId(cardId);

        when(paymentCardRepository.findByIdAndUserId(cardId, userId)).thenReturn(Optional.of(card));
        when(paymentCardMapper.toDTO(any(PaymentCard.class))).thenReturn(responseDTO);

        // Act
        PaymentCardResponseDTO result = paymentCardService.getCardByUserAndId(userId, cardId);

        // Assert
        assertNotNull(result);
        assertEquals(cardId, result.getId());
        verify(paymentCardRepository).findByIdAndUserId(cardId, userId);
    }
}