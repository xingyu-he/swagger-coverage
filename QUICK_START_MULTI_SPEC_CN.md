# 多 Spec 功能快速开始指南

## 🚀 5 分钟上手

### 1️⃣ 准备工作

确保你已经构建了最新版本：

```bash
cd /path/to/swagger-coverage
./gradlew clean build -x test
./gradlew :swagger-coverage-commandline:installDist
```

### 2️⃣ 基本使用

**单个 spec（原有方式）**:
```bash
swagger-coverage-commandline \
  -s api.yaml \
  -i swagger-coverage-output \
  -c config.json
```

**多个 spec（新功能）**:
```bash
# 方式 1: 多次使用 -s
swagger-coverage-commandline \
  -s api1.yaml \
  -s api2.yaml \
  -s api3.yaml \
  -i swagger-coverage-output \
  -c config.json

# 方式 2: 逗号分隔（推荐，更简洁）
swagger-coverage-commandline \
  -s api1.yaml,api2.yaml,api3.yaml \
  -i swagger-coverage-output \
  -c config.json

# 方式 3: 混合使用
swagger-coverage-commandline \
  -s api1.yaml,api2.yaml \
  -s api3.yaml \
  -i swagger-coverage-output \
  -c config.json
```

### 3️⃣ 实际示例

假设你有以下场景：

```bash
# 你的项目结构
project/
├── specs/
│   ├── user-api.yaml      # 用户服务 API
│   ├── order-api.yaml     # 订单服务 API
│   └── payment-api.yaml   # 支付服务 API
├── test-results/
│   └── swagger-coverage-output/  # 测试输出
└── config.json
```

**运行命令**:
```bash
cd project

# 方式 1: 多次使用 -s
swagger-coverage-commandline \
  -s specs/user-api.yaml \
  -s specs/order-api.yaml \
  -s specs/payment-api.yaml \
  -i test-results/swagger-coverage-output \
  -c config.json

# 方式 2: 逗号分隔（推荐）
swagger-coverage-commandline \
  -s specs/user-api.yaml,specs/order-api.yaml,specs/payment-api.yaml \
  -i test-results/swagger-coverage-output \
  -c config.json
```

**查看结果**:
```bash
# 报告会生成在当前目录
open swagger-coverage-report.html
```

### 4️⃣ 配置文件示例

`config.json`:
```json
{
  "rules": {
    "status": {
      "filter": ["200", "201", "400", "404", "500"]
    }
  },
  "writers": {
    "html": {
      "filename": "swagger-coverage-report.html"
    }
  },
  "excludedOperations": [
    "/api/internal/*",
    "GET /api/health",
    "GET /api/metrics"
  ]
}
```

### 5️⃣ 查看日志

运行时会看到类似的日志：

```
✅ INFO  - Loading spec from: specs/user-api.yaml
✅ INFO  - Using spec from specs/user-api.yaml as base
✅ INFO  - Loading spec from: specs/order-api.yaml
✅ INFO  - Merged spec from specs/order-api.yaml, total paths: 15
✅ INFO  - Loading spec from: specs/payment-api.yaml
✅ INFO  - Merged spec from specs/payment-api.yaml, total paths: 22
✅ INFO  - Final merged spec has 22 paths
```

## 💡 常见场景

### 场景 1: 微服务架构

```bash
#!/bin/bash
# generate-microservices-coverage.sh

# 方式 1: 多次使用 -s
swagger-coverage-commandline \
  -s https://api.example.com/user-service/openapi.yaml \
  -s https://api.example.com/order-service/openapi.yaml \
  -s https://api.example.com/payment-service/openapi.yaml \
  -s https://api.example.com/notification-service/openapi.yaml \
  -i ./test-output/swagger-coverage-output \
  -c ./config.json

# 方式 2: 逗号分隔（更简洁）
swagger-coverage-commandline \
  -s "https://api.example.com/user-service/openapi.yaml,https://api.example.com/order-service/openapi.yaml,https://api.example.com/payment-service/openapi.yaml,https://api.example.com/notification-service/openapi.yaml" \
  -i ./test-output/swagger-coverage-output \
  -c ./config.json

echo "✅ 微服务覆盖率报告已生成"
```

### 场景 2: 多团队协作

```bash
#!/bin/bash
# generate-team-coverage.sh

# 从 Git 仓库获取最新的 spec
TEAM_A_SPEC="https://git.company.com/team-a/api-spec/raw/main/openapi.yaml"
TEAM_B_SPEC="https://git.company.com/team-b/api-spec/raw/main/openapi.yaml"
TEAM_C_SPEC="https://git.company.com/team-c/api-spec/raw/main/openapi.yaml"

swagger-coverage-commandline \
  -s "$TEAM_A_SPEC" \
  -s "$TEAM_B_SPEC" \
  -s "$TEAM_C_SPEC" \
  -i ./integration-test-output/swagger-coverage-output \
  -c ./config.json

echo "✅ 多团队 API 覆盖率报告已生成"
```

### 场景 3: CI/CD 集成

