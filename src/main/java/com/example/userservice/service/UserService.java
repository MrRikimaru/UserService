package com.example.userservice.service;

import com.example.userservice.dto.PaymentCardResponseDTO;
import com.example.userservice.dto.UserRequestDTO;
import com.example.userservice.dto.UserResponseDTO;
import com.example.userservice.dto.UserWithCardsResponseDTO;
import com.example.userservice.entity.PaymentCard;
import com.example.userservice.entity.User;
import com.example.userservice.exception.DuplicateEmailException;
import com.example.userservice.exception.UserNotFoundException;
import com.example.userservice.mapper.PaymentCardMapper;
import com.example.userservice.mapper.UserMapper;
import com.example.userservice.repository.PaymentCardRepository;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.specification.UserSpecifications;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private static final String USER_NOT_FOUND_MESSAGE = "User not found with id: ";

    private final UserRepository userRepository;
    private final PaymentCardRepository paymentCardRepository;
    private final UserMapper userMapper;
    private final PaymentCardMapper paymentCardMapper;

    @Transactional
    public UserResponseDTO createUser(UserRequestDTO userRequestDTO) {
        log.info("Creating user with email: {}", userRequestDTO.getEmail());
        if (userRepository.existsByEmail(userRequestDTO.getEmail())) {
            log.warn("Duplicate email attempt: {}", userRequestDTO.getEmail());
            throw new DuplicateEmailException("User with email " + userRequestDTO.getEmail() + " already exists");
        }
        User user = userMapper.toEntity(userRequestDTO);
        User savedUser = userRepository.save(user);
        log.info("User created with id: {}", savedUser.getId());
        // No need to evict all entries - new user doesn't affect existing cache entries
        return userMapper.toDTO(savedUser);
    }

    @Cacheable(value = "users", key = "#id")
    public UserResponseDTO getUserById(Long id) {
        log.debug("Fetching user by id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_MESSAGE + id));
        return userMapper.toDTO(user);
    }

    @Cacheable(value = "usersWithCards", key = "#id")
    public UserWithCardsResponseDTO getUserWithCardsById(Long id){
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_MESSAGE + id));
        UserWithCardsResponseDTO userWithCards = new UserWithCardsResponseDTO();
        userWithCards.setId(user.getId());
        userWithCards.setName(user.getName());
        userWithCards.setSurname(user.getSurname());
        userWithCards.setBirthDate(user.getBirthDate());
        userWithCards.setEmail(user.getEmail());
        userWithCards.setActive(user.getActive());
        userWithCards.setCreatedAt(user.getCreatedAt());
        userWithCards.setUpdatedAt(user.getUpdatedAt());

        List<PaymentCardResponseDTO> cards = paymentCardRepository.findByUserId(id)
                .stream()
                .map(paymentCardMapper::toDTO)
                .toList();
        userWithCards.setPaymentCards(cards);

        return userWithCards;
    }
    public Page<UserResponseDTO> getAllUsers(String name, String surname, Boolean active, Pageable pageable) {
        Specification<User> spec = Specification.where(UserSpecifications.hasFirstName(name))
                .and(UserSpecifications.hasSurname(surname))
                .and(UserSpecifications.isActive(active));

        return userRepository.findAll(spec, pageable).map(userMapper::toDTO);
    }

    public Page<UserResponseDTO> getActiveUsers(Pageable pageable) {
        return userRepository.findByActiveTrue(pageable).map(userMapper::toDTO);
    }

    public Page<UserResponseDTO> getUsersByNameAndSurnameContaining(String name, String surname, Pageable pageable) {
        return userRepository.findByNameAndSurnameContaining(name, surname, pageable).map(userMapper::toDTO);
    }

    public Page<UserResponseDTO> getActiveUsersBornBefore(LocalDate birthDate, Pageable pageable) {
        return userRepository.findActiveUsersBornBefore(birthDate, true, pageable).map(userMapper::toDTO);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "users", key = "#id"),
            @CacheEvict(value = "usersWithCards", key = "#id"),
            @CacheEvict(value = "userCards", key = "#id")
    })
    public UserResponseDTO updateUser(Long id, UserRequestDTO userRequestDTO) {
        log.info("Updating user with id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_MESSAGE + id));

        // Check email uniqueness if email is being changed
        if (!user.getEmail().equals(userRequestDTO.getEmail()) &&
                userRepository.existsByEmail(userRequestDTO.getEmail())) {
            log.warn("Duplicate email attempt during update for user id: {}", id);
            throw new DuplicateEmailException("User with email " + userRequestDTO.getEmail() + " already exists");
        }

        user.setName(userRequestDTO.getName());
        user.setSurname(userRequestDTO.getSurname());
        user.setBirthDate(userRequestDTO.getBirthDate());
        user.setEmail(userRequestDTO.getEmail());

        User updatedUser = userRepository.save(user);
        log.info("User updated with id: {}", id);
        return userMapper.toDTO(updatedUser);
    }

    User getUserEntityById(Long id){
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_MESSAGE + id));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "users", key = "#id"),
            @CacheEvict(value = "usersWithCards", key = "#id"),
            @CacheEvict(value = "userCards", key = "#id")
    })
    public void activateUser(Long id) {
        log.info("Activating user with id: {}", id);
        User user = getUserEntityById(id);
        userRepository.updateActiveStatus(id, true);
        log.info("User activated with id: {}", id);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "users", key = "#id"),
            @CacheEvict(value = "usersWithCards", key = "#id"),
            @CacheEvict(value = "userCards", key = "#id")
    })
    public void deactivateUser(Long id) {
        log.info("Deactivating user with id: {}", id);
        User user = getUserEntityById(id);
        userRepository.updateActiveStatus(id, false);
        log.info("User deactivated with id: {}", id);
    }

    @Cacheable(value = "userCards", key = "#userId")
    public List<PaymentCardResponseDTO> getUserCards(Long userId) {
        User user = getUserEntityById(userId);
        List<PaymentCard> cards = paymentCardRepository.findByUserId(userId);
        return cards.stream().map(paymentCardMapper::toDTO).toList();
    }

    public Optional<UserResponseDTO> getUserByEmail(String email) {
        return userRepository.findByEmail(email).map(userMapper::toDTO);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "users", key = "#id"),
            @CacheEvict(value = "usersWithCards", key = "#id"),
            @CacheEvict(value = "userCards", key = "#id")
    })
    public void deleteUser(Long id) {
        log.info("Deleting user with id: {}", id);
        User user = getUserEntityById(id);
        // Payment cards will be deleted automatically due to CascadeType.ALL
        userRepository.deleteById(id);
        // Cache eviction is handled by @Caching annotation above
        log.info("User deleted with id: {}", id);
    }
}