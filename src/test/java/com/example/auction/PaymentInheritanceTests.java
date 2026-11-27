package com.example.auction;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.example.auction.domain.CardPayment;
import com.example.auction.domain.NetBankingPayment;
import com.example.auction.domain.UpiPayment;
import com.example.auction.repository.PaymentRepository;

@SpringBootTest
class PaymentInheritanceTests {
    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void singleTableStoresNullColumnsForUnrelatedPaymentTypes() {
        paymentRepository.saveAndFlush(new UpiPayment(1_000, "INR", "demo@upi"));
        paymentRepository.saveAndFlush(new CardPayment(2_000, "INR", "4242", "VISA"));
        paymentRepository.saveAndFlush(new NetBankingPayment(3_000, "INR", "Demo Bank", "account-ref"));

        List<PaymentRow> rows = jdbcTemplate.query(
                """
                        SELECT payment_type, upi_id, card_last_four, card_network, bank_name, account_reference
                        FROM payment
                        """,
                (resultSet, rowNumber) -> new PaymentRow(
                        resultSet.getString("payment_type"),
                        resultSet.getString("upi_id"),
                        resultSet.getString("card_last_four"),
                        resultSet.getString("card_network"),
                        resultSet.getString("bank_name"),
                        resultSet.getString("account_reference")));

        assertThat(rows).containsExactlyInAnyOrder(
                new PaymentRow("UPI", "demo@upi", null, null, null, null),
                new PaymentRow("CARD", null, "4242", "VISA", null, null),
                new PaymentRow("NET_BANKING", null, null, null, "Demo Bank", "account-ref"));
    }

    private record PaymentRow(
            String type,
            String upiId,
            String cardLastFour,
            String cardNetwork,
            String bankName,
            String accountReference) {
    }
}
