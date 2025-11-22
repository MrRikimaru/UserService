package com.example.userservice.repository;

import com.example.userservice.entity.PaymentCard;
import java.util.List;
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
public interface PaymentCardRepository
    extends JpaRepository<PaymentCard, Long>, JpaSpecificationExecutor<PaymentCard> {

  List<PaymentCard> findByUserId(Long userId);

  Page<PaymentCard> findByActiveTrue(Pageable pageable);

  Optional<PaymentCard> findByIdAndUserId(Long id, Long userId);

  @Query("SELECT pc FROM PaymentCard pc WHERE pc.user.id = :userId AND pc.active = :active")
  Page<PaymentCard> findByUserIdAndActiveStatus(
      @Param("userId") Long userId, @Param("active") Boolean active, Pageable pageable);

  @Query(
      value = "SELECT COUNT(*) FROM payment_cards pc WHERE pc.user_id = :userId",
      nativeQuery = true)
  int countCardsByUserId(@Param("userId") Long userId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("UPDATE PaymentCard pc SET pc.active = :active WHERE pc.id = :id")
  void updateActiveStatus(@Param("id") Long id, @Param("active") Boolean active);

  Page<PaymentCard> findByUserId(Long userId, Pageable pageable);

  Optional<PaymentCard> findByNumber(String number);
}
