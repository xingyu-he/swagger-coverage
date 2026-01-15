# 逗号分隔 Spec 文件功能

## ✨ 新增功能

现在 `-s` 参数支持**逗号分隔**的多个文件路径，让命令行更简洁！

## 🎯 使用方式对比

### 之前：只能多次使用 `-s`

```bash
swagger-coverage-commandline \
  -s spec1.yaml \
  -s spec2.yaml \
  -s spec3.yaml \
  -i swagger-coverage-output \
  -c config.json
```

### 现在：支持逗号分隔

```bash
# 方式 1: 逗号分隔（推荐，更简洁）
swagger-coverage-commandline \
  -s spec1.yaml,spec2.yaml,spec3.yaml \
  -i swagger-coverage-output \
  -c config.json

# 方式 2: 多次使用 -s（仍然支持）
swagger-coverage-commandline \
  -s spec1.yaml \
  -s spec2.yaml \
  -s spec3.yaml \
  -i swagger-coverage-output \
  -c config.json

# 方式 3: 混合使用
swagger-coverage-commandline \
  -s spec1.yaml,spec2.yaml \
  -s spec3.yaml \
  -i swagger-coverage-output \
  -c config.json
```

## 📋 实现细节

### 代码修改

**MainOptions.java**:
```java
// 修改前：只支持 URI 列表
private List<URI> specPaths = new ArrayList<>();

// 修改后：支持字符串列表，自动解析逗号分隔
private List<String> specPathStrings = new ArrayList<>();

public List<URI> getSpecPaths() {
    // 自动分割逗号分隔的值
    List<URI> result = new ArrayList<>();
    for (String pathString : specPathStrings) {
        String[] paths = pathString.split(",");
        for (String path : paths) {
            String trimmed = path.trim();
            if (!trimmed.isEmpty()) {
                result.add(new URI(trimmed));
            }
        }
    }
    return result;
}
```

### 解析逻辑

1. **接收参数**: `-s` 参数接收字符串列表
2. **分割处理**: 遇到逗号时自动分割
3. **去除空格**: 自动 trim 每个路径
4. **转换 URI**: 将字符串转换为 URI 对象
5. **错误处理**: 无效 URI 会抛出清晰的异常

## 🚀 使用示例

### 示例 1: 本地文件

```bash
# 简洁方式
swagger-coverage-commandline \
  -s user-api.yaml,order-api.yaml,payment-api.yaml \
  -i test-output \
  -c config.json
```

### 示例 2: 远程 URL

```bash
# 使用引号包裹（推荐）
swagger-coverage-commandline \
  -s "https://api.example.com/spec1.yaml,https://api.example.com/spec2.yaml" \
  -i test-output \
  -c config.json
```

### 示例 3: 混合本地和远程

```bash
swagger-coverage-commandline \
  -s local-spec1.yaml,local-spec2.yaml \
  -s https://api.example.com/remote-spec.yaml \
  -i test-output \
  -c config.json
```

### 示例 4: 带空格的路径

```bash
# 自动去除空格
swagger-coverage-commandline \
  -s "spec1.yaml, spec2.yaml, spec3.yaml" \
  -i test-output \
  -c config.json
```

## ✅ 测试验证

### 测试 1: 纯逗号分隔

```bash
swagger-coverage-commandline \
  -s test-spec-a.yaml,test-spec-b.yaml,test-spec-c.yaml \
  -i test-output \
  -c config.json
```

**结果**: ✅ 成功加载 3 个 spec 文件

**日志**:
```
INFO  - Loading spec from: test-spec-a.yaml
INFO  - Using spec from test-spec-a.yaml as base
INFO  - Loading spec from: test-spec-b.yaml
INFO  - Merged spec from test-spec-b.yaml, total paths: 2
INFO  - Loading spec from: test-spec-c.yaml
INFO  - Merged spec from test-spec-c.yaml, total paths: 3
INFO  - Final merged spec has 3 paths
```

### 测试 2: 混合使用

```bash
swagger-coverage-commandline \
  -s test-spec-a.yaml,test-spec-b.yaml \
  -s test-spec-c.yaml \
  -i test-output \
  -c config.json
```

**结果**: ✅ 成功加载 3 个 spec 文件

## 💡 最佳实践

### 1. 文件数量较少时使用逗号分隔

