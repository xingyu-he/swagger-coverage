# 排除 API 功能说明 (Exclude Operations Feature)

## 功能概述

这个功能允许用户在配置文件中指定需要排除的 API 操作，这些被排除的 API 将不会被统计到 "Empty Coverage" (零调用/未覆盖的 API) 中。

这对于以下场景非常有用：
- 内部调试接口
- 健康检查端点
- 管理员专用接口
- 计划废弃但暂未移除的接口
- 不需要测试覆盖的特定接口

## 配置方法

在配置文件（通常是 `configuration.json`）中添加 `excludedOperations` 字段：

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

## 支持的匹配模式

### 1. 精确路径匹配
```json
"excludedOperations": [
  "/api/users"
]
```
匹配所有 HTTP 方法的 `/api/users` 路径

### 2. 指定 HTTP 方法 + 路径
```json
"excludedOperations": [
  "GET /api/users",
  "POST /api/admin"
]
```
仅匹配指定 HTTP 方法和路径的组合

### 3. 单层通配符 (*)
```json
"excludedOperations": [
  "/api/internal/*"
]
```
匹配：
- ✅ `/api/internal/debug`
- ✅ `/api/internal/status`
- ❌ `/api/internal/sub/path` (不匹配多层路径)

### 4. 多层通配符 (**)
```json
"excludedOperations": [
  "/api/admin/**"
]
```
匹配：
- ✅ `/api/admin/users`
- ✅ `/api/admin/sub/path`
- ✅ `/api/admin/very/deep/path`

### 5. 组合使用
```json
"excludedOperations": [
  "GET /api/health",
  "GET /api/metrics",
  "/api/internal/*",
  "/actuator/**",
  "POST /api/debug/*"
]
```

## 实际使用示例

### 示例配置文件
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

### 命令行使用
```bash
java -jar swagger-coverage-commandline.jar \
  -s /path/to/swagger.yaml \
  -i /path/to/swagger-coverage-output \
  -c /path/to/configuration.json
```

## 工作原理

1. **配置加载**：在启动时，系统会读取配置文件中的 `excludedOperations` 列表
2. **统计过滤**：在 `ZeroCallStatisticsBuilder` 构建零调用统计时，会检查每个操作是否匹配排除模式
3. **路径匹配**：使用 Spring AntPathMatcher 进行路径模式匹配，支持 `*` 和 `**` 通配符
4. **结果生成**：被排除的 API 不会出现在 "Empty Coverage" 报告中

## 注意事项

1. **大小写敏感**：HTTP 方法会被转换为大写进行匹配（GET、POST、PUT 等）
2. **路径格式**：路径应该与 Swagger/OpenAPI 规范中定义的路径格式保持一致
3. **性能影响**：排除规则会在统计时进行检查，大量规则可能会略微影响性能
4. **日志输出**：被排除的操作会在 DEBUG 日志级别输出，便于调试

## 调试

启用 DEBUG 日志以查看哪些操作被排除：

```bash
java -jar swagger-coverage-commandline.jar \
  -s /path/to/swagger.yaml \
  -i /path/to/swagger-coverage-output \
  -c /path/to/configuration.json \
  --verbose
```

日志输出示例：
```
DEBUG - Operation [GET /api/health] is excluded by pattern [GET /api/health]
DEBUG - Operation [GET /api/internal/debug] is excluded by pattern [/api/internal/*]
```

## 技术实现细节

### 修改的文件

1. **ConfigurationOptions.java**
   - 添加 `excludedOperations` 字段
   - 添加 getter/setter 方法

2. **Configuration.java**
   - 添加 `getExcludedOperations()` 方法

3. **ZeroCallStatisticsBuilder.java**
   - 添加 `isExcluded()` 方法实现排除逻辑
   - 支持多种匹配模式
   - 使用 AntPathMatcher 进行路径匹配

### 匹配规则算法

```java
private boolean isExcluded(OperationKey operation) {
    // 1. 解析排除规则
    // 2. 分离 HTTP 方法和路径
    // 3. 使用 AntPathMatcher 进行模式匹配
    // 4. 返回匹配结果
}
```

## 版本兼容性

- 向后兼容：如果配置文件中没有 `excludedOperations` 字段，功能不会影响现有行为
- 空列表：如果 `excludedOperations` 为空数组，等同于未配置
- JSON 格式：必须是字符串数组

## 未来增强

可能的功能扩展：
- 支持正则表达式匹配
- 支持基于标签 (tags) 的排除
- 支持基于响应状态码的排除
- 支持从外部文件加载排除规则
