package com.example.userservice.service;

import com.example.userservice.entity.PaymentCard;
import com.example.userservice.entity.User;
import com.example.userservice.repository.PaymentCardRepository;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.specification.UserSpecifications;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PaymentCardRepository paymentCardRepository;

    @Transactional
    public User createUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("User with email " + user.getEmail() + " already exists");
        }
        return userRepository.save(user);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
    }

    public Page<User> getAllUsers(String firstName, String surname, Boolean active, Pageable pageable) {
        Specification<User> spec = Specification.where(UserSpecifications.hasFirstName(firstName))
                .and(UserSpecifications.hasSurname(surname))
                .and(UserSpecifications.isActive(active));

        return userRepository.findAll(spec, pageable);
    }

    public Page<User> getActiveUsers(Pageable pageable) {
        return userRepository.findByActiveTrue(pageable);
    }

    public Page<User> getUsersByNameAndSurnameContaining(String name, String surname, Pageable pageable) {
        return userRepository.findByNameAndSurnameContaining(name, surname, pageable);
    }

    public Page<User> getActiveUsersBornBefore(LocalDate birthDate, Pageable pageable) {
        return userRepository.findActiveUsersBornBefore(birthDate, true, pageable);
    }

    @Transactional
    public User updateUser(Long id, User userDetails) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));

        user.setName(userDetails.getName());
        user.setSurname(userDetails.getSurname());
        user.setBirthDate(userDetails.getBirthDate());
        user.setEmail(userDetails.getEmail());

        return userRepository.save(user);
    }

    @Transactional
    public void activateUser(Long id) {
        userRepository.updateActiveStatus(id, true);
    }

    @Transactional
    public void deactivateUser(Long id) {
        userRepository.updateActiveStatus(id, false);
    }

    public List<PaymentCard> getUserCards(Long userId) {
        return paymentCardRepository.findByUserId(userId);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}