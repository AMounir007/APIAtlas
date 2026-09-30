package com.apiatlas.controller;

import com.apiatlas.dto.StartDiscoveryRequest;
import com.apiatlas.dto.StopDiscoveryRequest;
import com.apiatlas.model.DiscoverySession;
import com.apiatlas.service.DiscoveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/discovery")
@RequiredArgsConstructor
public class DiscoveryController {
    private final DiscoveryService discovery;

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public DiscoverySession start(@Valid @RequestBody StartDiscoveryRequest request) {
        return discovery.start(request);
    }

    @PostMapping("/stop")
    public DiscoverySession stop(@Valid @RequestBody StopDiscoveryRequest request) {
        return discovery.stop(request.sessionId());
    }

    @GetMapping("/sessions")
    public List<DiscoverySession> sessions() {
        return discovery.list();
    }
}
