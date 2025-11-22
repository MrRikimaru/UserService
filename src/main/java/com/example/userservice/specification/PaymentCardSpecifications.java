package com.example.userservice.specification;

import com.example.userservice.entity.PaymentCard;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class PaymentCardSpecifications {
  private PaymentCardSpecifications() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  public static Specification<PaymentCard> hasHolderName(String holder) {
    return (root, query, criteriaBuilder) -> {
      if (!StringUtils.hasText(holder)) {
        return criteriaBuilder.conjunction();
      }
      return criteriaBuilder.like(
          criteriaBuilder.lower(root.get("holder")), "%" + holder.toLowerCase() + "%");
    };
  }

  public static Specification<PaymentCard> isActive(Boolean active) {
    return (root, query, criteriaBuilder) -> {
      if (active == null) {
        return criteriaBuilder.conjunction();
      }
      return criteriaBuilder.equal(root.get("active"), active);
    };
  }

  public static Specification<PaymentCard> hasUserId(Long userId) {
    return (root, query, criteriaBuilder) -> {
      if (userId == null) {
        return criteriaBuilder.conjunction();
      }
      return criteriaBuilder.equal(root.get("user").get("id"), userId);
    };
  }
}
