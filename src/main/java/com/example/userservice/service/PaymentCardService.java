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
import com.example.userservice.specification.PaymentCardSpecifications;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCardService {
  private static final String PAYMENT_CARD_NOT_FOUND_MESSAGE = "Payment card not found with id: ";

  private final PaymentCardRepository paymentCardRepository;
  private final UserService userService;
  private final PaymentCardMapper paymentCardMapper;
  private final CacheService cacheService;

  @Transactional
  @Caching(
      evict = {
        @CacheEvict(value = "userCards", key = "#userId"),
        @CacheEvict(value = "usersWithCards", key = "#userId")
      })
  public PaymentCardResponseDTO createCard(PaymentCardRequestDTO cardRequestDTO, Long userId) {
    log.info("Creating payment card for user: {}", userId);
    User user = userService.getUserEntityById(userId);

    // Use pessimistic lock to prevent race condition
    int cardCount = paymentCardRepository.countCardsByUserId(userId);
    if (cardCount >= 5) {
      log.warn("Card limit exceeded for user: {}", userId);
      throw new CardLimitExceededException("User cannot have more than 5 payment cards");
    }

    PaymentCard card = paymentCardMapper.toEntity(cardRequestDTO);
    // Double-check in entity method (defensive programming)
    user.addPaymentCard(card);
    PaymentCard savedCard = paymentCardRepository.save(card);
    log.info("Payment card created with id: {} for user: {}", savedCard.getId(), userId);
    return paymentCardMapper.toDTO(savedCard);
  }

  public PaymentCardResponseDTO getCardById(Long id) {
    log.debug("Fetching payment card by id: {}", id);
    PaymentCard card =
        paymentCardRepository
            .findById(id)
            .orElseThrow(
                () -> new PaymentCardNotFoundException(PAYMENT_CARD_NOT_FOUND_MESSAGE + id));
    return paymentCardMapper.toDTO(card);
  }

  public Page<PaymentCardResponseDTO> getAllCards(
          String holder, Boolean active, Long userId, Pageable pageable) {

    Specification<PaymentCard> spec = PaymentCardSpecifications.hasHolderName(holder)
            .and(PaymentCardSpecifications.isActive(active))
            .and(PaymentCardSpecifications.hasUserId(userId));

    return paymentCardRepository.findAll(spec, pageable).map(paymentCardMapper::toDTO);
  }

  public Page<PaymentCardResponseDTO> getActiveCards(Pageable pageable) {
    return paymentCardRepository.findByActiveTrue(pageable).map(paymentCardMapper::toDTO);
  }

  public Page<PaymentCardResponseDTO> getAllCardsByUserId(Long userId, Pageable pageable) {
    return paymentCardRepository.findByUserId(userId, pageable).map(paymentCardMapper::toDTO);
  }

  public Page<PaymentCardResponseDTO> getActiveCardsByUserId(Long userId, Pageable pageable) {
    return paymentCardRepository
        .findByUserIdAndActiveStatus(userId, true, pageable)
        .map(paymentCardMapper::toDTO);
  }

  @Transactional
  @Caching(
      evict = {
        @CacheEvict(value = "userCards", key = "#result.userId"),
        @CacheEvict(value = "usersWithCards", key = "#result.userId")
      })
  public PaymentCardResponseDTO updateCard(Long id, PaymentCardRequestDTO cardRequestDTO) {
    log.info("Updating payment card with id: {}", id);
    PaymentCard card =
        paymentCardRepository
            .findById(id)
            .orElseThrow(
                () -> new PaymentCardNotFoundException(PAYMENT_CARD_NOT_FOUND_MESSAGE + id));

    // Validate expiration date is in the future
    if (cardRequestDTO.getExpirationDate() != null
        && !cardRequestDTO.getExpirationDate().isAfter(java.time.LocalDate.now())) {
      log.warn("Invalid expiration date provided for card id: {}", id);
      throw new IllegalArgumentException("Expiration date must be in the future");
    }

    if (!card.getNumber().equals(cardRequestDTO.getNumber())) {
      paymentCardRepository
          .findByNumber(cardRequestDTO.getNumber())
          .ifPresent(
              existingCard -> {
                if (!existingCard.getId().equals(id)) {
                  log.warn("Duplicate card number attempt for card id: {}", id);
                  throw new DuplicateCardNumberException("Card with this number already exists");
                }
              });
    }

    card.setNumber(cardRequestDTO.getNumber());
    card.setHolder(cardRequestDTO.getHolder());
    card.setExpirationDate(cardRequestDTO.getExpirationDate());

    PaymentCard updatedCard = paymentCardRepository.save(card);
    log.info("Payment card updated with id: {}", id);
    return paymentCardMapper.toDTO(updatedCard);
  }

  @Transactional
  public void activateCard(Long id) {
    log.info("Activating payment card with id: {}", id);
    PaymentCard card = getCardEntityById(id);
    paymentCardRepository.updateActiveStatus(id, true);
    cacheService.evictUserCaches(card.getUser().getId());
    log.info("Payment card activated with id: {}", id);
  }

  @Transactional
  public void deactivateCard(Long id) {
    log.info("Deactivating payment card with id: {}", id);
    PaymentCard card = getCardEntityById(id);
    paymentCardRepository.updateActiveStatus(id, false);
    cacheService.evictUserCaches(card.getUser().getId());
    log.info("Payment card deactivated with id: {}", id);
  }

  public PaymentCardResponseDTO getCardByUserAndId(Long userId, Long cardId) {
    PaymentCard card =
        paymentCardRepository
            .findByIdAndUserId(cardId, userId)
            .orElseThrow(
                () ->
                    new PaymentCardNotFoundException(
                        PAYMENT_CARD_NOT_FOUND_MESSAGE + cardId + " for user: " + userId));
    return paymentCardMapper.toDTO(card);
  }

  public PaymentCardResponseDTO getCardByNumber(String number) {
    PaymentCard card =
        paymentCardRepository
            .findByNumber(number)
            .orElseThrow(
                () -> new PaymentCardNotFoundException("Card not found with number: " + number));
    return paymentCardMapper.toDTO(card);
  }

  @Transactional
  public void deleteCard(Long id) {
    log.info("Deleting payment card with id: {}", id);
    PaymentCard card = getCardEntityById(id);
    Long userId = card.getUser().getId();
    paymentCardRepository.deleteById(id);
    // Manually evict caches since we can't use #result in void methods
    cacheService.evictUserCaches(userId);
    log.info("Payment card deleted with id: {} for user: {}", id, userId);
  }

  private PaymentCard getCardEntityById(Long id) {
    return paymentCardRepository
        .findById(id)
        .orElseThrow(() -> new PaymentCardNotFoundException(PAYMENT_CARD_NOT_FOUND_MESSAGE + id));
  }
}
