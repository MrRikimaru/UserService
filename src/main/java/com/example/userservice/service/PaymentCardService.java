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
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class PaymentCardService {

    private final PaymentCardRepository paymentCardRepository;
    private final UserService userService;
    private final PaymentCardMapper paymentCardMapper;
    private final CacheService cacheService;

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "userCards", key = "#userId"),
            @CacheEvict(value = "usersWithCards", key = "#userId")
    })
    public PaymentCardResponseDTO createCard(PaymentCardRequestDTO cardRequestDTO, Long userId) {
        User user = userService.getUserEntityById(userId);

        int cardCount = paymentCardRepository.countCardsByUserId(userId);
        if (cardCount >= 5) {
            throw new CardLimitExceededException("User cannot have more than 5 payment cards");
        }

        PaymentCard card = paymentCardMapper.toEntity(cardRequestDTO);
        user.addPaymentCard(card);
        PaymentCard savedCard = paymentCardRepository.save(card);
        return paymentCardMapper.toDTO(savedCard);
    }

    public PaymentCardResponseDTO getCardById(Long id) {
        PaymentCard card = paymentCardRepository.findById(id)
                .orElseThrow(() -> new PaymentCardNotFoundException("Payment card not found with id: " + id));
        return paymentCardMapper.toDTO(card);
    }

    public Page<PaymentCardResponseDTO> getAllCards(String holder, Boolean active, Long userId, Pageable pageable) {
        Specification<PaymentCard> spec = Specification.where(PaymentCardSpecifications.hasHolderName(holder))
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
        return paymentCardRepository.findByUserIdAndActiveStatus(userId, true, pageable).map(paymentCardMapper::toDTO);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "userCards", key = "#result.userId"),
            @CacheEvict(value = "usersWithCards", key = "#result.userId")
    })
    public PaymentCardResponseDTO updateCard(Long id, PaymentCardRequestDTO cardRequestDTO) {
        PaymentCard card = paymentCardRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Payment card not found with id: " + id));

        if (!card.getNumber().equals(cardRequestDTO.getNumber())) {
            paymentCardRepository.findByNumber(cardRequestDTO.getNumber()).ifPresent(existingCard -> {
                if (!existingCard.getId().equals(id)) {
                    throw new DuplicateCardNumberException("Card with this number already exists");
                }
            });
        }

        card.setNumber(cardRequestDTO.getNumber());
        card.setHolder(cardRequestDTO.getHolder());
        card.setExpirationDate(cardRequestDTO.getExpirationDate());

        PaymentCard updatedCard = paymentCardRepository.save(card);
        return paymentCardMapper.toDTO(updatedCard);
    }

    @Transactional
    public void activateCard(Long id) {
        PaymentCard card = getCardEntityById(id);
        paymentCardRepository.updateActiveStatus(id, true);
        cacheService.evictUserCaches(card.getUser().getId());
    }

    @Transactional
    public void deactivateCard(Long id) {
        PaymentCard card = getCardEntityById(id);
        paymentCardRepository.updateActiveStatus(id, false);
        cacheService.evictUserCaches(card.getUser().getId());
    }

    public PaymentCardResponseDTO getCardByUserAndId(Long userId, Long cardId) {
        PaymentCard card = paymentCardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new PaymentCardNotFoundException("Payment card not found with id: " + cardId + " for user: " + userId));
        return paymentCardMapper.toDTO(card);
    }

    public PaymentCardResponseDTO getCardByNumber(String number) {
        PaymentCard card = paymentCardRepository.findByNumber(number)
                .orElseThrow(() -> new PaymentCardNotFoundException("Card not found with number: " + number));
        return paymentCardMapper.toDTO(card);
    }

    private PaymentCard getCardEntityById(Long id){
        return paymentCardRepository.findById(id)
                .orElseThrow(() -> new PaymentCardNotFoundException("Payment card not found with id: " + id));
    }
}