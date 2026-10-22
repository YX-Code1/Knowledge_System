# KnowledgeSystem 功能完整链路

> 每个功能从「用户操作」到「数据落库/返回」的完整调用链，含关键设计决策。
> 更新日期：2026-10-13

## 目录

1. [认证体系：登录 / 注册 / 找回密码](#1-认证体系登录--注册--找回密码)
2. [JWT 认证与 Redis 认证缓存](#2-jwt-认证与-redis-认证缓存)
3. [文章：发布 → 编辑 → 删除 → 索引联动](#3-文章发布--编辑--删除--索引联动)
4. [付费体系：充值 / 购买文章 / 购买合集](#4-付费体系充值--购买文章--购买合集)
5. [AI 能力：摘要 / 标签 / 自动重分类](#5-ai-能力摘要--标签--自动重分类)
6. [AI RAG 问答：检索 → 提示词 → SSE → 推荐卡片](#6-ai-rag-问答检索--提示词--sse--推荐卡片)
7. [向量索引：启动自愈 / 手动重建 / 增量维护](#7-向量索引启动自愈--手动重建--增量维护)
8. [私信与拉黑](#8-私信与拉黑)
9. [通知](#9-通知)
10. [评论 / 点赞 / 收藏](#10-评论--点赞--收藏)
11. [合集](#11-合集)
12. [管理员链路：角色 / 封禁 / 调余额](#12-管理员链路角色--封禁--调余额)
13. [前端聊天小窗（全局挂载 + 实时更新）](#13-前端聊天小窗全局挂载--实时更新)
14. [限流与降级全景图](#14-限流与降级全景图)

---

## 1. 认证体系：登录 / 注册 / 找回密码

### 1.1 注册链路

```
填写表单（用户名/密码/邮箱/验证码）
 → POST /api/auth/register
 → AuthServiceImpl.register
   ├─ 查用户名重复 → 重复则"用户名已存在"
   ├─ 统计该邮箱已注册账号数 ≥5 → 拒绝
   ├─ verifyCodeWithAttemptLimit：读 Redis email:code:{email}
   │   ├─ Redis 不可用 → fail-closed 拒绝（"验证码服务暂不可用"）
   │   ├─ 过期/不存在 → "验证码已过期"
   │   ├─ 输错次数 ≥5 → 作废验证码
   │   ├─ 不匹配 → Lua 原子自增 email:code:try:{email}（滑动窗口续期）
   │   └─ 匹配 → 删除验证码与计数（防重复使用）
   └─ BCrypt 加密密码入库，角色默认 USER
```

**验证码发送链路**（注册/找回共用）：

```
POST /api/auth/send-code {email}
 → 两层限流（Redis Lua 原子自增，防 check-then-act 竞态）：
   ① IP 限流：email:code:ip:{ip}，每小时 10 封（固定窗口）
   ② 邮箱冷却：email:code:cooldown:{email}，60 秒（固定窗口）
 → 生成 6 位随机码存 Redis email:code:{email}（TTL 5 分钟）
 → JavaMailSender 发送（QQ 邮箱 SMTP SSL 465）
```

### 1.2 登录链路

```
POST /api/auth/login {username, password}
 → 读 Redis login:fail:{username} ≥5 → "失败次数过多，请15分钟后再试"
 → 查用户：
   ├─ 不存在/已注销 → 统一提示"用户名或密码错误"（防用户名枚举）
   ├─ status=1（拉黑）→ "您的账号已被拉黑，请联系管理员"
   └─ BCrypt.matches 失败 → Lua 原子自增失败计数（15 分钟固定窗口）→ 统一错误提示
 → 成功：清除失败计数 → 生成 JWT（sub=username，claims: userId/role，24h）
 → 返回 token + 用户信息 + 余额
```

### 1.3 找回密码链路

```
POST /api/auth/reset-code {username, email}
 → 校验账号存在 + 邮箱与账号绑定邮箱一致（防任意账号找回）
 → 走验证码发送链路（独立 key 前缀 email:reset:code:）
POST /api/auth/reset-password {username, email, code, password}
 → 再次校验邮箱归属 → 验证码校验（同上，独立计数）→ BCrypt 更新密码
```

---

## 2. JWT 认证与 Redis 认证缓存

**每个请求的认证链路**：

```
请求带 Authorization: Bearer <token>
 → JwtAuthenticationFilter（OncePerRequestFilter）
   ├─ 验签 + 过期校验（jjwt，HS256，secret 来自配置）
   ├─ 取 claims.userId
   ├─ loadAuthUser(userId)：
   │   ① 读 Redis auth:user:{userId}（内容 "role|status|deleted"，TTL 60s）
   │      - 命中：仅用缓存字段构造 principal（其余字段为 null，需要时再查库）
   │      - Redis 不可用：降级直接查库（fail-open，认证不应被缓存故障阻断）
   │   ② 未命中：查库 → 回填 Redis
   ├─ deleted=1 或 status=1 → 拒绝认证（返回 null，走 401）
   └─ 构造 Authentication（ROLE_ + role）放入 SecurityContext
 → SecurityFilterChain 鉴权（permitAll 清单 / authenticated / @PreAuthorize）
```

**为什么不在 JWT 里信任 role**：改角色、拉黑要即时生效，只靠 JWT 要等 token 过期；Redis 缓存 + 变更时主动删除（UserServiceImpl.updateRole/updateStatus 调 evictAuthCache）是「少查库」与「即时生效」的折中。

**未登录/无权限**：SecurityConfig 统一返回 JSON（401"未登录或登录已过期" / 403"没有权限访问"），前端 request.js 拦截 401 清除 token 跳登录页。

---

## 3. 文章：发布 → 编辑 → 删除 → 索引联动

### 3.1 发布链路

```
写文章页（ArticleCreate.vue）
 ├─ [AI 按钮] 生成摘要：POST /api/ai/summary（消耗配额，见 §5）
 ├─ [AI 按钮] 推荐标签：POST /api/ai/tags
 └─ 提交 → POST /api/articles {title, summary, content, categoryId, price, tagIds}
      → ArticleServiceImpl.createArticle（@Transactional）
        ├─ validateLeafCategory：分类必须存在且是叶子分类
        ├─ 插入 article（status=1，价格空视为免费，计数清零）
        ├─ 批量插入 article_tag
        └─ eventPublisher.publishEvent(ArticleContentChangedEvent(articleId))
             └─ 事务提交后（AFTER_COMMIT）→ 异步（aiExecutor）
                → ChunkVectorStore.rebuildArticle：切块 → embedding → 写库 + 内存缓存
```

### 3.2 编辑链路

- `PUT /api/articles/{id}` → checkAuthorOrAdmin（仅作者或管理员）→ 分类变化时重校验叶子分类 → 更新文章 + 重建标签关联 → 发布事件 → 异步重建索引（先清旧分块再建新的）。

### 3.3 删除链路

```
DELETE /api/articles/{id}
 → checkAuthorOrAdmin
 → @Transactional 内：
   ├─ 逻辑删除 article（@TableLogic）
   ├─ 逻辑删除评论
   ├─ 物理删除：收藏/点赞/文章-标签/合集收录（这些表无 deleted 字段）
   ├─ 保留：article_purchase、balance_log（资金审计；不退款是产品决策）
   └─ 发布事件 → 异步清空该文索引（rebuildArticle 发现文章已删/未发布即只清不建）
```

### 3.4 阅读链路

```
GET /api/articles/{id}
 → incrementViewCount（阅读数 +1）
 → getArticleById：组装 VO + 标签
 → 付费权限判定：免费 || 作者 || 管理员 || 已购 || 已购包含它的合集 → 返回正文
   否则 content=null、locked=true（前端渲染购买面板）
```

---

## 4. 付费体系：充值 / 购买文章 / 购买合集

### 4.1 充值链路（模拟支付）

```
POST /api/recharges {amount: 0.01~10000}
 → RechargeServiceImpl.recharge（@Transactional）
   ├─ userMapper.addBalance：UPDATE user SET balance = balance + ? （原子加，防并发丢失）
   ├─ 插 recharge_record（status=1 成功）
   ├─ 查余额 → 插 balance_log（type=RECHARGE，balanceAfter 快照）
   └─ 返回最新余额 → 前端 store 同步刷新
```

### 4.2 购买文章链路

```
POST /api/articles/{id}/purchase
 → 校验：免费文章不卖 / 不能买自己的 / 重复购买拦截
 → @Transactional：
   ├─ 行锁读买家：SELECT ... FOR UPDATE（防并发超买）
   ├─ 余额不足 → "余额不足，请先充值"
   ├─ 扣买家余额、加作者余额（addBalance 原子更新）
   ├─ 插 article_purchase（唯一索引防重复购买）
   ├─ 插两条 balance_log：买家 PURCHASE（负）/ 作者 INCOME（正）
   └─ 插通知：给作者发"你的文章被购买"通知
 → 返回买家最新余额
```

### 4.3 购买合集链路

- 与购买文章同构（扣款 + collection_purchase + 双方流水 + 通知）。
- 附加校验：免费合集/自己的合集/已购拦截；**合集删除保护**——已售出的付费合集不允许删除。
- 效果：买家对合集内所有付费文章解锁（详情接口 covered 判定）。

---

## 5. AI 能力：摘要 / 标签 / 自动重分类

### 5.1 技术底座

- **对话模型**：DeepSeek（`deepseek-v4-flash`，OpenAI 兼容协议），流式与非流式两个 Bean（LangChain4j 手动装配，非 starter）。
- **Embedding 模型**：硅基流动 `BAAI/bge-m3`（1024 维，免费）。
- **线程池**：`aiExecutor`（重建索引/重分类，单线程串行，对限流友好）；`aiChatExecutor`（问答，独立单线程，避免重建堵死聊天）。

### 5.2 摘要生成链路（同步）

```
点"生成摘要" → POST /api/ai/summary
 → @AiQuota 切面：Lua 原子「校验+扣减」今日配额（30 次/天，不足即拒）
 → 拼 prompt（要求只输出 JSON {"summary":"..."}，正文截断 3000 字防超 token）
 → chatModel.chat（非流式）
 → AiJsonParser 宽松解析（容忍 ```json 围栏/前后杂文）→ 失败返回友好业务异常
```

### 5.3 标签推荐链路（同步）

```
POST /api/ai/tags
 → 配额扣减（同上）
 → 读现有标签库 → prompt 要求从现有标签中选 1~5 个、只输出 JSON {"tags":[...]}
 → 解析后按名称映射回标签 id（AI 编造的名字直接忽略，保证只落库已有标签）
```

### 5.4 自动重分类链路（AI 驱动）

```
管理员在父分类下新建第一个子分类
 → CategoryServiceImpl 发布 CategoryChildAddedEvent
 → AiCategorizationService 监听（AFTER_COMMIT + @Async）
 → 逐个把父分类下的文章：调 DeepSeek 判断属于哪个子分类 → 命中则改 categoryId
 → 失败的文章降级：仅记录（前端提示"分类已创建，AI 重分类未完成，请手动整理"）
```

---

## 6. AI RAG 问答：检索 → 提示词 → SSE → 推荐卡片

### 6.1 整体链路

```
用户在小窗提问 → 前端 fetch POST /api/ai/chat（手动解析 SSE，EventSource 不支持 POST）
 → 控制器：@AiQuota 扣配额 → 创建 SseEmitter(180s) → chatAsync 丢给 aiChatExecutor → 立即返回 emitter
 → chatAsync：
   ① checkRateLimit：Redis Lua 计数，每用户每分钟 20 次（固定窗口，fail-open）
   ② status 事件："正在检索知识库…"
   ③ 向量检索：问题 embedding → 与内存中全部向量做余弦相似度（归一化后=点积）
      → 小顶堆取 Top-5 分块 → 回查标题/摘要
   ④ articles 事件：候选文章（前端存作对照表，不直接展示）
   ⑤ status 事件："正在生成回答…"
   ⑥ 组装提示词：资料标注【文章ID：N】《标题》+ 摘要 + 内容；
      规则：严禁照抄原文；推荐类问题只许 1-2 句介绍；末尾输出推荐 JSON
   ⑦ DeepSeek 流式生成 → token 事件逐段推送
   ⑧ 完成 → done 事件 + emitter.complete()；出错 → error 事件
```

### 6.2 前端收尾（收到 done 即收尾，不依赖连接关闭）

```
收到 done → 停止读取 → finalizeAiAnswer：
  ① extractRecommend：定位最后一个 "recommend"，括号配对截取 JSON 对象
  ② 剥离正文中的 JSON（防模型格式漂移：围栏/无围栏/换行缩进均兼容）
  ③ 用 articleId 对照候选表 → 渲染"推荐阅读"卡片（标题+摘要，点击跳文章页）
  ④ aiSending=false → 按钮"停止"复位"发送"
兜底：20 秒无任何事件 → 看门狗强制中止；内容为空 → 弹"AI 响应超时"
```

### 6.3 容错矩阵

| 故障点 | 行为 |
|---|---|
| 索引为空 | error 事件："知识库索引尚未建立"；启动自愈见 §7 |
| 检索无命中 | token 事件："知识库中暂未收录相关资料" |
| 配额耗尽 | JSON 错误（流建立前）："今日 AI 使用次数已达上限" |
| 提问过频 | JSON 错误："提问太频繁，请稍后再试" |
| DeepSeek 报错 | error 事件："AI 服务调用失败" |
| Redis 不可用 | 限流/配额 fail-open 放行，主流程不受阻 |

---

## 7. 向量索引：启动自愈 / 手动重建 / 增量维护

### 7.1 索引结构

```
ai_article_chunk 表：文章按 500 字/块（重叠 50 字）切块，每块存 1024 维归一化向量（BLOB 4KB）
内存：ConcurrentHashMap<chunkId, float[]>，启动时全量加载（重启无需重建）
检索：问题向量与全部缓存向量点积（暴力遍历），当前数据量毫秒级
```

### 7.2 三条维护链路

```
① 启动自愈：loadFromDb → 索引为空但存在已发布文章 → aiExecutor 异步 rebuildAll
② 手动重建（管理员按钮）：POST /api/ai/rebuild-index → rebuildAllAsync
   - 清表 + 清缓存 → 逐篇 rebuildArticle（embedding + 入库 + 入缓存）
   - 单篇失败跳过继续（日志记录），篇间 300ms 间隔防限流
③ 增量维护：文章发布/编辑/删除 → ArticleContentChangedEvent → AFTER_COMMIT 异步 rebuildArticle
   （新文章自动入索引，无需手动重建；删除自动清索引）
```

---

## 8. 私信与拉黑

```
发私信 POST /api/messages {receiverId, content}
 → MessageServiceImpl.sendMessage：
   查 user_block（双向）→ 任一方拉黑 → "对方已将你拉黑" / "你已拉黑对方"
   → 插 message（未读数由接收方查询时统计）
会话列表 GET /api/messages/conversations：
 → 按对方分组取最后一条消息 + 未读数（未读 = 对方发来且未读的消息数）
打开会话 GET /api/messages/conversation/{peerId} → 加载消息后立即标记已读 → 刷新未读数
拉黑 POST /api/messages/block/{userId} → 插 user_block（唯一索引 uk_user_blocked）→ 双向禁言
```

---

## 9. 通知

```
触发点：购买文章/合集（通知作者）、文章被点赞等
Notification 表落库（type/content/是否已读）→ 消息中心分页展示
前端未读轮询：Home 头部铃铛 + 聊天小窗红点，每 30 秒拉一次
  GET /api/notifications/unread-count + GET /api/messages/unread-count 合并为总红点数
```

---

## 10. 评论 / 点赞 / 收藏

- **评论**：发表 → 插 comment + 文章 comment_count+1；删除 → 评论作者或管理员，逻辑删除 + 计数-1；回复通过 parentId 关联（前端渲染层级）。
- **点赞**：POST /api/articles/{id}/like → article_like 唯一索引（article_id,user_id）防重复 → like_count+1。
- **收藏**：toggle 幂等切换（有则删、无则插）→ check 接口供详情页高亮 → my 列表展示。

---

## 11. 合集

```
创建合集 → POST /api/collections {name, description, price}
添加文章 → 仅合集所有者 + 只能收录自己的文章（GET /api/articles/my 选文）
购买合集 → 同 §4.3；买家解锁合集内全部付费文章
删除合集 → 仅所有者；已售出的付费合集禁止删除（保护买家）
广场浏览 → 分页 + 按当前用户标记 own/purchased/已收录文章数
```

---

## 12. 管理员链路：角色 / 封禁 / 调余额

```
用户管理页（仅 SUPER_ADMIN）
 ├─ 改角色：PUT /api/users/{id}/role → 校验合法角色（USER/ADMIN）→ 更新 → 删除 Redis 认证缓存
 ├─ 封禁/解封：PUT /api/users/{id}/status → status=1 后，被拉黑用户下次请求认证被拒
 └─ 调余额：PUT /api/users/{id}/balance → 加/减余额 + balance_log（type=ADMIN，带备注）
分类/标签管理：ADMIN 起，见 API 文档 §3/§4
```

---

## 13. 前端聊天小窗（全局挂载 + 实时更新）

```
ChatWidget.vue 挂载在 App.vue（所有路由共用，切页不销毁）
 ├─ 收起态：右下角 FAB + 未读红点（组件自轮询 30 秒：通知未读 + 私信未读）
 ├─ 展开态：
 │   ├─ 会话列表：AI 助手固定置顶（不依赖是否有历史会话）+ 私信会话
 │   ├─ AI 问答视图：SSE 手动解析 + 打字动画 + 阶段提示 + 推荐卡片
 │   └─ 私信视图：消息气泡 + 拉黑按钮 + 30 秒自动刷新
 └─ 关键细节：
    - AI 消息对象用 reactive() 包装：流式 token 直改 content 才能触发界面实时刷新
    - 收 done 事件即收尾（不依赖连接关闭）；20 秒看门狗兜底
    - 卡片点击 → 收起面板 + 跳转文章详情页
```

---

## 14. 限流与降级全景图

| 场景 | 机制 | Redis 不可用时 |
|---|---|---|
| 登录防爆破 | 用户名 5 次失败锁 15 分钟（Lua 固定窗口） | fail-open 放行 |
| 验证码防轰炸 | IP 10 封/时 + 邮箱 60 秒冷却（Lua） | fail-open 放行 |
| 验证码防爆破 | 每码最多错 5 次（Lua 滑动窗口） | 计数加固失效，核验本身 fail-closed |
| 验证码核验 | Redis 读码比对 | **fail-closed**：拒绝通过并提示 |
| AI 提问频控 | 每用户每分钟 20 次（Lua 固定窗口） | fail-open 放行 |
| AI 每日配额 | 30 次/天（Lua 原子校验+扣减） | fail-open 放行 |
| 认证缓存 | Redis 60s TTL + 变更主动失效 | 降级直接查库 |
| 全部 Redis 操作 | 统一 try/catch 包装 | 见上各场景 |

**fail-open vs fail-closed 的判断原则**：加固手段（限流/计数/缓存）不可用就放行，不阻塞主流程；安全核验（验证码比对）不可用就拒绝，不能因降级而失去防护。
