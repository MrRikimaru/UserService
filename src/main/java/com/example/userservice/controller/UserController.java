package com.example.userservice.controller;

import com.example.userservice.dto.PaymentCardResponseDTO;
import com.example.userservice.dto.UserRequestDTO;
import com.example.userservice.dto.UserResponseDTO;
import com.example.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody UserRequestDTO userRequestDTO) {
        UserResponseDTO createdUser = userService.createUser(userRequestDTO);
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        UserResponseDTO user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @GetMapping
    public ResponseEntity<Page<UserResponseDTO>> getAllUsers(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String surname,
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        Page<UserResponseDTO> users = userService.getAllUsers(firstName, surname, active, pageable);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/active")
    public ResponseEntity<Page<UserResponseDTO>> getActiveUsers(Pageable pageable) {
        Page<UserResponseDTO> users = userService.getActiveUsers(pageable);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<UserResponseDTO>> getUsersByNameAndSurname(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String surname,
            Pageable pageable) {
        Page<UserResponseDTO> users = userService.getUsersByNameAndSurnameContaining(name, surname, pageable);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/born-before")
    public ResponseEntity<Page<UserResponseDTO>> getActiveUsersBornBefore(
            @RequestParam LocalDate birthDate,
            Pageable pageable) {
        Page<UserResponseDTO> users = userService.getActiveUsersBornBefore(birthDate, pageable);
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserRequestDTO userRequestDTO) {
        UserResponseDTO updatedUser = userService.updateUser(id, userRequestDTO);
        return ResponseEntity.ok(updatedUser);
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activateUser(@PathVariable Long id) {
        userService.activateUser(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long id) {
        userService.deactivateUser(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{userId}/cards")
    public ResponseEntity<List<PaymentCardResponseDTO>> getUserCards(@PathVariable Long userId) {
        List<PaymentCardResponseDTO> cards = userService.getUserCards(userId);
        return ResponseEntity.ok(cards);
    }
}