package com.example.userservice.service;

import com.example.userservice.entity.PaymentCard;
import com.example.userservice.entity.User;
import com.example.userservice.repository.PaymentCardRepository;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.specification.PaymentCardSpecifications;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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

    @Transactional
    public PaymentCard createCard(PaymentCard card, Long userId) {
        User user = userService.getUserById(userId);

        int cardCount = paymentCardRepository.countCardsByUserId(userId);
        if (cardCount >= 5) {
            throw new IllegalStateException("User cannot have more than 5 payment cards");
        }

        user.addPaymentCard(card);
        return paymentCardRepository.save(card);
    }

    public PaymentCard getCardById(Long id) {
        return paymentCardRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Payment card not found with id: " + id));
    }

    public Page<PaymentCard> getAllCards(String holderName, Boolean active, Long userId, Pageable pageable) {
        Specification<PaymentCard> spec = Specification.where(PaymentCardSpecifications.hasHolderName(holderName)).and(PaymentCardSpecifications.isActive(active)).and(PaymentCardSpecifications.hasUserId(userId));

        return paymentCardRepository.findAll(spec, pageable);
    }

    public Page<PaymentCard> getActiveCards(Pageable pageable) {
        return paymentCardRepository.findByActiveTrue(pageable);
    }

    public Page<PaymentCard> getAllCardsByUserId(Long userId, Pageable pageable) {
        return paymentCardRepository.findByUserId(userId, pageable);
    }

    public Page<PaymentCard> getActiveCardsByUserId(Long userId, Pageable pageable) {
        return paymentCardRepository.findByUserIdAndActiveStatus(userId, true, pageable);
    }

    @Transactional
    public PaymentCard updateCard(Long id, PaymentCard cardDetails) {
        PaymentCard card = getCardById(id);

        if (!card.getNumber().equals(cardDetails.getNumber())) {
            paymentCardRepository.findByNumber(cardDetails.getNumber()).ifPresent(existingCard -> {
                if (!existingCard.getId().equals(id)) {
                    throw new IllegalArgumentException("Card with this number already exists");
                }
            });
        }

        card.setNumber(cardDetails.getNumber());
        card.setHolder(cardDetails.getHolder());
        card.setExpirationDate(cardDetails.getExpirationDate());

        return paymentCardRepository.save(card);
    }

    @Transactional
    public void activateCard(Long id) {
        paymentCardRepository.updateActiveStatus(id, true);
    }

    @Transactional
    public void deactivateCard(Long id) {
        paymentCardRepository.updateActiveStatus(id, false);
    }

    public PaymentCard getCardByUserAndId(Long userId, Long cardId) {
        return paymentCardRepository.findByIdAndUserId(cardId, userId).orElseThrow(() -> new EntityNotFoundException("Payment card not found with id: " + cardId + " for user: " + userId));
    }

    public PaymentCard getCardByNumber(String number) {
        return paymentCardRepository.findByNumber(number).orElseThrow(() -> new EntityNotFoundException("Card not found with number: " + number));
    }
}