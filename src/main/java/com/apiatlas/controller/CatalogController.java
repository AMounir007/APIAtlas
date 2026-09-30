package com.apiatlas.controller;

import com.apiatlas.ai.AiAnalysisService;
import com.apiatlas.dto.ApiEndpointDto;
import com.apiatlas.model.ApiEndpoint;
import com.apiatlas.model.ApiTag;
import com.apiatlas.model.Enums.AuthType;
import com.apiatlas.model.Enums.EndpointStatus;
import com.apiatlas.model.Enums.Protocol;
import com.apiatlas.model.SecurityFinding;
import com.apiatlas.repository.ApiTagRepository;
import com.apiatlas.repository.SecurityFindingRepository;
import com.apiatlas.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/apis")
@RequiredArgsConstructor
public class CatalogController {
    private final CatalogService catalog;
    private final AiAnalysisService ai;
    private final ApiTagRepository tags;
    private final SecurityFindingRepository findings;

    public record Detail(ApiEndpoint endpoint, List<String> tags, List<SecurityFinding> findings) {}

    @GetMapping
    public Page<ApiEndpointDto> list(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size,
                                     @RequestParam(required = false) String q,
                                     @RequestParam(required = false) String method,
                                     @RequestParam(required = false) String module,
                                     @RequestParam(required = false) String service,
                                     @RequestParam(required = false) AuthType auth,
                                     @RequestParam(required = false) EndpointStatus status,
                                     @RequestParam(required = false) Protocol protocol) {
        var pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 200), Sort.by(Sort.Direction.DESC, "lastSeen"));
        return catalog.search(q, method, module, service, auth, status, protocol, pageable).map(ApiEndpointDto::from);
    }

    @GetMapping("/search")
    public Page<ApiEndpointDto> search(@RequestParam String q,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        return list(page, size, q, null, null, null, null, null, null);
    }

    @GetMapping("/{id}")
    public Detail get(@PathVariable Long id) {
        ApiEndpoint e = catalog.get(id);
        return new Detail(e, tags.findByEndpointId(id).stream().map(ApiTag::getTag).toList(), findings.findByEndpointId(id));
    }

    @PostMapping("/{id}/analyze")
    public ApiEndpointDto analyze(@PathVariable Long id) {
        return ApiEndpointDto.from(ai.enrich(id));
    }

    @PostMapping("/analyze-pending")
    public Map<String, Integer> analyzePending() {
        return Map.of("analyzed", ai.enrichPending());
    }
}
