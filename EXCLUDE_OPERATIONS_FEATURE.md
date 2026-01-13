# Exclude Operations Feature

## Feature Overview

This feature allows users to specify API operations to be excluded in the configuration file. These excluded APIs will not be counted in "Empty Coverage" (zero calls/uncovered APIs).

This is particularly useful for the following scenarios:
- Internal debugging endpoints
- Health check endpoints
- Admin-only endpoints
- APIs planned for deprecation but not yet removed
- Specific endpoints that don't require test coverage

## Configuration Method

Add the `excludedOperations` field in your configuration file (typically `configuration.json`):

```json
{
  "excludedOperations": [
    "/api/internal/*",
    "GET /api/health",
    "/api/admin/**"
  ],
  "rules": {
    ...
  },
  "writers": {
    ...
  }
}
```

## Supported Matching Patterns

### 1. Exact Path Matching
```json
"excludedOperations": [
  "/api/users"
]
```
Matches all HTTP methods for the `/api/users` path

### 2. HTTP Method + Path
```json
"excludedOperations": [
  "GET /api/users",
  "POST /api/admin"
]
```
Matches only the specified HTTP method and path combination

### 3. Single-Level Wildcard (*)
```json
"excludedOperations": [
  "/api/internal/*"
]
```
Matches:
- ✅ `/api/internal/debug`
- ✅ `/api/internal/status`
- ❌ `/api/internal/sub/path` (does not match multi-level paths)

### 4. Multi-Level Wildcard (**)
```json
"excludedOperations": [
  "/api/admin/**"
]
```
Matches:
- ✅ `/api/admin/users`
- ✅ `/api/admin/sub/path`
- ✅ `/api/admin/very/deep/path`

### 5. Combined Usage
```json
"excludedOperations": [
  "GET /api/health",
  "GET /api/metrics",
  "/api/internal/*",
  "/actuator/**",
  "POST /api/debug/*"
]
```

## Practical Usage Examples

### Example Configuration File
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
    },
    "only-declared-status": {
      "enable": true
    }
  },
  "writers": {
    "html": {
      "filename": "swagger-coverage-report.html",
      "locale": "en"
    },
    "json": {
      "filename": "swagger-coverage-report.json"
    }
  }
}
```

### Command Line Usage
```bash
java -jar swagger-coverage-commandline.jar \
  -s /path/to/swagger.yaml \
  -i /path/to/swagger-coverage-output \
  -c /path/to/configuration.json
```

## How It Works

1. **Configuration Loading**: At startup, the system reads the `excludedOperations` list from the configuration file
2. **Statistics Filtering**: When `ZeroCallStatisticsBuilder` builds zero-call statistics, it checks whether each operation matches the exclusion pattern
3. **Path Matching**: Uses Spring AntPathMatcher for path pattern matching, supporting `*` and `**` wildcards
4. **Result Generation**: Excluded APIs will not appear in the "Empty Coverage" report

## Important Notes

1. **Case Sensitivity**: HTTP methods are converted to uppercase for matching (GET, POST, PUT, etc.)
2. **Path Format**: Paths should be consistent with the format defined in Swagger/OpenAPI specifications
3. **Performance Impact**: Exclusion rules are checked during statistics compilation; a large number of rules may slightly impact performance
4. **Log Output**: Excluded operations are logged at DEBUG level for debugging purposes

## Debugging

Enable DEBUG logging to see which operations are being excluded:

```bash
java -jar swagger-coverage-commandline.jar \
  -s /path/to/swagger.yaml \
  -i /path/to/swagger-coverage-output \
  -c /path/to/configuration.json \
  --verbose
```

Example log output:
```
DEBUG - Operation [GET /api/health] is excluded by pattern [GET /api/health]
DEBUG - Operation [GET /api/internal/debug] is excluded by pattern [/api/internal/*]
```

## Technical Implementation Details

### Modified Files

1. **ConfigurationOptions.java**
   - Added `excludedOperations` field
   - Added getter/setter methods

2. **Configuration.java**
   - Added `getExcludedOperations()` method

3. **ZeroCallStatisticsBuilder.java**
   - Added `isExcluded()` method to implement exclusion logic
   - Supports multiple matching patterns
   - Uses AntPathMatcher for path matching

### Matching Algorithm

```java
private boolean isExcluded(OperationKey operation) {
    // 1. Parse exclusion rules
    // 2. Separate HTTP method and path
    // 3. Use AntPathMatcher for pattern matching
    // 4. Return matching result
}
```

## Version Compatibility

- **Backward Compatible**: If the configuration file does not have the `excludedOperations` field, the feature will not affect existing behavior
- **Empty List**: If `excludedOperations` is an empty array, it is equivalent to not being configured
- **JSON Format**: Must be a string array

## Future Enhancements

Possible feature extensions:
- Support for regular expression matching
- Support for tag-based exclusion
- Support for response status code-based exclusion
- Support for loading exclusion rules from external files
