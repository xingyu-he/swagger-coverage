package com.github.viclovsky.swagger.coverage.core.results.builder.postbuilder;

import com.github.viclovsky.swagger.coverage.configuration.Configuration;
import com.github.viclovsky.swagger.coverage.core.model.OperationKey;
import com.github.viclovsky.swagger.coverage.core.results.Results;
import com.github.viclovsky.swagger.coverage.core.results.builder.core.StatisticsOperationPostBuilder;
import com.github.viclovsky.swagger.coverage.core.results.data.ConditionCounter;
import com.github.viclovsky.swagger.coverage.core.results.data.CoverageOperationMap;
import com.github.viclovsky.swagger.coverage.core.results.data.CoverageState;
import com.github.viclovsky.swagger.coverage.core.results.data.OperationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;

import java.util.List;

public class ConditionStatisticsBuilder extends StatisticsOperationPostBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConditionStatisticsBuilder.class);
    private static final AntPathMatcher pathMatcher = new AntPathMatcher();

    private CoverageOperationMap coverageOperationMap = new CoverageOperationMap();
    private ConditionCounter conditionCounter = new ConditionCounter();
    private List<String> excludedOperations;

    @Override
    public void build(Results results, Configuration configuration) {
        this.excludedOperations = configuration.getExcludedOperations();
        super.build(results, configuration);
    }

    @Override
    public void buildResult(Results results) {
        results.setCoverageOperationMap(coverageOperationMap)
                .setConditionCounter(conditionCounter);
    }

    @Override
    public void buildOperation(OperationKey operation, OperationResult operationResult) {
        // 如果操作被排除，跳过统计
        if (isExcluded(operation)) {
            LOGGER.debug("Operation [{}] is excluded from statistics", operation);
            return;
        }

        conditionCounter.updateAll(operationResult.getAllConditionCount());
        conditionCounter.updateCovered(operationResult.getCoveredConditionCount());

        switch (operationResult.getState()) {
            case PARTY:
                coverageOperationMap.addParty(operation);
                break;
            case EMPTY:
                coverageOperationMap.addEmpty(operation);
                break;
            case FULL:
                coverageOperationMap.addFull(operation);
                break;
        }

        if (operationResult.getDeprecated()) {
            coverageOperationMap.addDeprecated(operation);
            conditionCounter.incrementDeprecated();

            if (operationResult.getState() == CoverageState.EMPTY) {
                conditionCounter.incrementDeprecatedAndEmpty();
            }
        }
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
