# Multiple Spec Support Feature

## 📖 Overview

swagger-coverage-commandline now supports processing multiple OpenAPI/Swagger specification files simultaneously, automatically merging them to generate a unified coverage report.

## ✨ Features

- ✅ Support for multiple spec file inputs
- ✅ Automatic merging of paths from multiple specs
- ✅ Support for both local files and remote URLs
- ✅ Path conflict detection and warnings
- ✅ Generate unified coverage reports

## 🚀 Usage

### Method 1: Multiple `-s` Parameters

```bash
swagger-coverage-commandline \
  -s spec1.yaml \
  -s spec2.yaml \
  -s spec3.yaml \
  -i swagger-coverage-output \
  -c config.json
```

### Method 2: Comma-Separated Values

```bash
swagger-coverage-commandline \
  -s spec1.yaml,spec2.yaml,spec3.yaml \
  -i swagger-coverage-output \
  -c config.json
```

### Method 3: Mixed Usage (Multiple `-s` + Comma-Separated)

```bash
swagger-coverage-commandline \
  -s spec1.yaml,spec2.yaml \
  -s spec3.yaml \
  -i swagger-coverage-output \
  -c config.json
```

### Method 4: Mix Local Files and Remote URLs

```bash
swagger-coverage-commandline \
  -s /path/to/local/spec1.yaml,/path/to/local/spec2.yaml \
  -s https://api.example.com/openapi.yaml \
  -i swagger-coverage-output \
  -c config.json
```

## 📋 How It Works

1. **Sequential Loading**: Each spec file is loaded in the order specified in command-line arguments
2. **Base Spec**: The first successfully loaded spec serves as the base
3. **Path Merging**: Paths from subsequent specs are merged into the base spec
4. **Conflict Handling**: If a path already exists, it's skipped and a warning is logged
5. **Unified Analysis**: Coverage analysis is performed using the merged spec

## 📊 Example Scenarios

### Scenario 1: Multi-Team API Specifications

Different teams maintain their own API specs, need to generate overall coverage report:

```bash
# Method 1: Multiple -s parameters
swagger-coverage-commandline \
  -s team-a-api.yaml \
  -s team-b-api.yaml \
  -s team-c-api.yaml \
  -i test-output \
  -c config.json

# Method 2: Comma-separated (more concise)
swagger-coverage-commandline \
  -s team-a-api.yaml,team-b-api.yaml,team-c-api.yaml \
  -i test-output \
  -c config.json
```

### Scenario 2: Microservices Architecture

Multiple microservices each with their own spec, need unified coverage view:

```bash
swagger-coverage-commandline \
  -s user-service-api.yaml \
  -s order-service-api.yaml \
  -s payment-service-api.yaml \
  -s notification-service-api.yaml \
  -i integration-test-output \
  -c config.json
```

### Scenario 3: Version Evolution

Analyze coverage across multiple API versions simultaneously:

```bash
swagger-coverage-commandline \
  -s api-v1.yaml \
  -s api-v2.yaml \
  -i test-output \
  -c config.json
```

## 🔍 Log Output Example

```
INFO  c.g.v.s.c.c.g.Generator - Loading spec from: test-spec1.yaml
INFO  c.g.v.s.c.c.g.Generator - Using spec from test-spec1.yaml as base
INFO  c.g.v.s.c.c.g.Generator - Loading spec from: test-spec2.yaml
DEBUG c.g.v.s.c.c.g.Generator - Added path /api/products from test-spec2.yaml
DEBUG c.g.v.s.c.c.g.Generator - Added path /api/products/{id} from test-spec2.yaml
INFO  c.g.v.s.c.c.g.Generator - Merged spec from test-spec2.yaml, total paths: 4
INFO  c.g.v.s.c.c.g.Generator - Final merged spec has 4 paths
```

## ⚠️ Important Notes

### Path Conflicts

