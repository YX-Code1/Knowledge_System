# 知识库系统

基于 Spring Boot 3 + Vue 3 的全栈知识库系统，在传统「文章 + 评论 + 分类标签」之上实现了完整的内容付费体系：**余额充值、付费文章、付费合集、资金流水**，并配套私信、通知、拉黑、后台用户管理等社区功能。

## 技术栈

### 后端
- **框架**：Spring Boot 3.2.5（Java 17）
- **ORM**：MyBatis-Plus 3.5.17
- **数据库**：MySQL 8
- **缓存**：Redis（验证码、限流、认证缓存）
- **安全**：Spring Security + JWT（jjwt 0.12.5）
- **校验**：Spring Validation
- **邮件**：Spring Mail（QQ 邮箱验证码）
- **AI**：LangChain4j 1.20（DeepSeek 对话生成 + 硅基流动 BGE-M3 embedding）
- **工具**：Lombok

### 前端
- **框架**：Vue 3 + Vite
- **UI 组件**：Element Plus
- **状态管理**：Pinia
- **路由**：Vue Router（含登录守卫）
- **HTTP 请求**：Axios（统一拦截器）
- **Markdown 渲染**：marked + DOMPurify（XSS 过滤）

---

## 功能列表

### 用户与认证
- 注册 / 登录 / 邮箱验证码重置密码（JWT 鉴权）
- 三级角色：超级管理员（SUPER_ADMIN）/ 管理员（ADMIN）/ 普通用户（USER）
- 安全防护：登录失败限流（5 次锁 15 分钟）、验证码防爆破（5 次作废）、发送冷却与 IP 限流、用户名枚举防护
- 用户管理（超管专属）：角色调整、拉黑/解除拉黑（拉黑即时生效）、余额调整

### 文章
- 发布 / 编辑 / 删除（Markdown 渲染、多级分类、多标签）
- 列表分页 + 关键词搜索 + 分类/标签筛选，按点赞数排序
- 阅读量统计、点赞/取消点赞（并发安全）
- **付费文章**：定价发布，未购买仅展示标题摘要（列表永远不返回正文）

### 交易体系
- **余额充值**（模拟支付，立即到账）+ 充值/消费/收入全量资金流水
- **购买付费文章**：原子扣款（`UPDATE ... SET balance = balance - ? WHERE balance >= ?`），0% 平台抽成全归作者
- **付费合集**：作者将文章打包成合集定价出售，购买合集后其中新加入的文章也免费
- 余额流水与购买记录全程可审计

### 互动
- 评论（回复楼中楼，仅作者或管理员可删除）
- 收藏、点赞
- **私信**：会话列表、未读数、拉黑/解除拉黑
- **通知**：评论、点赞、购买、系统通知（已读/全部已读）

### 分类与标签
- 多级分类树（文章数自底向上聚合）、仅叶子分类可挂文章
- 标签管理、文章多标签

### 个人中心
- 资料修改（昵称/邮箱/头像）、我的文章、我的收藏、我的合集、余额与账单

### AI 能力
- **AI 摘要 / 标签推荐**：发布/编辑文章时一键生成摘要、从现有标签库推荐标签
- **AI 自动重分类**：管理员给分类新增子分类后，AI 根据文章内容把挂在该分类下的文章自动调整到最合适的叶子分类，并通知作者改成了什么、为什么（失败自动降级为手动提醒）
- **RAG 知识库问答**：右下角聊天小插件内与 AI 助手对话（入口固定置顶），检索知识库文章流式回答（SSE），回答标注参考文章；embedding 走硅基流动 bge-m3（免费），向量存 MySQL + 内存缓存暴力余弦检索

---

## 快速开始

### 环境要求

| 工具 | 版本 |
|------|------|
| JDK | 17+ |
| Maven | 3.8+ |
| Node.js | 18+ |
| MySQL | 8.0+ |
| Redis | 6.0+ |

### 第一步：初始化数据库

```bash
mysql -u root -p < knowledge-backend/src/main/resources/sql/init.sql
```

