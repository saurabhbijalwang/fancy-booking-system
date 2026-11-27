package com.example.auction.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

import com.example.auction.generator.FancyNumberGenerator;

@Entity
@Table(name = "booking")
public class Booking {
    @Id
    @GeneratedValue(generator = "fancy-number")
    @GenericGenerator(name = "fancy-number", type = FancyNumberGenerator.class)
    @Column(length = 4)
    private String id;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    protected Booking() {
    }

    public Booking(String customerName) {
        this.customerName = customerName;
    }

    public String getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }
}
