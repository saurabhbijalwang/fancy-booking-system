package com.example.auction.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "net_banking_payment")
public class NetBankingPayment extends Payment {
    @Column(name = "bank_name", nullable = false, length = 80)
    private String bankName;

    @Column(name = "account_reference", nullable = false, length = 80)
    private String accountReference;

    protected NetBankingPayment() {
    }

    public NetBankingPayment(long amount, String currency, String bankName, String accountReference) {
        super(amount, currency);
        this.bankName = bankName;
        this.accountReference = accountReference;
    }

    public String getBankName() {
        return bankName;
    }

    public String getAccountReference() {
        return accountReference;
    }
}
