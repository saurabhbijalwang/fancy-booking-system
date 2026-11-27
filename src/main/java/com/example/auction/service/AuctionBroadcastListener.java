package com.example.auction.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AuctionBroadcastListener {
    private final SimpMessagingTemplate messagingTemplate;

    public AuctionBroadcastListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void broadcast(AuctionUpdatedEvent event) {
        messagingTemplate.convertAndSend("/topic/auctions/" + event.auction().id(), event.auction());
    }
}