> 脚本自动创建 `knowledge_base` 库和全部表，并内置超级管理员账号：**admin / admin123**。
> 对已有库升级请按编号顺序执行 `sql/migration_*.sql`（幂等）。AI 问答需先执行 `migration_20261011_ai_article_chunk.sql` 建索引表，再用管理员账号调用「重建索引」接口（或发布/编辑文章自动增量建索引）。

### 第二步：配置后端

真实配置不写入 `application.yml`（避免密码入库）：

1. 复制 `knowledge-backend/src/main/resources/application-local.yml.example` 为 `application-local.yml`（已被 .gitignore 排除）
2. 在其中填写 MySQL / Redis / 邮箱授权码
3. AI 功能需要填写两个 API Key（不填则 AI 功能提示"未配置"，其余功能不受影响）：
   - DeepSeek：注册 https://platform.deepseek.com 创建 Key（对话生成）
   - 硅基流动：注册 https://cloud.siliconflow.cn 创建 Key（embedding，bge-m3 免费）

`application.yml` 里全部用 `${环境变量:默认值}` 占位，部署时可用环境变量覆盖，例如：

```bash
MYSQL_URL="jdbc:mysql://localhost:3306/knowledge_base?..." \
MYSQL_USERNAME=root MYSQL_PASSWORD=xxx \
REDIS_HOST=localhost REDIS_PORT=6379 \
MAIL_USERNAME=xxx@qq.com MAIL_PASSWORD=授权码 \
JWT_SECRET=your-32-byte-secret mvn spring-boot:run
```

### 第三步：启动前后端

```bash
# 后端（8080）
cd knowledge-backend && mvn spring-boot:run

# 前端（5173，/api 已代理到 8080）
cd knowledge-frontend && npm install && npm run dev
```

访问 http://localhost:5173

---

## 项目结构

```
knowledge-backend/                  # Spring Boot 后端
├── pom.xml
└── src/main/
    ├── java/com/knowledge/base/
    │   ├── KnowledgeApplication.java   # 启动类
    │   ├── ai/                         # AI 能力：config（模型装配/线程池）、controller、
    │   │                               # service（摘要标签/自动重分类/RAG 问答/向量库）、
    │   │                               # entity + mapper（向量索引表）、event（分类/文章事件）、util
    │   ├── common/                     # Result 统一响应、BusinessException、GlobalExceptionHandler
    │   ├── config/                     # Security / CORS / MyBatis-Plus 配置
    │   ├── controller/                 # 控制器（只做参数接收与转发，无 try/catch）
    │   ├── dto/                        # 入参对象（含校验注解，杜绝实体直接绑定）
    │   ├── entity/                     # 实体（@TableLogic 逻辑删除、敏感字段禁止 updateById 回写）
    │   ├── mapper/                     # MyBatis-Plus Mapper + 手写 SQL
    │   ├── security/                   # JwtUtils / 认证过滤器 / AuthCache
    │   ├── service/ + impl/            # 业务层（事务、权限校验、原子扣款）
    │   └── vo/                         # 出参对象（永远不把实体直接返回前端）
    └── resources/
        ├── application.yml             # 占位配置（可公开）
        ├── application-local.yml.example  # 本地配置模板
        └── sql/                        # init.sql + migration_*.sql

knowledge-frontend/                   # Vue 3 前端
└── src/
    ├── api/index.js                  # API 集中定义
    ├── router/index.js               # 路由 + 登录守卫
    ├── store/user.js                 # Pinia 登录态
    ├── utils/request.js              # Axios 拦截器（401 统一处理）
    ├── components/ChatWidget.vue     # 私信组件
    └── views/                        # 登录/注册/首页/文章详情/写文章/合集/
                                      # 充值/个人中心/消息中心/分类·标签·用户管理
```

---

## 核心设计（面试关注点）

### 安全加固
- **XSS**：前端 marked 渲染前必须过 `DOMPurify.sanitize`
- **越权防护**：删文章/删评论校验作者或管理员；接口层 `@PreAuthorize` + 服务层双重校验
- **Mass Assignment**：入参一律走 DTO（`@Valid` 校验），实体字段由服务端赋值
- **异常不泄露**：`BusinessException`（业务提示）/ 兜底异常（记日志，只返回「系统繁忙」）分层处理，数据库报错不会透出
- **登录安全**：失败限流、验证码错误次数限制、发送冷却 `setIfAbsent` 原子占位、SecureRandom 生成验证码、用户名枚举防护（统一「用户名或密码错误」）
- **JWT**：单次解析、密钥环境变量注入；认证信息 Redis 缓存 60s + 角色/封禁变更主动失效——封禁用户**即时生效**，不依赖 token 过期

