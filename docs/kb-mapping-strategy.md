# kbId 映射策略说明

## 当前配置

文件：`docbase-admin/src/main/resources/application-dev.yml`

```yaml
docbase:
  ai:
    kb-mapping:
      default-kb-id: 5
      category-mappings:
        1: 5    # Study
        9: 5    # Java
```

## 默认知识库

`default-kb-id: 5`，对应 Python 侧 `knowledge_bases.id=5`（当前唯一有文档的知识库）。

任何未显式指定 kbId 且无法通过分类映射解析的问答请求，均默认路由到 kb_id=5。

## categoryId → kbId 映射表

| Java 分类 | category_id | → | Python kb_id | 说明 |
|-----------|-------------|---|-------------|------|
| Study | 1 | → | 5 | 根分类，所有文档的顶层归属 |
| Java | 9 | → | 5 | Study 的子分类 |

未在 `category-mappings` 中配置的分类 ID，不匹配任何映射，走 fallback-default。

## 解析优先级

1. 前端显式传入 `kbId` → 直接使用（最高优先级）
2. 前端传入 `documentId` → 查文档所属 `categoryId` → 查 category-mappings
3. 前端传入 `categoryId` → 查 category-mappings
4. 以上皆无 → 使用 `default-kb-id`

## 冲突检测

当请求同时携带 `documentId` 和 `categoryId`，但文档实际的 categoryId 与请求的 categoryId 不一致时，记录 WARNING 日志，仍优先使用 document 链路的解析结果。

## 扩展方式

后续新增知识库（如 kb_id=6）时，只需修改 yml：

```yaml
category-mappings:
  1: 5    # Study → 主知识库
  9: 5    # Java → 主知识库
  8: 6    # 大数据 → 新知识库(示例)
```

无需改代码、无需改数据库。
