# ZeroCode

zzz 的低代码应用生成平台基础工程。当前仅初始化后端通用基础设施与前端全局布局，不包含应用生成、登录或应用管理业务。

## 目录

- `backend`：Java 21 / Spring Boot 3.5.7 / Maven，包含 common、config、controller、exception、aop。
- `frontend`：Vue 3 / Vite / Ant Design Vue / Vue Router。
- `backend/sql/init.sql`：MySQL 8.x 数据库初始化脚本。

## 启动后端

准备 Java 21、Maven 3.6.3+ 和 MySQL 8.x。先执行 `backend/sql/init.sql`，然后设置数据库环境变量并启动：

```sh
cd backend
# macOS 下如 Maven 未使用 Java 21，可设置：
# export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export DB_USERNAME=root
export DB_PASSWORD='你的本地数据库密码'
# 可选：export DB_URL='jdbc:mysql://localhost:3306/zerocode?serverTimezone=Asia/Shanghai'
mvn spring-boot:run
```

- HTTP 服务检查：<http://localhost:8123/api/health>（只检测 HTTP 服务，数据库在实际查询时连接）
- Knife4j 文档：<http://localhost:8123/api/doc.html>
- OpenAPI：<http://localhost:8123/api/v3/api-docs/default>
- 测试：`mvn test`；打包：`mvn package`。
- 所有业务接口显式返回 `ResultUtils.success(data)`，异常由 `GlobalExceptionHandler` 统一包装，成功 code 为 0，错误使用对应 HTTP 状态码和业务 code。
- 请求对象使用 `@Valid` 触发校验；分页最多 100 条，业务层必须将排序字段映射到白名单。
- AOP 记录方法、耗时和 requestId；不会记录请求正文。响应头为 `X-Request-Id`。
- 默认跨域来源为 localhost/127.0.0.1:5173。通过 `CORS_ALLOWED_ORIGINS` 设置逗号分隔的来源。
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

当前只包含首页占位和 404 页面。登录按钮仅显示待接入提示，不包含登录业务。

## 文档参考

- [Vue 快速开始](https://vuejs.org/guide/quick-start.html)
- [Vue Router](https://router.vuejs.org/guide/)
- [Ant Design Vue Layout](https://antdv.com/components/layout)
- [Knife4j 快速开始](https://doc.xiaominfo.com/docs/quick-start)
- [Springdoc 兼容版本](https://springdoc.org/)
