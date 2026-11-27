package com.example.auction.domain;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("UPI")
public class UpiPayment extends Payment {
    @Column(name = "upi_id", length = 100)
    private String upiId;

    protected UpiPayment() {
    }

    public UpiPayment(long amount, String currency, String upiId) {
        super(amount, currency);
        this.upiId = upiId;
    }

    public String getUpiId() {
        return upiId;
    }
}
