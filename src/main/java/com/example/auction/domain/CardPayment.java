package com.example.auction.domain;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("CARD")
public class CardPayment extends Payment {
    @Column(name = "card_last_four", length = 4)
    private String cardLastFour;

    @Column(name = "card_network", length = 32)
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
