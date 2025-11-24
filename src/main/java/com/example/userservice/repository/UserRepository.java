package com.example.userservice.repository;

import com.example.userservice.entity.User;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

  Optional<User> findByEmail(String email);

  Page<User> findByActiveTrue(Pageable pageable);

  boolean existsByEmail(String email);

  @Query("SELECT u FROM User u LEFT JOIN FETCH u.paymentCards WHERE u.id = :id")
  Optional<User> findByIdWithCards(@Param("id") Long id);

  @Query(
      "SELECT u FROM User u WHERE "
          + "(:name IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND "
          + "(:surname IS NULL OR LOWER(u.surname) LIKE LOWER(CONCAT('%', :surname, '%')))")
  Page<User> findByNameAndSurnameContaining(
      @Param("name") String name, @Param("surname") String surname, Pageable pageable);

  @Query(
      value = "SELECT * FROM users u WHERE u.active = :active AND u.birth_date < :birthDate",
      nativeQuery = true)
  Page<User> findActiveUsersBornBefore(
      @Param("birthDate") LocalDate birthDate, @Param("active") boolean active, Pageable pageable);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("UPDATE User u SET u.active = :active WHERE u.id = :id")
  void updateActiveStatus(@Param("id") Long id, @Param("active") Boolean active);
}
