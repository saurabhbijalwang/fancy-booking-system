CREATE TABLE upi_payment (
    id BIGINT PRIMARY KEY REFERENCES payment(id),
    upi_id VARCHAR(100) NOT NULL
);

CREATE TABLE card_payment (
    id BIGINT PRIMARY KEY REFERENCES payment(id),
    card_last_four VARCHAR(4) NOT NULL,
    card_network VARCHAR(32) NOT NULL
);

CREATE TABLE net_banking_payment (
    id BIGINT PRIMARY KEY REFERENCES payment(id),
    bank_name VARCHAR(80) NOT NULL,
    account_reference VARCHAR(80) NOT NULL
);

INSERT INTO upi_payment (id, upi_id)
SELECT id, upi_id FROM payment WHERE payment_type = 'UPI';

INSERT INTO card_payment (id, card_last_four, card_network)
SELECT id, card_last_four, card_network FROM payment WHERE payment_type = 'CARD';

INSERT INTO net_banking_payment (id, bank_name, account_reference)
SELECT id, bank_name, account_reference FROM payment WHERE payment_type = 'NET_BANKING';

ALTER TABLE payment DROP COLUMN payment_type;
ALTER TABLE payment DROP COLUMN upi_id;
ALTER TABLE payment DROP COLUMN card_last_four;
ALTER TABLE payment DROP COLUMN card_network;
ALTER TABLE payment DROP COLUMN bank_name;
ALTER TABLE payment DROP COLUMN account_reference;
