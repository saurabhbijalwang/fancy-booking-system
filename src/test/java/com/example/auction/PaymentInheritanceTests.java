package com.example.auction;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.example.auction.domain.CardPayment;
import com.example.auction.domain.NetBankingPayment;
import com.example.auction.domain.Payment;
import com.example.auction.domain.UpiPayment;
import com.example.auction.repository.PaymentRepository;

@SpringBootTest
class PaymentInheritanceTests {
    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void joinedStrategyStoresSubtypeFieldsInDedicatedTables() {
        paymentRepository.saveAndFlush(new UpiPayment(1_000, "INR", "demo@upi"));
        paymentRepository.saveAndFlush(new CardPayment(2_000, "INR", "4242", "VISA"));
        paymentRepository.saveAndFlush(new NetBankingPayment(3_000, "INR", "Demo Bank", "account-ref"));

        List<Payment> payments = paymentRepository.findAll();
        assertThat(payments).hasSize(3);
        assertThat(payments).anySatisfy(payment -> {
            assertThat(payment).isInstanceOf(UpiPayment.class);
            assertThat(((UpiPayment) payment).getUpiId()).isEqualTo("demo@upi");
        });
        assertThat(payments).anySatisfy(payment -> {
            assertThat(payment).isInstanceOf(CardPayment.class);
            assertThat(((CardPayment) payment).getCardLastFour()).isEqualTo("4242");
        });
        assertThat(payments).anySatisfy(payment -> {
            assertThat(payment).isInstanceOf(NetBankingPayment.class);
            assertThat(((NetBankingPayment) payment).getBankName()).isEqualTo("Demo Bank");
        });

        assertThat(countRows("payment")).isEqualTo(3);
        assertThat(countRows("upi_payment")).isEqualTo(1);
        assertThat(countRows("card_payment")).isEqualTo(1);
        assertThat(countRows("net_banking_payment")).isEqualTo(1);
    }

    private int countRows(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }
}
