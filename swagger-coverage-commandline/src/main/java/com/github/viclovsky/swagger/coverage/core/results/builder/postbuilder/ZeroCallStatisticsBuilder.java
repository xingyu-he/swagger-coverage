package com.github.viclovsky.swagger.coverage.core.results.builder.postbuilder;

import com.github.viclovsky.swagger.coverage.configuration.Configuration;
import com.github.viclovsky.swagger.coverage.core.model.OperationKey;
import com.github.viclovsky.swagger.coverage.core.results.Results;
import com.github.viclovsky.swagger.coverage.core.results.builder.core.StatisticsOperationPostBuilder;
import com.github.viclovsky.swagger.coverage.core.results.data.OperationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ZeroCallStatisticsBuilder extends StatisticsOperationPostBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZeroCallStatisticsBuilder.class);
    private static final AntPathMatcher pathMatcher = new AntPathMatcher();

    private Set<OperationKey> zeroCall = new HashSet<>();
    private Set<OperationKey> excludedOperationKeys = new HashSet<>();
    private List<String> excludedOperations;
    private int excludedCount = 0;

    @Override
    public void build(Results results, Configuration configuration) {
        this.excludedOperations = configuration.getExcludedOperations();
        super.build(results, configuration);
    }

    @Override
    public void buildOperation(OperationKey operation, OperationResult operationResult) {
        if (operationResult.getProcessCount() == 0) {
            if (isExcluded(operation)) {
                excludedCount++;
                excludedOperationKeys.add(operation);
            } else {
                zeroCall.add(operation);
            }
        }
    }

    @Override
    public void buildResult(Results results) {
        results.setZeroCall(zeroCall);
        results.setExcludedOperations(excludedOperationKeys);
        results.setExcludedOperationsCount(excludedCount);
    }

    /**
     * Check if an operation should be excluded
     * Supported formats:
     * 1. "GET /api/users" - HTTP method and path
     * 2. "/api/users" - path only (matches all HTTP methods)
     * 3. "/api/users/*" - wildcard path
     * 4. "GET /api/users/*" - HTTP method + wildcard path
     */
    private boolean isExcluded(OperationKey operation) {
        if (excludedOperations == null || excludedOperations.isEmpty()) {
            return false;
        }

        String operationPath = operation.getPath();
        String operationMethod = operation.getHttpMethod().name();

        for (String excluded : excludedOperations) {
            String trimmedExcluded = excluded.trim();
            
            // Check if HTTP method is included
            String[] parts = trimmedExcluded.split("\\s+", 2);
            
            if (parts.length == 2) {
                // Format: "GET /api/users/*"
                String method = parts[0].toUpperCase();
                String path = parts[1];
                
                if (method.equals(operationMethod) && pathMatcher.match(path, operationPath)) {
                    LOGGER.debug("Operation [{}] is excluded by pattern [{}]", operation, trimmedExcluded);
                    return true;
                }
            } else {
                // Format: "/api/users/*" (matches all HTTP methods)
                String path = parts[0];
                
                if (pathMatcher.match(path, operationPath)) {
                    LOGGER.debug("Operation [{}] is excluded by pattern [{}]", operation, trimmedExcluded);
                    return true;
                }
            }
        }
        
        return false;
    }
}