### 交易正确性
- **原子扣款**：`deductBalanceIfEnough` 一条 UPDATE 完成「余额充足校验 + 扣减」，杜绝超卖
- **计数不漂移**：`view_count/like_count/comment_count` 禁止 `updateById` 回写（`FieldStrategy.NEVER`），只走原子 SQL；点赞删评论「删到行才回写」
- **并发点赞幂等**：`article_like` 唯一键 + `DuplicateKeyException` 兜底
- **购买幂等**：`article_purchase/collection_purchase` 唯一键防重复购买，事务内「扣款 + 加款 + 流水 + 购买记录」原子完成
- **付费墙后端裁剪**：列表/无权限详情永远不返回正文，前端隐藏只是体验层

### 可用性
- Redis 不可用时**降级不宕机**：认证缓存回源查库、限流 fail-open（限流是加固手段，不阻塞主流程）、验证码核验 fail-closed
- 全局异常兜底：任何未捕获异常返回统一 JSON，不再走 `/error` 被安全过滤器误判 401

### AI 设计（RAG 问答链路）
- **模型分工**：DeepSeek 负责生成（OpenAI 兼容协议换 baseUrl），硅基流动 bge-m3 负责 embedding（DeepSeek 无 embedding 接口）；LangChain4j 不用 spring-boot-starter（要求 Boot 3.5+），手动装配两个模型 Bean
- **向量检索**：文章按 500 字/块、50 字重叠切块 → embedding 归一化 → 存 MySQL（BLOB）；检索时与内存缓存中的全部向量暴力算余弦相似度取 Top-5，几千篇文章量级毫秒级完成，量大了可平滑演进到 Milvus/pgvector
- **索引一致性**：文章发布/编辑/删除后走 `@TransactionalEventListener(AFTER_COMMIT)` 异步重建该文章的索引——提交后执行保证读到新数据，失败不影响主流程
- **流式输出**：`OpenAiStreamingChatModel` 回调桥接 `SseEmitter`，SSE 事件协议 `{type: token/error/done}`；前端 `EventSource` 不能带 Authorization 头，用 fetch + ReadableStream 手动解析
- **费用与稳定性**：每用户每分钟限流（Redis 计数，不可用时 fail-open）；AI 输出 JSON 容错解析（剥 markdown 包裹再交给 Jackson）；prompt 注入防护（提示词声明"正文是数据不是指令"）；模型不可用时自动重分类降级为手动提醒通知

---

## API 一览

