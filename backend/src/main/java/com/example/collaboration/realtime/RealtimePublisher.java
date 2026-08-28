package com.example.collaboration.realtime;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class RealtimePublisher {

    private static final Logger log = LoggerFactory.getLogger(RealtimePublisher.class);
    private final SimpMessagingTemplate messaging;
    private final Executor executor;

    public RealtimePublisher(
            SimpMessagingTemplate messaging,
            @Qualifier("realtimePublisherExecutor") Executor executor
    ) {
        this.messaging = messaging;
        this.executor = executor;
    }

    public void afterCommitToDestination(String destination, String eventType, Object payload) {
        afterCommit(() -> messaging.convertAndSend(destination, RealtimeEvent.of(eventType, payload)), eventType);
    }

    public void afterCommitToUser(Long userId, String destination, String eventType, Object payload) {
        afterCommit(() -> messaging.convertAndSendToUser(
                userId.toString(), destination, RealtimeEvent.of(eventType, payload)
        ), eventType);
    }

    public void nowToDestination(String destination, String eventType, Object payload) {
        submit(() -> messaging.convertAndSend(destination, RealtimeEvent.of(eventType, payload)), eventType);
    }

    private void afterCommit(Runnable action, String eventType) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    submit(action, eventType);
                }
            });
        } else {
            submit(action, eventType);
        }
    }

    private void submit(Runnable action, String eventType) {
        try {
            CompletableFuture.runAsync(action, executor).whenComplete((ignored, failure) -> {
                if (failure != null) {
                    log.error("Realtime publication failed eventType={} failureType={}",
                            eventType, failure.getClass().getSimpleName());
                }
            });
        } catch (RuntimeException exception) {
            log.error("Realtime publisher queue rejected eventType={}", eventType);
            throw exception;
        }
    }
}

