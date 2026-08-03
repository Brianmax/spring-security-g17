package com.jwt.codigo.repository;

import com.jwt.codigo.enums.AccountStatus;
import com.jwt.codigo.enums.CurrencyCode;

import java.math.BigDecimal;
import java.util.UUID;

public interface AccountTransferSnapshot {

    UUID getId();

    AccountStatus getStatus();

    BigDecimal getBalance();

    CurrencyCode getCurrency();
}
