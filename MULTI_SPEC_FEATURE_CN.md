# 多 Spec 支持功能

## 📖 功能说明

swagger-coverage-commandline 现在支持同时处理多个 OpenAPI/Swagger 规范文件，自动将它们合并后生成统一的覆盖率报告。

## ✨ 特性

- ✅ 支持多个 spec 文件输入
- ✅ 自动合并多个 spec 的 paths
- ✅ 支持本地文件和远程 URL
- ✅ 路径冲突检测和警告
- ✅ 生成统一的覆盖率报告

## 🚀 使用方法

### 方法 1: 多次使用 `-s` 参数

```bash
swagger-coverage-commandline \
  -s spec1.yaml \
  -s spec2.yaml \
  -s spec3.yaml \
  -i swagger-coverage-output \
  -c config.json
```

### 方法 2: 使用逗号分隔多个文件

```bash
swagger-coverage-commandline \
  -s spec1.yaml,spec2.yaml,spec3.yaml \
  -i swagger-coverage-output \
  -c config.json
```

### 方法 3: 混合使用（多次 `-s` + 逗号分隔）

```bash
swagger-coverage-commandline \
  -s spec1.yaml,spec2.yaml \
  -s spec3.yaml \
  -i swagger-coverage-output \
  -c config.json
```

### 方法 4: 混合本地文件和远程 URL

```bash
swagger-coverage-commandline \
  -s /path/to/local/spec1.yaml,/path/to/local/spec2.yaml \
  -s https://api.example.com/openapi.yaml \
  -i swagger-coverage-output \
  -c config.json
```

## 📋 工作原理

1. **顺序加载**: 按照命令行参数的顺序依次加载每个 spec 文件
2. **基准 Spec**: 第一个成功加载的 spec 作为基准
3. **路径合并**: 后续 spec 的 paths 会合并到基准 spec 中
4. **冲突处理**: 如果路径已存在，会跳过并记录警告日志
5. **统一分析**: 使用合并后的 spec 进行覆盖率分析

## 📊 示例场景

### 场景 1: 多团队 API 规范

不同团队维护各自的 API 规范，需要生成整体覆盖率报告：

```bash
# 方式 1: 多次使用 -s
swagger-coverage-commandline \
  -s team-a-api.yaml \
  -s team-b-api.yaml \
  -s team-c-api.yaml \
  -i test-output \
  -c config.json

# 方式 2: 逗号分隔（更简洁）
swagger-coverage-commandline \
  -s team-a-api.yaml,team-b-api.yaml,team-c-api.yaml \
  -i test-output \
  -c config.json
```

### 场景 2: 微服务架构

多个微服务各有自己的 spec，需要统一的覆盖率视图：

```bash
swagger-coverage-commandline \
  -s user-service-api.yaml \
  -s order-service-api.yaml \
  -s payment-service-api.yaml \
  -s notification-service-api.yaml \
  -i integration-test-output \
  -c config.json
```

### 场景 3: 版本演进

同时分析多个 API 版本的覆盖率：

```bash
swagger-coverage-commandline \
  -s api-v1.yaml \
  -s api-v2.yaml \
  -i test-output \
  -c config.json
```

## 🔍 日志输出示例

```
INFO  c.g.v.s.c.c.g.Generator - Loading spec from: test-spec1.yaml
INFO  c.g.v.s.c.c.g.Generator - Using spec from test-spec1.yaml as base
INFO  c.g.v.s.c.c.g.Generator - Loading spec from: test-spec2.yaml
DEBUG c.g.v.s.c.c.g.Generator - Added path /api/products from test-spec2.yaml
DEBUG c.g.v.s.c.c.g.Generator - Added path /api/products/{id} from test-spec2.yaml
INFO  c.g.v.s.c.c.g.Generator - Merged spec from test-spec2.yaml, total paths: 4
INFO  c.g.v.s.c.c.g.Generator - Final merged spec has 4 paths
```

## ⚠️ 注意事项

### 路径冲突

如果多个 spec 文件包含相同的路径，工具会：
- 保留第一个出现的路径定义
- 跳过后续重复的路径
- 在日志中输出警告信息

```
WARN c.g.v.s.c.c.g.Generator - Path /api/users already exists in merged spec, skipping from spec2.yaml
```

### 最佳实践

1. **确保路径唯一性**: 不同 spec 文件应该定义不同的 API 路径
2. **使用统一的配置**: 所有 spec 应遵循相同的规范版本（OpenAPI 3.0）
3. **检查日志**: 关注合并过程中的警告信息
4. **验证结果**: 检查最终报告中的总路径数是否符合预期

## 🔧 配置示例

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

## 🆚 对比单 Spec 模式

### 之前（单 Spec）

```bash
# 只能分析一个 spec
swagger-coverage-commandline -s api.yaml -i output -c config.json
```

### 现在（多 Spec）

```bash
# 方式 1: 多次使用 -s
swagger-coverage-commandline \
  -s api1.yaml \
  -s api2.yaml \
  -s api3.yaml \
  -i output \
  -c config.json

# 方式 2: 逗号分隔（推荐，更简洁）
swagger-coverage-commandline \
  -s api1.yaml,api2.yaml,api3.yaml \
  -i output \
  -c config.json
```

## 🎯 实际应用案例

### 案例：整合多个团队的 API 覆盖率

**背景**: 公司有 3 个团队，各自维护独立的 API 规范，QA 团队需要生成整体的 API 覆盖率报告。

**解决方案**:

```bash
#!/bin/bash
# generate-coverage.sh

# 定义各团队的 spec 文件
TEAM_A_SPEC="https://git.company.com/team-a/api-spec/raw/main/openapi.yaml"
TEAM_B_SPEC="https://git.company.com/team-b/api-spec/raw/main/openapi.yaml"
TEAM_C_SPEC="https://git.company.com/team-c/api-spec/raw/main/openapi.yaml"

# 生成覆盖率报告
swagger-coverage-commandline \
  -s "$TEAM_A_SPEC" \
  -s "$TEAM_B_SPEC" \
  -s "$TEAM_C_SPEC" \
  -i ./test-results/swagger-coverage-output \
  -c ./config.json

echo "Coverage report generated: company-wide-api-coverage.html"
```

**结果**: 
- 自动合并 3 个团队的 API 规范
- 生成统一的覆盖率报告
- 清晰展示整体 API 覆盖情况

## 🐛 故障排查

### 问题 1: spec 文件加载失败

**症状**: 日志显示 "Failed to parse spec from: xxx"

**解决方案**:
- 检查文件路径是否正确
- 验证 spec 文件格式是否有效
- 确认网络连接（如果是远程 URL）

### 问题 2: 路径数量不符合预期

**症状**: 最终报告的路径数少于预期

**解决方案**:
- 检查日志中的路径冲突警告
- 确认是否有重复的路径定义
- 使用 DEBUG 级别日志查看详细信息

### 问题 3: 合并后的覆盖率异常

**症状**: 覆盖率统计不准确

**解决方案**:
- 确保所有 spec 使用相同的 OpenAPI 版本
- 检查 test output 是否包含所有 API 的调用记录
- 验证 config.json 中的规则配置

## 📚 相关文档

- [排除操作功能](./EXCLUDE_OPERATIONS_FEATURE_CN.md)
- [配置选项说明](./swagger-coverage-commandline/README.md)

## 🔄 版本历史

- **v1.1.0** (2026-01): 新增多 spec 支持功能
- **v1.0.0**: 初始版本，仅支持单个 spec

---

**提示**: 如有问题或建议，欢迎提交 Issue 或 Pull Request！
