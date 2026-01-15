[license]: http://www.apache.org/licenses/LICENSE-2.0 "Apache License 2.0"
[release]: https://github.com/viclovsky/swagger-coverage/releases/latest "Latest release"
[release-badge]: https://img.shields.io/github/release/viclovsky/swagger-coverage.svg?style=flat
[maven]: https://repo.maven.apache.org/maven2/com/github/viclovsky/swagger-coverage-commandline/ "Maven Central"
[maven-badge]: https://img.shields.io/maven-central/v/com.github.viclovsky/swagger-coverage-commandline.svg?style=flat

[![Build Status](https://github.com/viclovsky/swagger-coverage/workflows/Build/badge.svg)](https://github.com/viclovsky/swagger-coverage/actions)
[![release-badge][]][release]
[![maven-badge][]][maven]

# swagger-coverage

Swagger-coverage gives a full picture about coverage of API tests (regression) based on OAS (Swagger).
By saying coverage we mean not a broad theme functionality, but presence (or absence) of calls defined by API methods, parameters, return codes or other conditions which corresponds specification of API.

![Swagger Coverage Report](.github/swagger-coverage.png)

## How it works

Producing coverage report consists of two parts. Firstly, during test execution, filter/interceptor/proxy save information of calls in swagger format in specific folder on executing tests.
The next stage is to compare saved result with generated conditions from current API specification and builds report.

## How to use and examples

You can use swagger-coverage with any language and framework. You need to have proxy/filter/interceptor that accumulates data in swagger format.
Swagger-coverage have rest-assured integration from the box.

> There is also a Karate integration, which has its own [manual](/swagger-coverage-karate/README.md).

Add filter dependency:

```xml
 <dependency>
     <groupId>com.github.viclovsky</groupId>
     <artifactId>swagger-coverage-rest-assured</artifactId>
     <version>${latest-swagger-coverage-version}</version>
 </dependency>
```

or if use gradle, it can be added like

```
compile "com.github.viclovsky:swagger-coverage-rest-assured:$latest-swagger-coverage-version"
```

Just add filter into test client SwaggerCoverageRestAssured (SwaggerCoverageV3RestAssured for v3). For instance, as presented below:

```java
RestAssured.given().filter(new SwaggerCoverageRestAssured())
```

- Download and run command line.
  Download zip archive and unpack it. Don't forget to replace {latest-swagger-coverage-version} to latest version.

```
wget https://github.com/viclovsky/swagger-coverage/releases/download/{latest-swagger-coverage-version}/swagger-coverage-{latest-swagger-coverage-version}.zip
unzip swagger-coverage-commandline-{latest-swagger-coverage-version}.zip
```

Here is help of unzip swagger-commandline

```
./swagger-coverage-commandline --help

  Options:
  * -s, --spec
      Path to local or URL to remote swagger specification.
      Can be specified multiple times for multiple specs, or use comma-separated values.
      Examples:
        -s spec1.yaml -s spec2.yaml (multiple specs)
        -s spec1.yaml,spec2.yaml (comma-separated)
  * -i, --input
      Path to folder with generated files with coverage.
    -c, --configuration
      Path to file with report configuration.
    --help
      Print commandline help.
    -q, --quiet
      Switch on the quiet mode.
      Default: false
    -v, --verbose
      Switch on the verbose mode.
      Default: false
```

### Single Spec Usage

To compare result of API tests with current API specification and build report call command line tool after running tests like that:

```
./swagger-coverage-commandline -s swagger.json -i swagger-coverage-output
```

### Multiple Spec Usage

You can now analyze multiple API specifications simultaneously. The tool will automatically merge them:

```bash
# Method 1: Multiple -s parameters
./swagger-coverage-commandline \
  -s spec1.yaml \
  -s spec2.yaml \
  -s spec3.yaml \
  -i swagger-coverage-output

# Method 2: Comma-separated (recommended, more concise)
./swagger-coverage-commandline \
  -s spec1.yaml,spec2.yaml,spec3.yaml \
  -i swagger-coverage-output

# Method 3: Mixed usage
./swagger-coverage-commandline \
  -s spec1.yaml,spec2.yaml \
  -s spec3.yaml \
  -i swagger-coverage-output

# Method 4: Mix local files and remote URLs
./swagger-coverage-commandline \
  -s local-spec.yaml \
  -s https://api.example.com/openapi.yaml \
  -i swagger-coverage-output
```

Output of the command:

```
19:21:21 INFO  OperationSwaggerCoverageCalculator - Empty coverage:
...
19:21:21 INFO  OperationSwaggerCoverageCalculator - Partial coverage:
...
19:21:21 INFO  OperationSwaggerCoverageCalculator - Full coverage:
...
19:21:21 INFO  OperationSwaggerCoverageCalculator - Conditions: 874/2520
19:21:21 INFO  OperationSwaggerCoverageCalculator - Empty coverage 49.284 %
19:21:21 INFO  OperationSwaggerCoverageCalculator - Partial coverage 12.034 %
19:21:21 INFO  OperationSwaggerCoverageCalculator - Full coverage 38.682 %
19:21:21 INFO  FileSystemResultsWriter - Write html report in file '.../swagger-coverage-report.html'
```

Results (swagger-coverage-report.html/swagger-coverage-results.json) will be created after running of swagger-coverage.

## Configuration options

Swagger-coverage report can be configured by json-file.
You can control list of coverage, which be generated and checked for results.

### Excluding Operations from Coverage Statistics

You can exclude specific API operations from coverage statistics using the `excludedOperations` configuration. This is useful for internal APIs, health checks, or endpoints you don't want to include in coverage metrics.

Add the `excludedOperations` array at the root level of your configuration:

```json
{
  "excludedOperations": [
    "/api/internal/*",
    "GET /api/health",
    "GET /api/metrics",
    "/api/admin/**",
    "/actuator/**"
  ],
  "rules": {
    ...
  },
  "writers": {
    ...
  }
}
```

**Pattern Matching:**

- **Exact path**: `/api/users` - matches only this path
- **Method + path**: `GET /api/users` - matches specific method and path
- **Wildcard `*`**: `/api/internal/*` - matches one path segment (e.g., `/api/internal/debug`)
- **Double wildcard `**`**: `/api/admin/\*\*`- matches multiple path segments (e.g.,`/api/admin/users/list`)

**Effect:**

- Excluded operations won't be counted in "Empty coverage" statistics
- They will appear in a separate "Excluded" tab in the report
- The exclusion count is displayed in the summary: `Operations without calls: 5 (excluded: 3)`

**Example:**

```json
{
  "excludedOperations": ["GET /health", "GET /metrics", "/internal/**"],
  "rules": {
    "status": {
      "filter": ["200", "201", "400", "404"]
    }
  },
  "writers": {
    "html": {
      "filename": "swagger-coverage-report.html"
    }
  }
}
```

For more details, see [EXCLUDE_OPERATIONS_FEATURE.md](./EXCLUDE_OPERATIONS_FEATURE.md) and [MULTI_SPEC_FEATURE.md](./MULTI_SPEC_FEATURE.md).

## Rules configuration options

Options for different rules are placed in "rules" section.
You can disable some rules or change their behavior.

#### Checking response http-status

This rule create condition for every status from _responses_-section of swagger specification.
Condition mark _covered_ when report generator find specific status in results files.
Options for this rules are placed in _status_ subsection in _rules_ sections.

You can setup next options:

**enable** - _true/false_. You can disable this rule. Default value is _true_.

**filter** - _[val1,val2]_. Rule will ignore all status, which not in filter list.

**ignore** - _[val1,val2]_. Rule will ignore all status, which in ignore list.

```
{
  "rules" : {
    "status": {
      "enable": true,
      "ignore": ["400","500"],
      "filter": ["200"]
    },

    ....
  },

  ....
}
```

#### Checking the list of declared and received statuses

This rule create condition for comparing declared and received status.
Condition marked as _covered_ when result not contains any of undeclared status.
_Uncovered_ state of this condition indicates missed status in original swagger-documentation
or server errors.
Options for this rules are placed in _only-declared-status_ subsection in _rules_ sections.

You can setup next options:

**enable** - _true/false_. You can disable this rule. Default value is _true_.

```
{
  "rules" : {

    ....

    "only-declared-status" : {
      "enable" : true
    }
  },

   ....
}
```

#### Excluding deprecated operations from the coverage report statistic

This rule is created for cases when you don't want to measure coverage of deprecated operations, but only for actual ones. <br>
If an operation is deprecated then it will be excluded from _Full_, _Partial_, and _Empty_ categories and won't affect the "Operations coverage summary"

Options for this rule are placed in "_exclude-deprecated_" subsection in _rules_ sections.

You can set up next options:

**enable** - _true/false_. <br>
By default, this rule is not enabled. Add it to the config file with _true_ value to enable this rule, like in the example below:

```
{
  "rules" : {

    ....

    "exclude-deprecated" : {
      "enable" : true
    }
  },

   ....
}
```

If you need you can add your rules for generation of conditions. So, please, send your PRs.

## Result writer configuration

Options for report generation setting are placed in _writers_ sections.

#### HTML report writer

Options for html-report placed in subsection _html_ of _writers_ sections.

You can setup next options:

**locale** - two latter language code. Now supported only _en/ru_.

**filename** - filename for html report.

**numberFormat** - [Extended Java decimal format](https://freemarker.apache.org/docs/ref_builtins_number.html#topic.extendedJavaDecimalFormat) to control how numbers are displayed in the report.

```
{
  ....

  "writers": {
      "html": {
        "locale": "ru",
        "filename":"report.html",
        "numberFormat": "0.##"
      }
  }
}
```

#### Report customization

To customize your http report with your own template set full path to the template like below:

```
{
  ....

  "writers": {
    "html": {
      ....
      "customTemplatePath": "/full/path/to/report_custom.ftl"
    }
  }
}
```

[Look here](https://github.com/swagger-api/swagger-parser/blob/master/modules/swagger-parser-core/src/main/java/io/swagger/v3/parser/core/models/ParseOptions.java) to see all available options.

## Demo

I have prepared several tests. Thus you are able to have a look and touch swagger-coverage. Just run `run.sh` script.

## New Features

### Multiple Spec Support (v1.1.0+)

Analyze multiple API specifications in a single run. Perfect for:

- **Microservices architecture** - combine specs from multiple services
- **Multi-team projects** - merge specs from different teams
- **API versioning** - analyze multiple versions together

See detailed documentation:

- [MULTI_SPEC_FEATURE.md](./MULTI_SPEC_FEATURE.md) - Complete feature guide
- [COMMA_SEPARATED_FEATURE_CN.md](./COMMA_SEPARATED_FEATURE_CN.md) - Comma-separated usage (中文)
- [QUICK_START_MULTI_SPEC_CN.md](./QUICK_START_MULTI_SPEC_CN.md) - Quick start guide (中文)

### Exclude Operations (v1.0.0+)

Exclude specific API operations from coverage statistics. Useful for:

- **Internal APIs** - exclude internal/admin endpoints
- **Health checks** - exclude monitoring endpoints
- **Deprecated APIs** - exclude old APIs you don't want to track

See detailed documentation:

- [EXCLUDE_OPERATIONS_FEATURE.md](./EXCLUDE_OPERATIONS_FEATURE.md) - Complete feature guide
- [EXCLUDE_OPERATIONS_FEATURE_CN.md](./EXCLUDE_OPERATIONS_FEATURE_CN.md) - 中文文档

## Important remark

Swagger-coverage works fine with clients which were generated from swagger (for example: https://github.com/OpenAPITools/openapi-generator).
Because all methods/parameters which will be saved are 100% compatible with current API specification.

## Requirements

For a moment swagger-coverage is compatible only with OpenApi specifications v2 & v3. It is possible that swagger-coverage will support other versions.

## Pull Requests

My project is open for any enhancement. So, your help is much appreciated. Please, feel free to open your pull request or issue and I will consider it in several days.

## Created & Maintained By

[Victor Orlovsky](https://github.com/viclovsky)

## Contributing to swagger-coverage

Thanks to all people who contributed. Especially

- [@TemaMak](https://github.com/TemaMak)
- [@Emilio-Pega](https://github.com/Emilio-Pega)

who have contributed significant improvements to swagger-coverage.

## License

Swagger coverage is released under version 2.0 of the [Apache License](http://www.apache.org/licenses/LICENSE-2.0)
