package com.example.auction.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "card_payment")
public class CardPayment extends Payment {
    @Column(name = "card_last_four", nullable = false, length = 4)
    private String cardLastFour;

    @Column(name = "card_network", nullable = false, length = 32)
    private String cardNetwork;

    protected CardPayment() {
    }

    public CardPayment(long amount, String currency, String cardLastFour, String cardNetwork) {
        super(amount, currency);
        this.cardLastFour = cardLastFour;
        this.cardNetwork = cardNetwork;
    }

    public String getCardLastFour() {
        return cardLastFour;
    }

    public String getCardNetwork() {
        return cardNetwork;
    }
}
