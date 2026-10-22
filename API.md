# KnowledgeSystem 接口文档

> 前端（Vue 3 + Vite，端口 5173，`/api` 代理到后端 8080）与后端（Spring Boot 3.2.5）的全部 HTTP 接口说明。
> 更新日期：2026-10-13

## 目录

- [通用约定](#通用约定)
- [1. 认证模块 /api/auth](#1-认证模块-apiauth)
- [2. 文章模块 /api/articles](#2-文章模块-apiarticles)
- [3. 分类模块 /api/categories](#3-分类模块-apicategories)
- [4. 标签模块 /api/tags](#4-标签模块-apitags)
- [5. 评论模块 /api/comments](#5-评论模块-apicomments)
- [6. 收藏模块 /api/favorites](#6-收藏模块-apifavorites)
- [7. 合集模块 /api/collections](#7-合集模块-apicollections)
- [8. 充值/余额模块 /api/recharges](#8-充值余额模块-apirecharges)
- [9. 私信模块 /api/messages](#9-私信模块-apimessages)
- [10. 通知模块 /api/notifications](#10-通知模块-apinotifications)
- [11. 个人资料 /api/profile](#11-个人资料-apiprofile)
- [12. 用户管理 /api/users](#12-用户管理-apiusers)
- [13. AI 能力 /api/ai](#13-ai-能力-apiai)
- [附：权限矩阵](#附权限矩阵)

---

## 通用约定

### Base URL 与代理

| 环境 | 说明 |
|---|---|
| 开发 | 前端 `http://localhost:5173`，`/api/**` 由 Vite 代理到 `http://localhost:8080` |
| 生产 | 前端构建产物由任意静态服务器托管，`/api` 指向后端部署地址 |

### 统一响应结构 `Result<T>`

```json
{ "code": 200, "message": "success", "data": { } }
```

| code | 含义 |
|---|---|
| 200 | 成功 |
| 500 | 业务失败（message 为面向用户的中文提示） |
| 401 | 未登录或登录已过期（HTTP 状态码同为 401，前端据此清除 token 跳登录页） |
| 403 | 已登录但无权限（HTTP 状态码同为 403） |
| 404 | 接口不存在 |

### 认证方式

- 登录后返回 JWT，前端存 `sessionStorage.token`，请求头携带：`Authorization: Bearer <token>`。
- JWT 24 小时有效；服务端解析出 `userId` 后从 Redis 缓存（60 秒 TTL）读取 `role|status|deleted`，未命中查库回填。
- 角色：`USER` / `ADMIN` / `SUPER_ADMIN`。方法级权限注解：`@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")` 等。

### 分页约定

分页接口统一返回 `Page<T>`：`{ "records": [...], "total": 100, "size": 10, "current": 1, "pages": 10 }`。
请求参数统一为 `page`（页码，从 1 开始，默认 1）、`size`（每页条数，各接口默认不同）。

### 全局校验与异常

- 请求体用 `jakarta.validation` 校验（`@Valid`），校验失败返回第一条字段错误的中文提示（code 500）。
- 所有业务异常由 `GlobalExceptionHandler` 统一兜底，不泄露内部细节。

---

## 1. 认证模块 /api/auth

**全部公开（SecurityConfig permitAll）。**

### 1.1 登录

`POST /api/auth/login`

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| username | String | 是 | 用户名 |
| password | String | 是 | 密码 |

成功返回 `data`（LoginVO）：

| 字段 | 说明 |
|---|---|
| token | JWT（放 Authorization 头） |
| userId / username / nickname / role / avatar | 用户信息 |
| balance | 当前余额 |

**安全规则**：同一用户名连续输错 5 次密码，锁定 15 分钟（Redis Lua 计数，固定窗口）；用户不存在与密码错误统一提示"用户名或密码错误"（防用户名枚举）；账号被拉黑（status=1）提示"您的账号已被拉黑"。

### 1.2 注册

`POST /api/auth/register`

| 参数 | 类型 | 必填 | 校验 |
|---|---|---|---|
| username | String | 是 | 3-20 位 |
| password | String | 是 | 6-20 位 |
| nickname | String | 否 | 缺省用 username |
| email | String | 是 | 邮箱格式 |
| code | String | 是 | 6 位数字验证码 |

**规则**：同一邮箱最多注册 5 个账号；验证码 5 分钟有效，最多输错 5 次（超次作废需重发）。

### 1.3 发送注册验证码

`POST /api/auth/send-code`

| 参数 | 类型 | 必填 |
|---|---|---|
| email | String | 是 |

**限流**：同一邮箱 60 秒冷却；同一 IP 每小时最多 10 封（防轰炸）。

### 1.4 发送重置密码验证码

`POST /api/auth/reset-code`

| 参数 | 类型 | 必填 |
|---|---|---|
| username | String | 是 |
| email | String | 是 |

**规则**：校验账号存在且邮箱与账号绑定邮箱一致，否则提示"这个邮箱貌似不是您的呢喵~"。

### 1.5 重置密码

`POST /api/auth/reset-password`

| 参数 | 类型 | 必填 |
|---|---|---|
| username | String | 是 |
| email | String | 是 |
| code | String | 是（6 位数字） |
| password | String | 是（6-20 位） |

---

## 2. 文章模块 /api/articles

**GET 公开；POST/PUT/DELETE 需登录**（列表与详情未登录也可浏览，付费正文锁定）。

### 2.1 文章列表（分页 + 筛选）

`GET /api/articles?page=1&size=10&categoryId=&tagId=&keyword=&sort=&freeOnly=`

- `categoryId`：按分类筛选；`tagId`：按标签筛选；`keyword`：标题/摘要关键词。
- `sort`：`latest` 按发布时间倒序；`hot` 按浏览量倒序；不传时保持原有按点赞数倒序。
- `freeOnly`：为 `true` 时只返回免费文章（price 为 0 或 null）。
- 返回 `Page<ArticleVO>`（列表不含正文 content，仅摘要）。

### 2.2 文章详情

`GET /api/articles/{id}`

- 每次调用自动增加阅读数。
- 返回 `ArticleVO`：含 `content`（正文）、`locked`（是否锁定）、`purchased`、`liked`、`authorId`、`categoryId`、`tags` 等。
- **付费权限**：免费文章 / 作者本人 / 管理员 / 已购文章 / 已购含该文章的合集 → `locked=false`；否则 `content=null` 且 `locked=true`。

### 2.3 我的文章

`GET /api/articles/my?page=&size=` —— 登录，返回当前用户自己的文章（供合集选文）。

### 2.4 发布文章

`POST /api/articles`

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| title | String | 是 | 标题 |
| summary | String | 否 | 摘要（AI 可生成） |
| content | String | 是 | 正文 |
| categoryId | Long | 是 | 必须是**叶子分类**（无子分类） |
| price | BigDecimal | 否 | 售价，null/0 为免费，最多两位小数 |
| tagIds | List\<Long\> | 否 | 标签 ID 列表 |

发布后：事务提交后异步重建该文章向量索引（入 AI 问答检索池）。

### 2.5 编辑文章

`PUT /api/articles/{id}` —— 参数同 2.4。仅作者或管理员；分类变化时重新校验叶子分类；事务提交后异步重建索引。

### 2.6 删除文章

`DELETE /api/articles/{id}` —— 仅作者或管理员。逻辑删除文章 + 级联清理（评论逻辑删除；收藏/点赞/文章-标签/合集收录物理删除）；**购买记录与余额流水保留**（审计 + 不退款产品决策）；事务提交后异步清空索引。

### 2.7 购买付费文章

`POST /api/articles/{id}/purchase` —— 登录。

- 免费文章不可购买；不能购买自己的文章；重复购买拦截。
- 事务内：校验余额 → 扣买家余额、加作者余额 → 写 `article_purchase` → 写双方 `balance_log`。
- 返回 `data`：买家最新余额。

### 2.8 点赞

`POST /api/articles/{id}/like` —— 登录。重复点赞拦截（唯一索引），维护文章 `like_count` 计数。

---

## 3. 分类模块 /api/categories

**GET 公开；写操作仅 ADMIN/SUPER_ADMIN。**

### 3.1 分类列表

`GET /api/categories` —— 返回全部分类树（含 `parentId`、`depth`、`hasChildren`、`articleCount`）。

### 3.2 创建分类

`POST /api/categories`（管理员）—— `{ name, parentId(0/null=顶级), sort }`。
**AI 联动**：父分类下新增第一个子分类时，发布 `CategoryChildAddedEvent`，触发 AI 把父分类下的文章自动重分类到新子分类（异步）。

### 3.3 修改分类

`PUT /api/categories/{id}`（管理员）—— 参数同创建。

### 3.4 删除分类

`DELETE /api/categories/{id}`（管理员）—— 有子分类或有文章时拒绝删除。

---

## 4. 标签模块 /api/tags

**GET 公开；写操作仅 ADMIN/SUPER_ADMIN。**

- `GET /api/tags`：全部标签（含每标签文章数）。
- `POST /api/tags`：创建（`{name}`，重名拒绝）。
- `PUT /api/tags/{id}`：改名。
- `DELETE /api/tags/{id}`：删除，同时物理删除所有文章上的该标签关联。

---

## 5. 评论模块 /api/comments

**GET 公开；POST/DELETE 需登录。**

### 5.1 评论列表

`GET /api/comments/article/{articleId}` —— 公开，返回 `List<CommentVO>`（含评论人昵称/头像、回复关系 parentId）。

### 5.2 发表评论

`POST /api/comments`

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| articleId | Long | 是 | |
| content | String | 是 | 评论内容 |
| parentId | Long | 否 | 回复某条评论时传其 id |

发表后维护文章 `comment_count`。

### 5.3 删除评论

`DELETE /api/comments/{id}` —— 评论作者或管理员。

---

## 6. 收藏模块 /api/favorites

**全部需登录。**

- `POST /api/favorites/toggle/{articleId}`：收藏/取消收藏（幂等切换）。
- `GET /api/favorites/check/{articleId}`：返回是否已收藏 `Boolean`。
- `GET /api/favorites/my`：我的收藏列表 `List<FavoriteVO>`。

---

## 7. 合集模块 /api/collections

**广场/详情公开浏览；写操作需登录。**

### 7.1 合集广场

`GET /api/collections?page=1&size=12` —— 公开，返回 `Page<CollectionVO>`（按当前用户标记 `own`/`purchased`）。

### 7.2 我的合集

`GET /api/collections/my` —— 登录，返回 `List<CollectionVO>`。

### 7.3 合集详情

`GET /api/collections/{id}` —— 公开，含文章列表（未购合集时付费文章正文同样锁定）。

### 7.4 创建/修改/删除合集

- `POST /api/collections`：`{ name(≤50字), description(≤200字), price }`。
- `PUT /api/collections/{id}`：仅合集所有者。
- `DELETE /api/collections/{id}`：仅所有者；**删除时校验：已售出的付费合集不可删除**（保护买家权益）。

### 7.5 合集内文章管理

- `POST /api/collections/{id}/articles`：`{ articleId }`，仅所有者，且只能收录**自己的**文章。
- `DELETE /api/collections/{id}/articles/{articleId}`：移除。

### 7.6 购买合集

`POST /api/collections/{id}/purchase` —— 登录。免费/自己的/已购拦截；事务内扣款 + 记录 `collection_purchase` + 双方流水；返回买家最新余额。
购买后，合集内所有付费文章对该买家解锁（详情接口的 covered 判断）。

---

## 8. 充值/余额模块 /api/recharges

**全部需登录。**

- `GET /api/recharges/balance`：当前余额。
- `GET /api/recharges/logs?page=&size=`：余额流水（收支明细，含 changeAmount/balanceAfter/type/remark）。
- `POST /api/recharges`：充值（**模拟支付，立即到账**），`{ amount: 0.01~10000 }`，事务内加余额 + 充值记录 + 流水；返回最新余额。
- `GET /api/recharges?page=&size=`：我的充值记录。

---

## 9. 私信模块 /api/messages

**全部需登录。**

- `POST /api/messages`：发私信，`{ receiverId, content(≤500字) }`。**拉黑校验**：任一方拉黑对方时拒绝发送。
- `GET /api/messages/conversations`：会话列表 `List<ConversationVO>`（对方昵称/头像、最后一条消息、未读数、是否拉黑）。
- `GET /api/messages/conversation/{peerId}?page=&size=50`：与某人的消息分页；**打开即已读**。
- `GET /api/messages/unread-count`：未读私信总数（聊天小窗红点轮询用）。
- `POST /api/messages/block/{userId}`：拉黑。
- `DELETE /api/messages/block/{userId}`：解除拉黑。

---

## 10. 通知模块 /api/notifications

**全部需登录。**

- `GET /api/notifications?page=&size=10`：我的通知（购买成功、文章被买、被点赞等系统通知）。
- `GET /api/notifications/unread-count`：未读通知数。
- `PUT /api/notifications/{id}/read`：单条已读。
- `PUT /api/notifications/read-all`：全部已读。

---

## 11. 个人资料 /api/profile

**需登录。**

- `GET /api/profile`：自己的完整资料（昵称/邮箱/头像/余额等，服务端按 id 重新查库，不信任 JWT 里的快照）。
- `PUT /api/profile`：`{ nickname(≤50), email(格式校验), avatar }`。

---

## 12. 用户管理 /api/users

**整个 Controller 仅 SUPER_ADMIN 可访问。**

- `GET /api/users?page=&size=&keyword=`：用户分页（关键词搜用户名/昵称）。
- `PUT /api/users/{id}/role`：`{ role: "USER"|"ADMIN" }`（SUPER_ADMIN 不可被降级）。
- `PUT /api/users/{id}/status`：`{ status: 0|1 }`（1=拉黑封禁）。
- `PUT /api/users/{id}/balance`：`{ change: ±10000 内, remark(≤200) }` 管理员调余额。

角色/状态变更后**主动删除 Redis 认证缓存**，新权限立即生效（60 秒 TTL 兜底）。

---

## 13. AI 能力 /api/ai

**全部需登录**（`/api/ai/**` 不在 permitAll 清单）。

### 13.1 生成文章摘要（同步）

`POST /api/ai/summary` —— `{ title, content }` → `{ summary }`（≤100 字）。
**消耗 1 次每日配额**；DeepSeek 生成，prompt 要求纯 JSON 输出后手动解析。

### 13.2 推荐标签（同步）

`POST /api/ai/tags` —— `{ title, content }` → `List<TagVO>`（1~5 个，只从现有标签库选，AI 编造的名字被忽略）。

### 13.3 RAG 流式问答（SSE）

`POST /api/ai/chat` —— `{ question(≤500字), history:[{role,content}] }`

- 响应 `Content-Type: text/event-stream`，每条 `data:` 是 JSON 事件：

| type | 载荷 | 说明 |
|---|---|---|
| `status` | `{message}` | 阶段提示："正在检索知识库…" / "正在生成回答…" |
| `articles` | `{data:[{articleId,title,summary}]}` | 检索命中的候选文章（前端作推荐卡片对照表） |
| `token` | `{content}` | 流式回答片段 |
| `done` | - | 生成完毕（前端收到即收尾） |
| `error` | `{message}` | 出错提示 |

- **流程**：每用户每分钟 20 次限流（Redis Lua）→ 向量检索 Top-5 分块 → 组装提示词（含【文章ID】标注 + 摘要）→ DeepSeek 流式生成 → AI 按提示词在末尾输出推荐 JSON → 前端剥离 JSON 并渲染"推荐阅读"卡片。
- **业务错误（非 SSE）**：限流、配额耗尽、参数错误等发生在流建立前时，返回普通 JSON 错误（前端按 Content-Type 区分处理）。

### 13.4 查询今日配额

`GET /api/ai/quota` → `{ used, dailyLimit, remaining }`（默认 30 次/天，key 自带日期跨天清零）。

### 13.5 重建向量索引（管理员）

`POST /api/ai/rebuild-index` —— 仅 ADMIN/SUPER_ADMIN。异步全量重建（逐篇 embedding + 篇间 300ms 间隔，单篇失败跳过继续），接口立即返回。

---

## 附：权限矩阵

| 模块/操作 | 游客 | USER | ADMIN | SUPER_ADMIN |
|---|:-:|:-:|:-:|:-:|
| 浏览文章/评论/分类/标签/合集广场 | ✅ | ✅ | ✅ | ✅ |
| 注册/登录/找回密码 | ✅ | ✅ | ✅ | ✅ |
| 发布/编辑/删除文章 | ❌ | 仅自己的 | ✅ 全部 | ✅ 全部 |
| 购买文章/合集、充值、收藏、点赞、评论、私信、通知 | ❌ | ✅ | ✅ | ✅ |
| 删除评论 | ❌ | 仅自己的 | ✅ | ✅ |
| 创建/修改/删除合集 | ❌ | 仅自己的 | 仅自己的 | 仅自己的 |
| 分类/标签管理 | ❌ | ❌ | ✅ | ✅ |
| AI 摘要/标签/问答/配额 | ❌ | ✅（每日配额） | ✅ | ✅ |
| 重建向量索引 | ❌ | ❌ | ✅ | ✅ |
| 用户管理（角色/封禁/调余额） | ❌ | ❌ | ❌ | ✅ |
