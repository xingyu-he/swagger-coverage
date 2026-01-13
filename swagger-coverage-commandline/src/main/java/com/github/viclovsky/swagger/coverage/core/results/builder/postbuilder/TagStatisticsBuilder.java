package com.github.viclovsky.swagger.coverage.core.results.builder.postbuilder;

import com.github.viclovsky.swagger.coverage.configuration.Configuration;
import com.github.viclovsky.swagger.coverage.core.generator.SwaggerSpecificationProcessor;
import com.github.viclovsky.swagger.coverage.core.model.OperationKey;
import com.github.viclovsky.swagger.coverage.core.model.OperationsHolder;
import com.github.viclovsky.swagger.coverage.core.results.Results;
import com.github.viclovsky.swagger.coverage.core.results.builder.core.StatisticsOperationPostBuilder;
import com.github.viclovsky.swagger.coverage.core.results.data.CoverageCounter;
import com.github.viclovsky.swagger.coverage.core.results.data.OperationResult;
import com.github.viclovsky.swagger.coverage.core.results.data.TagCoverage;
import com.github.viclovsky.swagger.coverage.core.rule.core.ConditionRule;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

public class TagStatisticsBuilder extends StatisticsOperationPostBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(TagStatisticsBuilder.class);
    private static final AntPathMatcher pathMatcher = new AntPathMatcher();

    private Map<OperationKey, List<String>> operationToTag;

    private Map<String, TagCoverage> tagCoverageMap;
    private CoverageCounter tagCounter = new CoverageCounter();
    private List<String> excludedOperations;

    @Override
    public TagStatisticsBuilder configure(OpenAPI swagger, List<ConditionRule> rules) {
        OperationsHolder operations = SwaggerSpecificationProcessor.extractOperation(swagger);

        tagCoverageMap = ofNullable(swagger.getTags())
                .orElse(emptyList())
                .stream()
                .collect(toMap(Tag::getName, TagCoverage::new));

        operationToTag = operations.getOperations()
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().getTags() != null)
                .collect(toMap(Map.Entry::getKey, entry -> entry.getValue().getTags()));

        operationToTag.forEach((key, value) -> value.stream()
                .filter(tag -> tagCoverageMap.containsKey(tag))
                .forEach(tag -> tagCoverageMap.get(tag).addOperation(key)));

        return this;
    }

    @Override
    public void build(Results results, Configuration configuration) {
        this.excludedOperations = configuration.getExcludedOperations();
        super.build(results, configuration);
    }

    @Override
    public void buildOperation(OperationKey operation, OperationResult operationResult) {
        // 如果操作被排除，跳过统计
        if (isExcluded(operation)) {
            LOGGER.debug("Operation [{}] is excluded from tag statistics", operation);
            return;
        }

        operationToTag.forEach((key, value) -> {
            if (operation.toString().equals(key.toString())) {
                value.stream()
                        .filter(tag -> tagCoverageMap.containsKey(tag))
                        .forEach(tag -> tagCoverageMap.get(tag)
                        .updateCallCount(operationResult.getProcessCount())
                        .incrementByState(operationResult.getState())
                        .updateAllConditionCount(operationResult.getAllConditionCount())
                        .updateCoveredConditionCount(operationResult.getCoveredConditionCount())
                        .updateState()
                );
            }
        });
    }

    @Override
    public void buildResult(Results results) {
        LOGGER.info(tagCoverageMap.toString());
        tagCoverageMap.forEach((key, value) -> tagCounter.incrementByState(value.getState()));
        results.setTagCoverageMap(tagCoverageMap).setTagCounter(tagCounter);
    }

    /**
     * 检查操作是否应该被排除（与 ZeroCallStatisticsBuilder 保持一致）
     */
    private boolean isExcluded(OperationKey operation) {
        if (excludedOperations == null || excludedOperations.isEmpty()) {
            return false;
        }

        String operationPath = operation.getPath();
        String operationMethod = operation.getHttpMethod().name();

        for (String excluded : excludedOperations) {
            String trimmedExcluded = excluded.trim();
            
            // 检查是否包含 HTTP 方法
            String[] parts = trimmedExcluded.split("\\s+", 2);
            
            if (parts.length == 2) {
                // 格式: "GET /api/users/*"
                String method = parts[0].toUpperCase();
                String path = parts[1];
                
                if (method.equals(operationMethod) && pathMatcher.match(path, operationPath)) {
                    return true;
                }
            } else {
                // 格式: "/api/users/*" (匹配所有 HTTP 方法)
                String path = parts[0];
                
                if (pathMatcher.match(path, operationPath)) {
                    return true;
                }
            }
        }
        
        return false;
    }
}
