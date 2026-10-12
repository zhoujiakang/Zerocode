# ZeroCode

zzz 的低代码应用生成平台基础工程。包含后端基础设施、前端全局布局与用户模块。支持用户注册、登录、获取当前用户、退出登录、管理员搜索和逻辑删除用户。应用生成等业务尚未接入。

## 目录

- `backend`：Java 21 / Spring Boot 3.5.7 / MyBatis-Plus 3.5.17 / Maven，包含 common、config、controller、exception、satoken、aop、model、mapper、service。
- `frontend`：Vue 3 / Vite / Ant Design Vue / Vue Router / Pinia。
- `backend/sql/init.sql`：MySQL 8.x 数据库及用户表初始化脚本。

## 启动后端

准备 Java 21、Maven 3.6.3+ 和 MySQL 8.x。当前本机 `zerocode` 数据库和用户表已创建。数据库账号保存在 `backend/config/application-local.yml`（Git 忽略），默认使用 local profile。进入 backend 后可以直接启动：

```sh
cd backend
# macOS 下如 Maven 未使用 Java 21，可设置：
# export JAVA_HOME=$(/usr/libexec/java_home -v 21)
# 本机无需重复配置密码；其他环境可以通过环境变量覆盖：
# export DB_USERNAME=root
# export DB_PASSWORD='你的本地数据库密码'
# 可选：export DB_URL='jdbc:mysql://localhost:3306/zerocode?serverTimezone=Asia/Shanghai'
mvn spring-boot:run
```

- HTTP 服务检查：<http://localhost:8123/api/health>（只检测 HTTP 服务，数据库在实际查询时连接）
- Knife4j 文档：<http://localhost:8123/api/doc.html>
- OpenAPI：<http://localhost:8123/api/v3/api-docs/default>
- 测试：`mvn test`；打包：`mvn package`。
- 所有业务接口显式返回 `ResultUtils.success(data)`，异常由 `GlobalExceptionHandler` 统一包装，成功 code 为 0，错误使用对应 HTTP 状态码和业务 code。
- 请求对象使用 `@Valid` 触发校验；分页最多 100 条，当前用户列表不指定排序条件，直接返回数据库查询结果。
- AOP 记录方法、耗时和 requestId；不会记录请求正文。响应头为 `X-Request-Id`。
- 默认允许 localhost/127.0.0.1 的 5173（前端）和 8123（接口文档）来源。通过 `CORS_ALLOWED_ORIGINS` 设置逗号分隔的来源。
- 生产环境用 `SPRING_PROFILES_ACTIVE=prod`，默认关闭接口文档；数据库密码从环境变量提供。
- Knife4j 使用 Jakarta starter，配合 Springdoc 2.8.x。关闭 Knife4j 4.5.0 的增强模式以避开二进制兼容问题，`doc.html` 文档 UI 和接口调试仍可使用。

## 启动前端

使用 Node.js 20.19+ 或 22.12+：

```sh
cd frontend
npm ci
npm run dev
```

访问 <http://localhost:5173>。开发环境由 Vite 将 `/api` 代理到后端 8123 端口。生产构建运行 `npm run build`；部署时配置 `/api` 反向代理和 Vue Router history 回退到 `index.html`。

## 全局布局

`App.vue` 引入 `layouts/BasicLayout.vue`，使用 Ant Design Vue Layout 实现上中下结构。`GlobalHeader` 包含 PNG logo、标题、配置菜单和登录占位按钮；内容通过 RouterView 切换；`GlobalFooter` 显示 zzz 版权信息。

底部采用 flex 布局贴底：内容短时位于视口底部，内容长时随文档排布，不覆盖正文。移动端导航换行，卡片改为单列。没有默认 `main.css` 文件或引用，基础样式在 `App.vue` 内统一管理。

在 `frontend/src/config/site.js` 修改标题、菜单、作者及 `githubUrl`。GitHub 地址为空时不会生成无效超链接。

页面按 URL 层级组织在 `src/pages`，包括 `homepage/HomePage.vue`、`user/UserLoginPage.vue`、`user/UserRegisterPage.vue`、`user/UserProfilePage.vue`、`admin/UserManagePage.vue` 和错误页。顶部栏根据 Pinia 用户状态展示头像、昵称、个人信息和退出登录菜单；管理员额外显示用户管理入口。

