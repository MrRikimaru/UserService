package com.example.userservice.mapper;

import com.example.userservice.dto.PaymentCardRequestDTO;
import com.example.userservice.dto.PaymentCardResponseDTO;
import com.example.userservice.entity.PaymentCard;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentCardMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PaymentCard toEntity(PaymentCardRequestDTO paymentCardRequestDTO);

    @Mapping(source = "user.id", target = "userId")
    PaymentCardResponseDTO toDTO(PaymentCard paymentCard);
}