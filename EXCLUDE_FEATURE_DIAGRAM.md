# Exclude API Feature Flowcharts

## Updated Empty Coverage Statistics Flow

```mermaid
flowchart TB
    Start([Start: ZeroCallStatisticsBuilder]) --> LoadConfig[Load Configuration<br/>Configuration.getExcludedOperations]

    LoadConfig --> GetOps[Get All Operations]
    GetOps --> LoopOps{Iterate Each Operation}

    LoopOps -->|Each Operation| CheckCount{ProcessCount == 0?}
    CheckCount -->|No| LoopOps
    CheckCount -->|Yes| CheckExclude{Check if Excluded<br/>isExcluded}

    CheckExclude --> ParsePattern[Parse Exclusion Pattern]
    ParsePattern --> HasMethod{Pattern Contains HTTP Method?}

    HasMethod -->|Yes| MatchMethodAndPath[Match Method + Path<br/>AntPathMatcher]
    HasMethod -->|No| MatchPathOnly[Match Path Only<br/>AntPathMatcher]

    MatchMethodAndPath --> IsMatch{Match Success?}
    MatchPathOnly --> IsMatch

    IsMatch -->|Yes| LogExclude[Log Message<br/>Operation is excluded]
    IsMatch -->|No| CheckNext{More Patterns?}

    CheckNext -->|Yes| ParsePattern
    CheckNext -->|No| AddToZeroCall[Add to ZeroCall List]

    LogExclude --> LoopOps
    AddToZeroCall --> LoopOps

    LoopOps -->|Complete| BuildResult[Build Result<br/>results.setZeroCall]
    BuildResult --> End([End])

    style Start fill:#e1f5e1
    style End fill:#e1f5e1
    style CheckExclude fill:#fff4e1
    style IsMatch fill:#ffe1e1
    style LogExclude fill:#e1e5ff
    style AddToZeroCall fill:#e1e5ff
```

## Configuration File to Code Flow

```mermaid
flowchart LR
    Config[configuration.json] --> Parse[ConfigurationOptions]
    Parse --> ExcludedOps[excludedOperations: List]

    ExcludedOps --> Config1[Configuration.getExcludedOperations]
    Config1 --> Builder[ZeroCallStatisticsBuilder]

    Builder --> Check{Iterate Operations}
    Check --> Match[AntPathMatcher]

    Match --> Result1[Matched: Exclude]
    Match --> Result2[Not Matched: Count]

    style Config fill:#e1f5e1
    style ExcludedOps fill:#fff4e1
    style Result1 fill:#ffe1e1
    style Result2 fill:#e1e5ff
```

## Matching Pattern Examples

```mermaid
graph TB
    subgraph "Single-Level Wildcard *"
        P1["/api/internal/*"]
        P1 --> M1[✅ /api/internal/debug]
        P1 --> M2[✅ /api/internal/status]
        P1 --> M3[❌ /api/internal/sub/path]
    end

    subgraph "Multi-Level Wildcard **"
        P2["/api/admin/**"]
        P2 --> M4[✅ /api/admin/users]
        P2 --> M5[✅ /api/admin/sub/path]
        P2 --> M6[✅ /api/admin/very/deep/path]
    end

    subgraph "HTTP Method + Path"
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

## Complete Data Flow

```mermaid
sequenceDiagram
    participant User as User
    participant CLI as CommandLine
    participant Gen as Generator
    participant Builder as ZeroCallStatisticsBuilder
    participant Config as Configuration
    participant Matcher as AntPathMatcher
    participant Results as Results

    User->>CLI: Start command + config file
    CLI->>Gen: Create Generator
    Gen->>Config: Load configuration file
    Config-->>Gen: Return Configuration

    Gen->>Builder: Initialize Builder
    Gen->>Builder: build(results, configuration)

    Builder->>Config: getExcludedOperations()
    Config-->>Builder: Return exclusion list

    loop Iterate Each Operation
        Builder->>Builder: Check processCount == 0
        alt processCount == 0
            Builder->>Builder: isExcluded(operation)
            loop Iterate Exclusion Patterns
                Builder->>Matcher: match(pattern, path)
                Matcher-->>Builder: Match result
            end
            alt Not Excluded
                Builder->>Builder: Add to zeroCall set
            else Excluded
                Builder->>Builder: Log and skip
            end
        end
    end

    Builder->>Results: setZeroCall(zeroCall)
    Results-->>Gen: Return results
    Gen-->>User: Generate report
```

## Configuration Examples

### Minimal Configuration

```json
{
  "excludedOperations": ["GET /api/health"]
}
```

### Complete Configuration

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

## Key Code Snippets

### isExcluded Method Logic

```java
private boolean isExcluded(OperationKey operation) {
    if (excludedOperations == null || excludedOperations.isEmpty()) {
        return false;  // No exclusion rules
    }

    String operationPath = operation.getPath();
    String operationMethod = operation.getHttpMethod().name();

    for (String excluded : excludedOperations) {
        String[] parts = excluded.trim().split("\\s+", 2);

        if (parts.length == 2) {
            // Format: "GET /api/users/*"
            String method = parts[0].toUpperCase();
            String path = parts[1];

            if (method.equals(operationMethod) &&
                pathMatcher.match(path, operationPath)) {
                return true;  // Match successful, exclude
            }
        } else {
            // Format: "/api/users/*" (all methods)
            if (pathMatcher.match(parts[0], operationPath)) {
                return true;  // Match successful, exclude
            }
        }
    }

    return false;  // No exclusion rule matched
}
```

## Use Cases

### Use Case 1: Exclude Health Check Endpoints

```json
{
  "excludedOperations": [
    "GET /health",
    "GET /actuator/health",
    "GET /api/health"
  ]
}
```

### Use Case 2: Exclude Internal APIs

```json
{
  "excludedOperations": ["/internal/**", "/api/internal/**", "/admin/**"]
}
```

### Use Case 3: Exclude Specific Operations

```json
{
  "excludedOperations": [
    "GET /api/metrics",
    "POST /api/debug/reset",
    "DELETE /api/cache"
  ]
}
```

### Use Case 4: Mixed Usage

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
