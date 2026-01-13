# 排除 API 功能流程图

## 更新后的 Empty Coverage 统计流程

```mermaid
flowchart TB
    Start([开始: ZeroCallStatisticsBuilder]) --> LoadConfig[加载配置<br/>Configuration.getExcludedOperations]
    
    LoadConfig --> GetOps[获取所有 Operations]
    GetOps --> LoopOps{遍历每个 Operation}
    
    LoopOps -->|每个 Operation| CheckCount{ProcessCount == 0?}
    CheckCount -->|否| LoopOps
    CheckCount -->|是| CheckExclude{检查是否被排除<br/>isExcluded}
    
    CheckExclude --> ParsePattern[解析排除模式]
    ParsePattern --> HasMethod{模式包含 HTTP 方法?}
    
    HasMethod -->|是| MatchMethodAndPath[匹配方法 + 路径<br/>AntPathMatcher]
    HasMethod -->|否| MatchPathOnly[仅匹配路径<br/>AntPathMatcher]
    
    MatchMethodAndPath --> IsMatch{匹配成功?}
    MatchPathOnly --> IsMatch
    
    IsMatch -->|是| LogExclude[记录日志<br/>Operation is excluded]
    IsMatch -->|否| CheckNext{还有其他模式?}
    
    CheckNext -->|是| ParsePattern
    CheckNext -->|否| AddToZeroCall[添加到 ZeroCall 列表]
    
    LogExclude --> LoopOps
    AddToZeroCall --> LoopOps
    
    LoopOps -->|完成| BuildResult[构建结果<br/>results.setZeroCall]
    BuildResult --> End([结束])
    
    style Start fill:#e1f5e1
    style End fill:#e1f5e1
    style CheckExclude fill:#fff4e1
    style IsMatch fill:#ffe1e1
    style LogExclude fill:#e1e5ff
    style AddToZeroCall fill:#e1e5ff
```

## 配置文件到代码流程

```mermaid
flowchart LR
    Config[configuration.json] --> Parse[ConfigurationOptions]
    Parse --> ExcludedOps[excludedOperations: List]
    
    ExcludedOps --> Config1[Configuration.getExcludedOperations]
    Config1 --> Builder[ZeroCallStatisticsBuilder]
    
    Builder --> Check{遍历 Operations}
    Check --> Match[AntPathMatcher]
    
    Match --> Result1[匹配: 排除]
    Match --> Result2[不匹配: 统计]
    
    style Config fill:#e1f5e1
    style ExcludedOps fill:#fff4e1
    style Result1 fill:#ffe1e1
    style Result2 fill:#e1e5ff
```

## 匹配模式示例

```mermaid
graph TB
    subgraph "单层通配符 *"
        P1["/api/internal/*"]
        P1 --> M1[✅ /api/internal/debug]
        P1 --> M2[✅ /api/internal/status]
        P1 --> M3[❌ /api/internal/sub/path]
    end
    
    subgraph "多层通配符 **"
        P2["/api/admin/**"]
        P2 --> M4[✅ /api/admin/users]
        P2 --> M5[✅ /api/admin/sub/path]
        P2 --> M6[✅ /api/admin/very/deep/path]
    end
    
    subgraph "HTTP 方法 + 路径"
        P3["GET /api/health"]
        P3 --> M7[✅ GET /api/health]
        P3 --> M8[❌ POST /api/health]
    end
    
    style P1 fill:#fff4e1
    style P2 fill:#fff4e1
    style P3 fill:#fff4e1
    style M1 fill:#e1f5e1
    style M2 fill:#e1f5e1
    style M4 fill:#e1f5e1
    style M5 fill:#e1f5e1
    style M6 fill:#e1f5e1
    style M7 fill:#e1f5e1
    style M3 fill:#ffe1e1
    style M8 fill:#ffe1e1
```

## 完整的数据流

```mermaid
sequenceDiagram
    participant User as 用户
    participant CLI as CommandLine
    participant Gen as Generator
    participant Builder as ZeroCallStatisticsBuilder
    participant Config as Configuration
    participant Matcher as AntPathMatcher
    participant Results as Results
    
    User->>CLI: 启动命令 + 配置文件
    CLI->>Gen: 创建 Generator
    Gen->>Config: 加载配置文件
    Config-->>Gen: 返回 Configuration
    
    Gen->>Builder: 初始化 Builder
    Gen->>Builder: build(results, configuration)
    
    Builder->>Config: getExcludedOperations()
    Config-->>Builder: 返回排除列表
    
    loop 遍历每个 Operation
        Builder->>Builder: 检查 processCount == 0
        alt processCount == 0
            Builder->>Builder: isExcluded(operation)
            loop 遍历排除模式
                Builder->>Matcher: match(pattern, path)
                Matcher-->>Builder: 匹配结果
            end
            alt 未被排除
                Builder->>Builder: 添加到 zeroCall 集合
            else 被排除
                Builder->>Builder: 记录日志并跳过
            end
        end
    end
    
    Builder->>Results: setZeroCall(zeroCall)
    Results-->>Gen: 返回结果
    Gen-->>User: 生成报告
```

## 配置示例

### 最小配置
```json
{
  "excludedOperations": [
    "GET /api/health"
  ]
}
```

### 完整配置
```json
{
  "excludedOperations": [
    "/api/internal/*",
    "GET /api/health",
    "GET /api/metrics",
    "/api/admin/**",
    "POST /api/debug/*",
    "/actuator/**"
  ],
  "rules": {
    "status": {
      "filter": ["200"]
    }
  },
  "writers": {
    "html": {
      "filename": "swagger-coverage-report.html"
    }
  }
}
```

## 关键代码片段

### isExcluded 方法逻辑

```java
private boolean isExcluded(OperationKey operation) {
    if (excludedOperations == null || excludedOperations.isEmpty()) {
        return false;  // 没有排除规则
    }

    String operationPath = operation.getPath();
    String operationMethod = operation.getHttpMethod().name();

    for (String excluded : excludedOperations) {
        String[] parts = excluded.trim().split("\\s+", 2);
        
        if (parts.length == 2) {
            // 格式: "GET /api/users/*"
            String method = parts[0].toUpperCase();
            String path = parts[1];
            
            if (method.equals(operationMethod) && 
                pathMatcher.match(path, operationPath)) {
                return true;  // 匹配成功，排除
            }
        } else {
            // 格式: "/api/users/*" (所有方法)
            if (pathMatcher.match(parts[0], operationPath)) {
                return true;  // 匹配成功，排除
            }
        }
    }
    
    return false;  // 未匹配任何排除规则
}
```

## 使用场景

### 场景 1：排除健康检查端点
```json
{
  "excludedOperations": [
    "GET /health",
    "GET /actuator/health",
    "GET /api/health"
  ]
}
```

### 场景 2：排除内部 API
```json
{
  "excludedOperations": [
    "/internal/**",
    "/api/internal/**",
    "/admin/**"
  ]
}
```

### 场景 3：排除特定操作
```json
{
  "excludedOperations": [
    "GET /api/metrics",
    "POST /api/debug/reset",
    "DELETE /api/cache"
  ]
}
```

### 场景 4：混合使用
```json
{
  "excludedOperations": [
    "GET /health",
    "/internal/**",
    "POST /api/test/*",
    "/actuator/**"
  ]
}
```
