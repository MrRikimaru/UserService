package com.example.userservice.specification;

import com.example.userservice.entity.User;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class UserSpecifications {
    public static Specification<User> hasFirstName(String name) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(name)) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("name")),
                    "%" + name.toLowerCase() + "%"
            );
        };
    }

    public static Specification<User> hasSurname(String surname) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(surname)) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("surname")),
                    "%" + surname.toLowerCase() + "%"
            );
        };
    }

    public static Specification<User> isActive(Boolean active) {
        return (root, query, criteriaBuilder) -> {
            if (active == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("active"), active);
        };
    }
}