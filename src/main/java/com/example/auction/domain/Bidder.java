package com.example.auction.domain;

import java.time.Instant;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "bidder")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Bidder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Bidder() {
    }

    public Bidder(String displayName, String email, Instant createdAt) {
        this.displayName = displayName;
        this.email = email;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }
}
