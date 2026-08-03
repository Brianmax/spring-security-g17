--liquibase formatted sql

--changeset banking-api:002-cross-currency-transfers
ALTER TABLE transfers
    ADD COLUMN destination_amount NUMERIC(19, 4),
    ADD COLUMN source_currency VARCHAR(3),
    ADD COLUMN destination_currency VARCHAR(3),
    ADD COLUMN exchange_rate NUMERIC(19, 8),
    ADD COLUMN exchange_rate_date DATE,
    ADD COLUMN exchange_rate_provider VARCHAR(40);

UPDATE transfers transfer
SET destination_amount = transfer.amount,
    source_currency = source_account.currency,
    destination_currency = destination_account.currency,
    exchange_rate = 1.00000000,
    exchange_rate_provider = 'INTERNAL'
FROM bank_accounts source_account,
     bank_accounts destination_account
WHERE source_account.id = transfer.source_account_id
  AND destination_account.id = transfer.destination_account_id;

ALTER TABLE transfers
    ALTER COLUMN destination_amount SET NOT NULL,
    ALTER COLUMN source_currency SET NOT NULL,
    ALTER COLUMN destination_currency SET NOT NULL,
    ALTER COLUMN exchange_rate SET NOT NULL,
    ALTER COLUMN exchange_rate_provider SET NOT NULL,
    ADD CONSTRAINT ck_transfers_destination_amount CHECK (destination_amount > 0),
    ADD CONSTRAINT ck_transfers_source_currency CHECK (source_currency IN ('USD', 'EUR', 'PEN')),
    ADD CONSTRAINT ck_transfers_destination_currency CHECK (destination_currency IN ('USD', 'EUR', 'PEN')),
    ADD CONSTRAINT ck_transfers_exchange_rate CHECK (exchange_rate > 0);

--rollback ALTER TABLE transfers DROP CONSTRAINT ck_transfers_exchange_rate, DROP CONSTRAINT ck_transfers_destination_currency, DROP CONSTRAINT ck_transfers_source_currency, DROP CONSTRAINT ck_transfers_destination_amount, DROP COLUMN exchange_rate_provider, DROP COLUMN exchange_rate_date, DROP COLUMN exchange_rate, DROP COLUMN destination_currency, DROP COLUMN source_currency, DROP COLUMN destination_amount;
