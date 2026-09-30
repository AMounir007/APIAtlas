package com.apiatlas.service;

import com.apiatlas.model.ApiEndpoint;
import com.apiatlas.model.Enums.AuthType;
import com.apiatlas.model.Enums.EndpointStatus;
import com.apiatlas.model.Enums.Protocol;
import com.apiatlas.repository.ApiEndpointRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogService {
    private final ApiEndpointRepository endpoints;

    public Page<ApiEndpoint> search(String q, String method, String module, String service, AuthType auth,
                                    EndpointStatus status, Protocol protocol, Pageable pageable) {
        Specification<ApiEndpoint> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String like = "%" + escapeLike(q.trim().toLowerCase(Locale.ROOT)) + "%";
                p.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like, '\\'),
                        cb.like(cb.lower(root.get("url")), like, '\\'),
                        cb.like(cb.lower(root.get("module")), like, '\\'),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("description"), "")), like, '\\')));
            }
            if (method != null && !method.isBlank()) p.add(cb.equal(root.get("method"), method.toUpperCase(Locale.ROOT)));
            if (module != null && !module.isBlank()) p.add(cb.equal(root.get("module"), module));
            if (service != null && !service.isBlank()) p.add(cb.equal(root.get("service"), service));
            if (auth != null) p.add(cb.equal(root.get("authType"), auth));
            if (status != null) p.add(cb.equal(root.get("status"), status));
            if (protocol != null) p.add(cb.equal(root.get("protocol"), protocol));
            return cb.and(p.toArray(new Predicate[0]));
        };
        return endpoints.findAll(spec, pageable);
    }

    public ApiEndpoint get(Long id) {
        return endpoints.findById(id).orElseThrow(() -> new NoSuchElementException("API " + id + " not found"));
    }

    /** All endpoints, optionally restricted to one service (used by exporters). */
    public List<ApiEndpoint> listAll(String service) {
        Sort sort = Sort.by("service", "path", "method");
        if (service == null || service.isBlank()) {
            return endpoints.findAll(sort);
        }
        return endpoints.findAll((root, query, cb) -> cb.equal(root.get("service"), service), sort);
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
