# Kong AI Books 📚

一个基于现代技术栈的智能在线书籍平台，集成AI技术提供智能搜索、阅读和问答功能。

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2+-green.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.2+-blue.svg)](https://reactjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.0+-blue.svg)](https://www.typescriptlang.org/)
[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](https://github.com/your-username/kong-ai-books/actions)
[![Docker](https://img.shields.io/badge/docker-ready-blue.svg)](https://hub.docker.com/)

> 🚧 **项目状态**: 正在积极开发中，欢迎Star关注项目进展！

## 📸 项目预览

<!-- 
TODO: 添加项目截图
![主页预览](docs/images/homepage.png)
![阅读器预览](docs/images/reader.png)
![AI对话预览](docs/images/ai-chat.png)
-->

> 📝 **注意**: 项目截图将在MVP版本完成后添加。

## 🌟 核心特性

- **📖 在线阅读**: 支持PDF、EPUB、TXT等多种格式的在线阅读
- **🤖 AI智能搜索**: 基于RAG技术的语义搜索和智能问答
- **💬 智能对话**: 与AI助手进行书籍相关的深度对话
- **📊 个性化推荐**: 基于阅读行为的智能书籍推荐
- **🔖 阅读管理**: 书签、阅读进度、笔记等完整的阅读体验
- **🎨 现代UI**: 专业简洁的用户界面，支持深色模式

## 🏗️ 技术架构

### 前端技术栈
- **React 18.2+** - 利用并发特性和自动批处理
- **TypeScript 5.0+** - 类型安全和开发体验
- **Vite 4.0+** - 快速开发服务器和优化构建
- **Tailwind CSS** - 现代CSS框架
- **Zustand** - 轻量级状态管理
- **React Query** - 服务器状态管理

### 后端技术栈
- **Spring Boot 3.2+** - 现代Java开发框架
- **Spring AI 0.8+** - AI能力集成
- **Spring Security 6+** - 安全认证
- **PostgreSQL 15+** - 主数据库
- **Redis 7+** - 缓存和会话存储
- **Chroma** - 向量数据库

### AI/RAG技术栈
- **OpenAI GPT-4** - 大语言模型
- **Spring AI** - 统一AI接口
- **Chroma Vector Store** - 向量存储和检索
- **Apache Tika** - 文档解析和处理

## 🚀 快速开始

### 环境要求

- **Java 17+**
- **Node.js 18+**
- **Docker & Docker Compose**
- **PostgreSQL 15+** (可选，Docker已包含)
- **Redis 7+** (可选，Docker已包含)

### 1. 克隆项目

```bash
git clone https://github.com/your-username/kong-ai-books.git
cd kong-ai-books
```

### 2. 环境配置

```bash
# 复制环境变量配置文件
cp .env.example .env

# 编辑环境变量，设置你的OpenAI API Key
vim .env
```

### 3. Docker一键启动

```bash
# 启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

### 4. 本地开发启动

#### 启动后端服务

```bash
cd backend

# 启动数据库 (如果没有Docker)
# 确保PostgreSQL和Redis正在运行

# 运行Spring Boot应用
./gradlew bootRun
```

#### 启动前端服务

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

## 📁 项目结构

```
kong-ai-books/
├── frontend/                   # React前端应用
│   ├── src/
│   │   ├── components/        # 组件库
│   │   ├── pages/            # 页面组件
│   │   ├── hooks/            # 自定义Hooks
│   │   ├── services/         # API服务层
│   │   ├── store/            # 状态管理
│   │   └── types/            # TypeScript类型
│   ├── public/               # 静态资源
│   └── package.json
├── backend/                   # Spring Boot后端
│   ├── src/main/java/com/kong/aibooks/
│   │   ├── api/              # API控制器
│   │   ├── core/             # 核心业务逻辑
│   │   ├── data/             # 数据访问层
│   │   ├── common/           # 公共模块
│   │   └── integration/      # 外部集成
│   ├── src/main/resources/   # 配置文件
│   └── build.gradle
├── docker/                   # Docker配置
├── nginx/                    # Nginx配置
├── docs/                     # 详细文档
│   ├── ARCHITECTURE.md       # 架构设计
│   ├── ai-rag-architecture.md # AI/RAG系统设计
│   ├── backend-architecture.md # 后端架构
│   └── frontend-architecture.md # 前端架构
└── docker-compose.yml        # Docker编排文件
```

## 🔧 开发指南

### API文档

启动后端服务后，访问以下地址查看API文档：
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

### 主要API端点

```
GET  /api/v1/books              # 获取书籍列表
GET  /api/v1/books/{id}         # 获取书籍详情
GET  /api/v1/books/{id}/content # 获取书籍内容
POST /api/v1/ai/search          # AI智能搜索
POST /api/v1/ai/chat            # AI对话
POST /api/v1/auth/login         # 用户登录
```

### 数据库迁移

```bash
# 运行数据库迁移
cd backend
./gradlew flywayMigrate

# 查看迁移状态
./gradlew flywayInfo
```

### 代码质量

```bash
# 前端代码检查
cd frontend
npm run lint
npm run type-check
npm run test

# 后端代码检查
cd backend
./gradlew check
./gradlew test
```

## 🤖 AI功能配置

### OpenAI配置

1. 获取OpenAI API Key: https://platform.openai.com/api-keys
2. 在`.env`文件中设置: `OPENAI_API_KEY=your_api_key_here`

### 向量数据库

项目使用Chroma作为向量数据库，支持：
- 文档自动向量化
- 语义相似性搜索
- 上下文检索增强

### RAG工作流程

1. **文档摄取**: 自动解析PDF/EPUB文件
2. **文本分块**: 智能分割文档内容
3. **向量化**: 生成文本嵌入向量
4. **存储**: 保存到Chroma向量数据库
5. **检索**: 基于查询的相似性搜索
6. **生成**: 结合上下文的AI回答

## 📊 监控和运维

### 健康检查

- 应用健康状态: http://localhost:8080/actuator/health
- 指标监控: http://localhost:8080/actuator/metrics
- Prometheus: http://localhost:8080/actuator/prometheus

### 日志管理

```bash
# 查看应用日志
docker-compose logs -f backend

# 查看特定服务日志
docker-compose logs -f frontend
```

## 🔒 安全特性

- **JWT认证**: 无状态身份验证
- **OAuth2集成**: 支持Google登录
- **CORS配置**: 跨域资源共享
- **SQL注入防护**: 参数化查询
- **XSS防护**: 输入验证和输出编码

## 🚀 部署指南

### Docker部署 (推荐)

```bash
# 生产环境部署
docker-compose -f docker-compose.prod.yml up -d

# 扩展服务实例
docker-compose up -d --scale backend=3
```

### 传统部署

详细的部署指南请参考: [deployment-guide.md](deployment-guide.md)

## 📖 文档

### 🏗️ 架构文档
- [系统架构设计](ARCHITECTURE.md) - 整体架构概览
- [AI/RAG系统架构](ai-rag-architecture.md) - AI和RAG技术详解
- [后端架构设计](backend-architecture.md) - Spring Boot后端设计
- [前端架构设计](frontend-architecture.md) - React前端设计
- [数据库设计](database-schema.md) - 数据库结构设计

### 🚀 开发文档
- [快速开始指南](QUICKSTART.md) - 5分钟快速启动
- [开发指南](DEVELOPMENT.md) - 详细开发说明
- [功能特性](FEATURES.md) - 完整功能列表
- [技术栈详解](TECH_STACK.md) - 技术选择和对比
- [贡献指南](CONTRIBUTING.md) - 如何参与贡献
- [部署指南](deployment-guide.md) - 生产环境部署

### 📋 其他文档
- [变更日志](CHANGELOG.md) - 版本更新记录
- [许可证](LICENSE) - MIT开源许可证

## 🤝 贡献指南

1. Fork项目
2. 创建特性分支: `git checkout -b feature/amazing-feature`
3. 提交更改: `git commit -m 'Add amazing feature'`
4. 推送分支: `git push origin feature/amazing-feature`
5. 提交Pull Request

### 开发规范

- 遵循代码风格指南
- 编写单元测试
- 更新相关文档
- 确保CI/CD通过

## 📝 许可证

本项目采用MIT许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 🙏 致谢

- [Spring AI](https://spring.io/projects/spring-ai) - AI集成框架
- [Chroma](https://www.trychroma.com/) - 向量数据库
- [React](https://reactjs.org/) - 前端框架
- [Vite](https://vitejs.dev/) - 构建工具

## 🔗 快速链接

| 资源 | 链接 | 描述 |
|------|------|------|
| 🚀 快速开始 | [QUICKSTART.md](QUICKSTART.md) | 5分钟快速启动指南 |
| 🛠️ 开发指南 | [DEVELOPMENT.md](DEVELOPMENT.md) | 详细开发说明 |
| 🏗️ 系统架构 | [ARCHITECTURE.md](ARCHITECTURE.md) | 完整架构设计 |
| 🤖 AI架构 | [ai-rag-architecture.md](ai-rag-architecture.md) | AI/RAG技术详解 |
| 🌟 功能特性 | [FEATURES.md](FEATURES.md) | 完整功能列表 |
| 🔧 技术栈 | [TECH_STACK.md](TECH_STACK.md) | 技术选择和对比 |
| 🗺️ 项目路线图 | [ROADMAP.md](ROADMAP.md) | 开发计划和愿景 |
| 🤝 贡献指南 | [CONTRIBUTING.md](CONTRIBUTING.md) | 如何参与贡献 |

## 📊 项目统计

![GitHub stars](https://img.shields.io/github/stars/your-username/kong-ai-books?style=social)
![GitHub forks](https://img.shields.io/github/forks/your-username/kong-ai-books?style=social)
![GitHub issues](https://img.shields.io/github/issues/your-username/kong-ai-books)
![GitHub pull requests](https://img.shields.io/github/issues-pr/your-username/kong-ai-books)

## 📞 联系我们

- 🏠 **项目主页**: https://github.com/your-username/kong-ai-books
- 🐛 **问题反馈**: https://github.com/your-username/kong-ai-books/issues
- 💬 **讨论交流**: https://github.com/your-username/kong-ai-books/discussions
- 📧 **邮箱联系**: contact@kong-ai-books.com
- 📚 **文档网站**: https://docs.kong-ai-books.com (即将推出)

## ⭐ 支持项目

如果这个项目对你有帮助，请考虑：

- ⭐ 给项目点个Star
- 🐛 报告问题和建议
- 🤝 提交代码贡献
- 📢 分享给更多人

---

<div align="center">

**Kong AI Books** - 让阅读更智能 🚀

[开始使用](QUICKSTART.md) • [查看文档](ARCHITECTURE.md) • [参与贡献](CONTRIBUTING.md)

</div>
