package com.example.auction.domain;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("NET_BANKING")
public class NetBankingPayment extends Payment {
    @Column(name = "bank_name", length = 80)
    private String bankName;

    @Column(name = "account_reference", length = 80)
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