When multiple spec files contain the same path, the tool will:
- Keep the first occurrence of the path definition
- Skip subsequent duplicate paths
- Output warning messages in the logs

```
WARN c.g.v.s.c.c.g.Generator - Path /api/users already exists in merged spec, skipping from spec2.yaml
```

### Best Practices

1. **Ensure Path Uniqueness**: Different spec files should define different API paths
2. **Use Consistent Configuration**: All specs should follow the same specification version (OpenAPI 3.0)
3. **Check Logs**: Pay attention to warning messages during the merge process
4. **Verify Results**: Check if the total number of paths in the final report matches expectations

## 🔧 Configuration Example

`config.json`:

```json
{
  "rules": {
    "status": {
      "filter": ["200", "201", "400", "404"]
    }
  },
  "writers": {
    "html": {
      "filename": "multi-spec-coverage-report.html"
    }
  },
  "excludedOperations": [
    "/api/internal/*",
    "GET /api/health"
  ]
}
```

## 🆚 Comparison with Single Spec Mode

### Before (Single Spec)

```bash
# Can only analyze one spec
swagger-coverage-commandline -s api.yaml -i output -c config.json
```

### Now (Multiple Specs)

```bash
# Method 1: Multiple -s parameters
swagger-coverage-commandline \
  -s api1.yaml \
  -s api2.yaml \
  -s api3.yaml \
  -i output \
  -c config.json

# Method 2: Comma-separated (recommended, more concise)
swagger-coverage-commandline \
  -s api1.yaml,api2.yaml,api3.yaml \
  -i output \
  -c config.json
```

## 🎯 Real-World Use Case

### Case: Consolidating API Coverage Across Multiple Teams

**Background**: A company has 3 teams, each maintaining independent API specifications. The QA team needs to generate an overall API coverage report.

**Solution**:

```bash
#!/bin/bash
# generate-coverage.sh

# Define spec files for each team
TEAM_A_SPEC="https://git.company.com/team-a/api-spec/raw/main/openapi.yaml"
TEAM_B_SPEC="https://git.company.com/team-b/api-spec/raw/main/openapi.yaml"
TEAM_C_SPEC="https://git.company.com/team-c/api-spec/raw/main/openapi.yaml"

# Generate coverage report
swagger-coverage-commandline \
  -s "$TEAM_A_SPEC" \
  -s "$TEAM_B_SPEC" \
  -s "$TEAM_C_SPEC" \
  -i ./test-results/swagger-coverage-output \
  -c ./config.json

echo "Coverage report generated: company-wide-api-coverage.html"
```

**Result**: 
- Automatically merges API specs from 3 teams
- Generates unified coverage report
- Clearly shows overall API coverage status

## 🐛 Troubleshooting

### Issue 1: Spec File Loading Failed

**Symptom**: Log shows "Failed to parse spec from: xxx"

**Solution**:
- Check if the file path is correct
- Verify if the spec file format is valid
- Confirm network connection (if remote URL)

### Issue 2: Path Count Doesn't Match Expectations

**Symptom**: Final report has fewer paths than expected

**Solution**:
- Check for path conflict warnings in logs
- Confirm if there are duplicate path definitions
- Use DEBUG level logging for detailed information

### Issue 3: Abnormal Coverage After Merge

**Symptom**: Coverage statistics are inaccurate

**Solution**:
- Ensure all specs use the same OpenAPI version
- Check if test output contains call records for all APIs
- Verify rule configuration in config.json

## 📚 Related Documentation

- [Exclude Operations Feature](./EXCLUDE_OPERATIONS_FEATURE.md)
- [Configuration Options](./swagger-coverage-commandline/README.md)

## 🔄 Version History

- **v1.1.0** (2026-01): Added multiple spec support feature
- **v1.0.0**: Initial version, single spec only

---

**Note**: If you have questions or suggestions, feel free to submit an Issue or Pull Request!
