package com.example.backend.research.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseEmitterService {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterService.class);
    private final Map<UUID, List<SseEmitter>> emittersByRun = new ConcurrentHashMap<>();

    public record ResearchEvent(
            String eventName,
            UUID runId,
            String message,
            String timestamp
    ) {}

    public SseEmitter subscribe(UUID runId) {
        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L); // 10 minutes timeout
        emittersByRun.computeIfAbsent(runId, k -> Collections.synchronizedList(new ArrayList<>())).add(emitter);

        emitter.onCompletion(() -> removeEmitter(runId, emitter));
        emitter.onTimeout(() -> removeEmitter(runId, emitter));
        emitter.onError(e -> removeEmitter(runId, emitter));

        // Initial connect event
        sendEventToEmitter(emitter, new ResearchEvent("connected", runId, "Connected to research run event stream", Instant.now().toString()));
        log.info("Client subscribed to SSE for research run {}", runId);
        return emitter;
    }

    public void publishEvent(UUID runId, String eventName, String message) {
        ResearchEvent event = new ResearchEvent(eventName, runId, message, Instant.now().toString());
        List<SseEmitter> emitters = emittersByRun.get(runId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        List<SseEmitter> deadEmitters = new ArrayList<>();
        synchronized (emitters) {
            for (SseEmitter emitter : emitters) {
                try {
                    sendEventToEmitter(emitter, event);
                } catch (Exception e) {
                    deadEmitters.add(emitter);
                }
            }
            emitters.removeAll(deadEmitters);
        }
    }

    private void sendEventToEmitter(SseEmitter emitter, ResearchEvent event) {
        try {
            emitter.send(SseEmitter.event()
                    .name(event.eventName())
                    .data(event));
        } catch (IOException e) {
            log.debug("Failed to send SSE event {} to emitter: {}", event.eventName(), e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void removeEmitter(UUID runId, SseEmitter emitter) {
        List<SseEmitter> list = emittersByRun.get(runId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                emittersByRun.remove(runId);
            }
        }
    }
}