```yaml
# .github/workflows/api-coverage.yml
name: API Coverage Report

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  coverage:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      
      - name: Run Integration Tests
        run: ./run-integration-tests.sh
      
      - name: Generate Coverage Report
        run: |
          swagger-coverage-commandline \
            -s specs/service-a-api.yaml \
            -s specs/service-b-api.yaml \
            -s specs/service-c-api.yaml \
            -i test-results/swagger-coverage-output \
            -c config.json
      
      - name: Upload Coverage Report
        uses: actions/upload-artifact@v2
        with:
          name: api-coverage-report
          path: swagger-coverage-report.html
```

## ⚠️ 注意事项

### 路径冲突

如果看到这样的警告：
```
⚠️  WARN - Path /api/users already exists in merged spec, skipping from spec2.yaml
```

**原因**: 多个 spec 文件定义了相同的路径  
**处理**: 工具会保留第一个出现的定义，跳过后续重复的

**解决方案**:
1. 确保不同 spec 文件定义不同的路径
2. 或者在合并前手动处理冲突

### Spec 加载失败

如果看到：
```
⚠️  WARN - Failed to parse spec from: xxx.yaml
```

**检查清单**:
- [ ] 文件路径是否正确
- [ ] 文件格式是否有效（YAML/JSON）
- [ ] 网络连接是否正常（远程 URL）
- [ ] 文件权限是否正确

## 🎯 最佳实践

### 1. 使用脚本管理

创建一个脚本来管理你的覆盖率生成：

```bash
#!/bin/bash
# generate-coverage.sh

set -e  # 遇到错误立即退出

# 配置
SPEC_DIR="./specs"
OUTPUT_DIR="./test-results/swagger-coverage-output"
CONFIG_FILE="./config.json"
REPORT_FILE="./coverage-report.html"

# 检查目录
if [ ! -d "$OUTPUT_DIR" ]; then
    echo "❌ 错误: 测试输出目录不存在: $OUTPUT_DIR"
    exit 1
fi

# 查找所有 spec 文件
SPEC_FILES=$(find "$SPEC_DIR" -name "*.yaml" -o -name "*.yml" -o -name "*.json")

if [ -z "$SPEC_FILES" ]; then
    echo "❌ 错误: 在 $SPEC_DIR 中未找到任何 spec 文件"
    exit 1
fi

# 构建命令
CMD="swagger-coverage-commandline"
for spec in $SPEC_FILES; do
    CMD="$CMD -s $spec"
done
CMD="$CMD -i $OUTPUT_DIR -c $CONFIG_FILE"

# 执行
echo "🚀 生成 API 覆盖率报告..."
echo "📝 Spec 文件:"
echo "$SPEC_FILES" | sed 's/^/  - /'
echo ""

eval $CMD

if [ $? -eq 0 ]; then
    echo "✅ 报告生成成功: $REPORT_FILE"
else
    echo "❌ 报告生成失败"
    exit 1
fi
```

### 2. 组织 Spec 文件

推荐的目录结构：

```
project/
├── specs/
│   ├── services/
│   │   ├── user-service.yaml
│   │   ├── order-service.yaml
│   │   └── payment-service.yaml
│   ├── internal/
│   │   └── admin-api.yaml
│   └── external/
│       └── partner-api.yaml
├── test-results/
│   └── swagger-coverage-output/
├── config.json
└── generate-coverage.sh
```

### 3. 使用环境变量

```bash
#!/bin/bash
# 支持不同环境

ENV=${1:-dev}

case $ENV in
  dev)
    SPEC_URLS=(
      "http://localhost:8080/api-docs"
      "http://localhost:8081/api-docs"
    )
    ;;
  staging)
    SPEC_URLS=(
      "https://staging-api.example.com/openapi.yaml"
      "https://staging-api2.example.com/openapi.yaml"
    )
    ;;
  prod)
    SPEC_URLS=(
      "https://api.example.com/openapi.yaml"
      "https://api2.example.com/openapi.yaml"
    )
    ;;
esac

CMD="swagger-coverage-commandline"
for url in "${SPEC_URLS[@]}"; do
    CMD="$CMD -s $url"
done
CMD="$CMD -i ./test-output/swagger-coverage-output -c ./config.json"

eval $CMD
```

## 📊 验证结果

生成报告后，检查以下内容：

1. **总路径数**: 确认是否等于所有 spec 的路径总和
2. **覆盖率**: 查看整体覆盖率百分比
3. **未调用操作**: 检查哪些 API 没有被测试覆盖
4. **排除的操作**: 确认排除列表是否正确

## 🔗 相关链接

- [完整功能文档](./MULTI_SPEC_FEATURE_CN.md)
- [实现总结](./MULTI_SPEC_IMPLEMENTATION_SUMMARY_CN.md)
- [排除操作功能](./EXCLUDE_OPERATIONS_FEATURE_CN.md)

## ❓ 遇到问题？

1. 查看日志输出，特别是 WARN 和 ERROR 级别的消息
2. 使用 DEBUG 日志查看详细信息：
   ```bash
   export LOG_LEVEL=DEBUG
   swagger-coverage-commandline -s ... -i ... -c ...
   ```
3. 检查 [故障排查指南](./MULTI_SPEC_FEATURE_CN.md#-故障排查)

---

**祝你使用愉快！** 🎉
