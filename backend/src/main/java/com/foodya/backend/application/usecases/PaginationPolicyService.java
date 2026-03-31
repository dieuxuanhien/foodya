package com.foodya.backend.application.usecases;

import com.foodya.backend.application.ports.out.SystemParameterPort;
import com.foodya.backend.domain.entities.SystemParameter;
import com.foodya.backend.application.exception.ValidationException;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class PaginationPolicyService {

    public record PaginationSpec(int page, int size, int offset) {
    }

    private final SystemParameterPort systemParameterPort;

    public PaginationPolicyService(SystemParameterPort systemParameterPort) {
        this.systemParameterPort = systemParameterPort;
    }

    public PaginationSpec page(Integer page, Integer size) {
        int resolvedPage = page == null ? 0 : page;
        int defaultSize = intParam("search.default_page_size", 20);
        int maxSize = intParam("search.max_page_size", 100);
        int resolvedSize = size == null ? defaultSize : size;

        if (resolvedPage < 0) {
            throw new ValidationException("invalid pagination values", Map.of("page", "must be >= 0"));
        }
        if (resolvedSize <= 0 || resolvedSize > maxSize) {
            throw new ValidationException("invalid pagination values", Map.of("size", "must be between 1 and " + maxSize));
        }

        return new PaginationSpec(resolvedPage, resolvedSize, resolvedPage * resolvedSize);
    }

    private int intParam(String key, int fallback) {
        return systemParameterPort.findById(key)
                .map(SystemParameter::getValue)
                .map(String::trim)
                .map(Integer::parseInt)
                .orElse(fallback);
    }
}
