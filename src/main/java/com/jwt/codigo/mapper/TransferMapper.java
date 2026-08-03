package com.jwt.codigo.mapper;

import com.jwt.codigo.dto.transfer.TransferResponse;
import com.jwt.codigo.entity.TransferEntity;
import org.springframework.stereotype.Component;

@Component
public class TransferMapper {

    public TransferResponse toResponse(TransferEntity transfer) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getSourceAccount().getId(),
                transfer.getDestinationAccount().getId(),
                transfer.getAmount(),
                transfer.getDestinationAmount(),
                transfer.getSourceCurrency(),
                transfer.getDestinationCurrency(),
                transfer.getExchangeRate(),
                transfer.getExchangeRateDate(),
                transfer.getExchangeRateProvider(),
                transfer.getStatus(),
                transfer.getReference(),
                transfer.getIdempotencyKey(),
                transfer.getDescription(),
                transfer.getCreatedAt(),
                transfer.getCompletedAt(),
                transfer.getFailureReason()
        );
    }
}
