package com.apiatlas.analyzer;

import com.apiatlas.config.AtlasProperties;
import com.apiatlas.dto.ApiEndpointDto;
import com.apiatlas.dto.DuplicateGroupDto;
import com.apiatlas.model.ApiEndpoint;
import com.apiatlas.model.Enums.EndpointStatus;
import com.apiatlas.repository.ApiEndpointRepository;
import com.apiatlas.utility.PathNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/** Catalog-level analysis: duplicate detection and unused API detection. */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApiAnalyzer {
    private final ApiEndpointRepository endpoints;
    private final AtlasProperties props;

    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT10M")
    @Transactional
    public void markUnused() {
        Instant cutoff = Instant.now().minus(props.analysis().unusedThreshold());
        List<ApiEndpoint> stale = endpoints.findByStatusAndLastSeenBefore(EndpointStatus.ACTIVE, cutoff);
        stale.forEach(e -> e.setStatus(EndpointStatus.UNUSED));
        endpoints.saveAll(stale);
        if (!stale.isEmpty()) {
            log.info("Marked {} endpoints as UNUSED", stale.size());
        }
    }

    /** Groups endpoints with the same method and (version-insensitive) path similarity above the configured threshold. */
    @Transactional(readOnly = true)
    public List<DuplicateGroupDto> findDuplicates() {
        double threshold = props.analysis().duplicateSimilarity();
        Map<String, List<ApiEndpoint>> byMethod = endpoints.findAll().stream()
                .collect(Collectors.groupingBy(ApiEndpoint::getMethod));
        List<DuplicateGroupDto> groups = new ArrayList<>();
        byMethod.forEach((method, list) -> {
            List<List<ApiEndpoint>> clusters = new ArrayList<>();
            List<Set<String>> reps = new ArrayList<>();
            for (ApiEndpoint e : list) {
                Set<String> segs = segments(e.getPath());
                int target = -1;
                for (int i = 0; i < reps.size(); i++) {
                    if (similarity(reps.get(i), segs) >= threshold) {
                        target = i;
                        break;
                    }
                }
                if (target < 0) {
                    clusters.add(new ArrayList<>(List.of(e)));
                    reps.add(segs);
                } else {
                    clusters.get(target).add(e);
                }
            }
            for (int i = 0; i < clusters.size(); i++) {
                List<ApiEndpoint> c = clusters.get(i);
                if (c.size() > 1) {
                    groups.add(new DuplicateGroupDto(method + " " + PathNormalizer.stripVersion(c.get(0).getPath()),
                            threshold, c.stream().map(ApiEndpointDto::from).toList()));
                }
            }
        });
        groups.sort(Comparator.comparingInt((DuplicateGroupDto g) -> g.endpoints().size()).reversed());
        return groups;
    }

    static Set<String> segments(String path) {
        Set<String> out = new LinkedHashSet<>();
        for (String s : PathNormalizer.stripVersion(path).split("/")) {
            if (!s.isEmpty()) out.add(s.toLowerCase(Locale.ROOT));
        }
        return out;
    }

    static double similarity(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) return 1.0;
        Set<String> inter = new HashSet<>(a);
        inter.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return (double) inter.size() / union.size();
    }
}