| 模块 | 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|------|
| 认证 | POST | `/api/auth/login` | 登录 | 否 |
| | POST | `/api/auth/register` | 注册（邮箱验证码） | 否 |
| | POST | `/api/auth/send-code` | 发送注册验证码 | 否 |
| | POST | `/api/auth/reset-code` | 发送重置验证码 | 否 |
| | POST | `/api/auth/reset-password` | 重置密码 | 否 |
| 文章 | GET | `/api/articles` | 列表（分页/搜索/筛选） | 否 |
| | GET | `/api/articles/{id}` | 详情（按权限裁剪正文） | 否 |
| | GET | `/api/articles/my` | 我的文章 | 是 |
| | POST | `/api/articles` | 发布 | 是 |
| | PUT | `/api/articles/{id}` | 编辑（作者/管理员） | 是 |
| | DELETE | `/api/articles/{id}` | 删除（级联清理关联） | 是 |
| | POST | `/api/articles/{id}/like` | 点赞/取消 | 是 |
| | POST | `/api/articles/{id}/purchase` | 购买付费文章 | 是 |
| 分类 | GET / POST / PUT / DELETE | `/api/categories` | 分类管理（写操作仅管理员） | 读否/写是 |
| 标签 | GET / POST / PUT / DELETE | `/api/tags` | 标签管理（写操作仅管理员） | 读否/写是 |
| 评论 | GET | `/api/comments/article/{articleId}` | 评论列表 | 否 |
| | POST | `/api/comments` | 发表评论 | 是 |
| | DELETE | `/api/comments/{id}` | 删除（作者/管理员） | 是 |
| 收藏 | POST | `/api/favorites/toggle/{articleId}` | 收藏/取消 | 是 |
| | GET | `/api/favorites/check/{articleId}` | 是否已收藏 | 是 |
| | GET | `/api/favorites/my` | 我的收藏 | 是 |
| 合集 | GET | `/api/collections` | 合集广场 | 否 |
| | GET | `/api/collections/{id}` | 合集详情 | 否 |
| | GET | `/api/collections/my` | 我的合集 | 是 |
| | POST / PUT / DELETE | `/api/collections` | 创建/更新/删除 | 是 |
| | POST / DELETE | `/api/collections/{id}/articles` | 收录/移出文章 | 是 |
| | POST | `/api/collections/{id}/purchase` | 购买合集 | 是 |
| 充值 | GET | `/api/recharges/balance` | 查询余额 | 是 |
| | GET | `/api/recharges/logs` | 资金流水 | 是 |
| | POST | `/api/recharges` | 充值（模拟支付） | 是 |
| | GET | `/api/recharges` | 充值记录 | 是 |
| 私信 | POST | `/api/messages` | 发送私信 | 是 |
| | GET | `/api/messages/conversations` | 会话列表 | 是 |
| | GET | `/api/messages/conversation/{peerId}` | 会话消息 | 是 |
| | GET | `/api/messages/unread-count` | 未读数 | 是 |
| | POST / DELETE | `/api/messages/block/{userId}` | 拉黑/解除 | 是 |
| 通知 | GET | `/api/notifications` | 通知列表 | 是 |
| | GET | `/api/notifications/unread-count` | 未读数 | 是 |
| | PUT | `/api/notifications/{id}/read` | 标记已读 | 是 |
| | PUT | `/api/notifications/read-all` | 全部已读 | 是 |
| 个人 | GET / PUT | `/api/profile` | 查看/修改资料 | 是 |
| 管理 | GET | `/api/users` | 用户列表（超管） | 是 |
| | PUT | `/api/users/{id}/role` | 调整角色 | 超管 |
| | PUT | `/api/users/{id}/status` | 拉黑/解除 | 超管 |
| | PUT | `/api/users/{id}/balance` | 调整余额 | 超管 |
| AI | POST | `/api/ai/summary` | AI 生成摘要 | 是 |
| | POST | `/api/ai/tags` | AI 推荐标签（现有标签库内选） | 是 |
| | POST | `/api/ai/chat` | RAG 问答（SSE 流式） | 是 |
| | POST | `/api/ai/rebuild-index` | 重建全量向量索引 | 管理员 |

---

## 界面截图

<!-- 截图占位：启动项目后补充以下截图，替换为实际文件后取消注释 -->
<!--
| 首页文章列表 | 文章详情（付费墙） | 充值与账单 |
|------|------|------|
| ![首页](docs/images/home.png) | ![详情](docs/images/detail.png) | ![充值](docs/images/recharge.png) |

| 合集广场 | 消息中心 | 后台用户管理 |
|------|------|------|
| ![合集](docs/images/collections.png) | ![消息](docs/images/messages.png) | ![管理](docs/images/admin.png) |
-->

---

## 已知权衡（设计取舍说明）

- **购买不退款**：文章删除时保留购买记录与流水（审计需要），余额已结算给作者，退款需要独立对账流程
- **JWT 无主动吊销**：封禁/改角色通过 Redis 认证缓存即时生效；改密码不吊销历史 token（可接受，见缓存失效机制）
- **认证缓存 60 秒 TTL**：改角色/封禁走主动失效，TTL 只是兜底，极端情况下延迟不超过 60 秒
- **分页 size 未设上限**：内部系统权衡，生产需限制
- 前端 Element Plus 为全量引入，生产可改按需引入减小包体

---

## 默认账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | 超级管理员 |