## 文档参考

- [Vue 快速开始](https://vuejs.org/guide/quick-start.html)
- [Vue Router](https://router.vuejs.org/guide/)
- [Ant Design Vue Layout](https://antdv.com/components/layout)
- [Knife4j 快速开始](https://doc.xiaominfo.com/docs/quick-start)
- [Springdoc 兼容版本](https://springdoc.org/)

## 用户模块

| 接口（统一 `/api` 前缀） | 权限 | 功能 |
| --- | --- | --- |
| `POST /user/register` | 公开 | 账号、密码、确认密码注册，返回字符串形式的用户 ID |
| `POST /user/login` | 公开 | 登录，返回脱敏用户信息，设置 HttpOnly Cookie |
| `GET /user/get/login` | 已登录 | 获取当前登录用户，刷新网页时恢复状态 |
| `POST /user/logout` | 公开、幂等 | 注销当前浏览器会话 |
| `POST /user/list/page` | admin | 按 ID、账号、昵称、角色分页搜索用户 |
| `POST /user/delete` | admin | 逻辑删除用户并使其所有登录会话失效 |

账号为 4–32 位字母、数字或下划线，统一转为小写；密码为 8–64 位且 UTF-8 不超过 72 字节，使用 BCrypt 随机盐哈希存储。注册接口不接受用户角色，永远创建普通用户；确认密码不一致和唯一索引冲突都会返回明确错误。所有用户视图不包含密码，ID 使用字符串避免前端精度丢失。

管理员初始化：先通过注册页面注册账号，再由数据库维护者在 MySQL 中执行（替换为你的实际账号）：

```sql
UPDATE `user` SET userRole = 'admin', editTime = NOW()
WHERE userAccount = '你的管理员账号' AND isDelete = 0;
```

没有默认管理员账号或预置密码。角色从数据库实时获取，修改后立即生效；管理员不能删除当前登录的自己。被逻辑删除的账号仍占用唯一账号名，不能重新注册。

Sa-Token 会话默认保存在单个后端进程内存中，有效期 30 天；后端重启会失效。若以后部署多实例或需要重启后保持会话，再接入共享会话存储。浏览器通过 `zerocode-token` HttpOnly / SameSite=Lax Cookie 携带会话，不在 localStorage 保存令牌或密码。生产 HTTPS 下启用 Secure Cookie；前端和 `/api` 推荐部署在同一站点，通过反向代理访问。

浏览器跨域来源通过 `CORS_ALLOWED_ORIGINS` 配置。前端路由守卫用于界面导航，后端 `@SaCheckRole("admin")` 执行最终权限检查。会话过期返回 401，普通用户越权返回 403；前端清理失效的用户状态并跳转登录。

用户模块测试：`cd backend && mvn test`（使用独立 H2 MySQL 模式，不连接或修改你的 MySQL 数据）。其他机器需执行 MySQL 初始化脚本，并从 `backend/config/application-local.example.yml` 复制本地配置或设置 `DB_USERNAME`、`DB_PASSWORD`。前端状态测试：`cd frontend && npm test`。

官方参考：[Sa-Token 注解鉴权](https://sa-token.com/use/at-check.html)、[Sa-Token 权限认证](https://sa-token.com/use/jur-auth.html)、[Pinia Store](https://pinia.vuejs.org/core-concepts/)。

数据访问采用 MyBatis-Plus `BaseMapper`、Lambda 条件查询和分页插件，账号唯一约束由数据库保证；实体显式映射驼峰字段。`@TableLogic` 自动过滤被删除用户。创建时间使用数据库默认值，编辑与更新时间使用 MySQL 的 `ON UPDATE CURRENT_TIMESTAMP` 自动维护。生产环境启用 prod profile 后不加载本地账号配置。

MyBatis-Plus 参考：[Spring Boot 3 安装](https://baomidou.com/en/getting-started/install/)、[分页插件](https://baomidou.com/plugins/pagination/)。

## 后端职责分层

采用 `Controller → Service → Mapper`：Controller 接收参数和返回响应；Service 执行注册、登录、注销等业务，使用 Lambda 条件构造器调用 Mapper；Mapper 继承 MyBatis-Plus BaseMapper 提供 CRUD 和分页能力。无 Repository 层，无手写业务 SQL。

- `satoken/SaTokenConfig`：启用 Sa-Token 注解鉴权。
- `satoken/UserRoleProvider`：从数据库获取用户角色和权限。
- `aop/RequestLogAspect`：记录请求追踪 ID、调用方法、耗时，与 Sa-Token 的鉴权职责独立。

已有数据库的时间字段变更脚本为 `backend/sql/migrate-user-times.sql`。本机已执行：editTime 和 updateTime 都随记录实际修改自动更新，createTime 保持不变。实体将这些时间字段配置为禁止插入/更新赋值，不再使用 MetaObjectHandler。

## AI 应用生成：第一步（基础配置与工厂）

已接入 LangChain4j 1.22.0 和 Redis 记忆组件 1.22.0-beta32。当前阶段提供模型注册表、两种生成模式、系统提示词、Agent 接口和组装工厂，尚未提供正式文件工具、生成接口或生成页面。

- `AiModelRegistry`：通过 OpenAI 兼容 Chat Completions 接口按配置名称创建和复用模型，不固定为某一家服务商。各场景的 Agent 工厂可以选择不同模型配置。
- `CodeGenerationAgentFactory`：绑定模型、模式提示词、调用方提供的工具集合和窗口记忆。使用方必须先验证应用归属，并在同一应用内串行调用。
- `AgentContext`：以 `userId:appId:mode` 作为记忆 ID；同一应用续写可以重新创建 Agent 并恢复 Redis 记忆。
- `RedisChatMemoryStore`：普通字符串存储，不需要 RedisJSON。默认前缀 `zerocode:agent:memory:`，每次更新后有效期为 7 天。Redis 默认 `127.0.0.1:6379`；host、port、user、password、TLS 可通过环境变量配置。
- 系统提示词放在 `backend/src/main/resources/prompts`，采用通用规则 + HTML / 多文件规则，明确要求源码通过工具保存。
- `AI_ENABLED` 默认 false，未配置模型时现有用户功能正常启动；尝试创建 Agent 时返回明确的 AI 未配置异常。

启用某个 OpenAI 兼容服务时，在本地环境提供：

```sh
export AI_ENABLED=true
export AI_BASE_URL='https://你的服务地址/v1'
export AI_MODEL_NAME='实际模型名称'
export AI_API_KEY='你的模型密钥'
```

密钥也可放入 Git 忽略的 `backend/config/application-local.yml`，不要写入公共配置或提交到仓库。模型必须支持 Chat Completions 工具调用；当前适配器不是 Responses API 适配器。

多个模型的配置示例（写入本地配置，按实际服务填写）：

```yaml
app:
  ai:
    enabled: true
    code-generation-model: coding
    models:
      coding:
        base-url: ${CODING_BASE_URL}
        model-name: ${CODING_MODEL_NAME}
        api-key: ${CODING_API_KEY}
      planning:
        base-url: ${PLANNING_BASE_URL}
        model-name: ${PLANNING_MODEL_NAME}
        api-key: ${PLANNING_API_KEY}
```

Redis 记忆是模型可见的窗口，不是完整对话历史；完整历史和应用信息将在后续步骤保存在 MySQL 中。Agent 实例缓存和正式 ToolManager 也随文件工具步骤实现。

验证：`mvn test` 使用模拟模型，不产生模型费用。运行真实 Redis 验证时使用 `RUN_REDIS_TESTS=true mvn test`；测试只操作随机 `zerocode:test:` 前缀并自行清理，不修改现有记忆。

开发步骤：
1. Redis / 多模型配置 / 提示词 / Agent 工厂基础。
2. 绑定应用工作目录的读、写、修改和目录工具，以及工厂缓存。
3. 应用与对话历史持久化、生成与继续修改接口。
4. 前端生成页面和联调。

每个步骤验证完成后单独 Git 提交。
