# Kong AI Books 快速开始指南

这个指南将帮助你在5分钟内启动Kong AI Books项目。

## ⚡ 5分钟快速启动

### 前置条件

确保你的系统已安装：
- [Docker](https://docs.docker.com/get-docker/) 和 [Docker Compose](https://docs.docker.com/compose/install/)
- [Git](https://git-scm.com/)

### 第1步: 获取代码

```bash
# 克隆项目
git clone https://github.com/your-username/kong-ai-books.git
cd kong-ai-books
```

### 第2步: 配置环境

```bash
# 复制环境配置文件
cp .env.example .env

# 编辑环境变量 (至少需要设置OpenAI API Key)
nano .env
```

**重要**: 在`.env`文件中设置你的OpenAI API Key：
```
OPENAI_API_KEY=sk-your-openai-api-key-here
```

### 第3步: 启动服务

```bash
# 一键启动所有服务
docker-compose up -d

# 等待服务启动 (大约1-2分钟)
docker-compose logs -f backend | grep "Started KongAiBooksApplication"
```

### 第4步: 访问应用

🎉 **完成！** 现在你可以访问：

- **前端应用**: http://localhost:3000
- **后端API**: http://localhost:8080
- **API文档**: http://localhost:8080/swagger-ui.html
- **健康检查**: http://localhost:8080/actuator/health

## 🔧 验证安装

### 检查服务状态

```bash
# 查看所有服务状态
docker-compose ps

# 应该看到以下服务都是Up状态:
# - frontend
# - backend  
# - postgres
# - redis
# - chroma
# - nginx
```

### 测试API

```bash
# 测试后端健康状态
curl http://localhost:8080/actuator/health

# 测试书籍API
curl http://localhost:8080/api/v1/books

# 测试AI功能 (需要先有数据)
curl -X POST http://localhost:8080/api/v1/ai/search \
  -H "Content-Type: application/json" \
  -d '{"query": "人工智能相关的书籍"}'
```

## 📚 下一步

### 1. 上传测试书籍

```bash
# 将PDF或EPUB文件放入uploads目录
mkdir -p uploads
cp your-test-book.pdf uploads/

# 通过API上传书籍 (或使用前端界面)
```

### 2. 创建用户账户

访问 http://localhost:3000 并注册新账户，或使用Google OAuth登录。

### 3. 体验AI功能

- 上传书籍后，系统会自动进行向量化处理
- 使用AI搜索功能查找相关内容
- 与AI助手进行书籍相关的对话

## 🛠️ 开发模式

如果你想进行开发，可以使用本地开发模式：

### 启动基础服务

```bash
# 只启动数据库和依赖服务
docker-compose up -d postgres redis chroma
```

### 启动后端 (开发模式)

```bash
cd backend

# 运行数据库迁移
./gradlew flywayMigrate

# 启动Spring Boot (热重载)
./gradlew bootRun
```

### 启动前端 (开发模式)

```bash
cd frontend

# 安装依赖
npm install

# 启动Vite开发服务器 (热重载)
npm run dev
```

## 🐛 常见问题

### 问题1: 端口被占用

```bash
# 检查端口占用
netstat -tlnp | grep :3000
netstat -tlnp | grep :8080

# 停止占用端口的进程或修改docker-compose.yml中的端口映射
```

### 问题2: OpenAI API调用失败

```bash
# 检查API Key是否正确设置
docker-compose exec backend env | grep OPENAI

# 查看后端日志
docker-compose logs backend | grep -i openai
```

### 问题3: 数据库连接失败

```bash
# 检查PostgreSQL服务状态
docker-compose exec postgres pg_isready

# 查看数据库日志
docker-compose logs postgres
```

### 问题4: 前端无法访问后端

```bash
# 检查网络连接
docker-compose exec frontend curl http://backend:8080/actuator/health

# 检查CORS配置
curl -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: GET" \
  -X OPTIONS \
  http://localhost:8080/api/v1/books
```

## 🧹 清理环境

当你想要重置环境时：

```bash
# 停止所有服务
docker-compose down

# 删除所有数据 (谨慎使用!)
docker-compose down -v

# 删除所有镜像
docker-compose down --rmi all
```

## 📞 获取帮助

如果遇到问题：

1. 查看[开发指南](DEVELOPMENT.md)
2. 搜索[Issues](https://github.com/your-username/kong-ai-books/issues)
3. 在[Discussions](https://github.com/your-username/kong-ai-books/discussions)中提问
4. 联系开发团队：support@kong-ai-books.com

---

🎉 **欢迎来到Kong AI Books的世界！**