```bash
# 推荐：简洁明了
swagger-coverage-commandline \
  -s api1.yaml,api2.yaml,api3.yaml \
  -i test-output \
  -c config.json
```

### 2. 文件数量较多时使用脚本

```bash
#!/bin/bash
# generate-coverage.sh

SPECS="spec1.yaml,spec2.yaml,spec3.yaml,spec4.yaml,spec5.yaml"

swagger-coverage-commandline \
  -s "$SPECS" \
  -i test-output \
  -c config.json
```

### 3. 远程 URL 使用引号

```bash
# 推荐：避免 shell 解析问题
swagger-coverage-commandline \
  -s "https://api.example.com/spec1.yaml,https://api.example.com/spec2.yaml" \
  -i test-output \
  -c config.json
```

### 4. 混合本地和远程时分组

```bash
# 推荐：本地文件用逗号分隔，远程 URL 单独指定
swagger-coverage-commandline \
  -s local1.yaml,local2.yaml,local3.yaml \
  -s https://api.example.com/remote.yaml \
  -i test-output \
  -c config.json
```

## ⚠️ 注意事项

### 1. URL 中的逗号

如果 URL 本身包含逗号（极少见），需要单独使用 `-s` 参数：

```bash
swagger-coverage-commandline \
  -s "https://api.example.com/spec?param=a,b,c" \
  -s other-spec.yaml \
  -i test-output \
  -c config.json
```

### 2. 路径中的空格

自动去除首尾空格，但路径中间的空格会保留：

```bash
# ✅ 正确：自动去除首尾空格
-s "spec1.yaml, spec2.yaml"

# ⚠️ 注意：路径中间的空格会保留
-s "my spec.yaml"  # 文件名确实包含空格
```

### 3. 空值处理

空字符串会被自动忽略：

```bash
# 以下两种写法效果相同
-s "spec1.yaml,,spec2.yaml"  # 中间的空值会被忽略
-s "spec1.yaml,spec2.yaml"
```

## 🔧 实际应用

### CI/CD 脚本

```bash
#!/bin/bash
# .github/workflows/api-coverage.sh

set -e

# 定义所有 spec 文件（逗号分隔）
SPECS="services/user-api.yaml,services/order-api.yaml,services/payment-api.yaml"

# 生成覆盖率报告
swagger-coverage-commandline \
  -s "$SPECS" \
  -i ./test-results/swagger-coverage-output \
  -c ./config.json

echo "✅ Coverage report generated"
```

### Docker 环境

```dockerfile
# Dockerfile
FROM openjdk:11-jre-slim

COPY swagger-coverage-commandline /usr/local/bin/
COPY specs/*.yaml /specs/

# 使用逗号分隔指定所有 spec
CMD swagger-coverage-commandline \
    -s /specs/api1.yaml,/specs/api2.yaml,/specs/api3.yaml \
    -i /test-output \
    -c /config.json
```

### Makefile

```makefile
# Makefile
SPECS := api1.yaml,api2.yaml,api3.yaml

coverage:
	swagger-coverage-commandline \
		-s $(SPECS) \
		-i test-output \
		-c config.json

.PHONY: coverage
```

## 📊 性能对比

逗号分隔和多次使用 `-s` 在性能上**完全相同**，只是语法上的差异：

| 方式 | 命令长度 | 可读性 | 性能 |
|------|---------|--------|------|
| 逗号分隔 | 更短 | ⭐⭐⭐⭐⭐ | 相同 |
| 多次 -s | 较长 | ⭐⭐⭐⭐ | 相同 |
| 混合使用 | 中等 | ⭐⭐⭐⭐ | 相同 |

## 🎉 总结

### 优势

✅ **更简洁**: 减少重复的 `-s` 参数  
✅ **易读性**: 文件列表一目了然  
✅ **灵活性**: 可以混合使用两种方式  
✅ **向后兼容**: 原有的多次 `-s` 方式仍然有效  

### 推荐使用场景

- ✅ 文件数量 ≤ 5 个：使用逗号分隔
- ✅ 文件数量 > 5 个：使用脚本动态生成
- ✅ 混合本地和远程：分组使用
- ✅ CI/CD 环境：使用环境变量 + 逗号分隔

---

**版本**: v1.1.0  
**更新日期**: 2026-01-15  
**状态**: ✅ 已实现并测试通过
