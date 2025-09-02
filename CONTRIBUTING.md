# 贡献指南

感谢你对Kong AI Books项目的关注！我们欢迎所有形式的贡献，包括但不限于代码、文档、测试、问题反馈等。

## 🤝 如何贡献

### 报告问题

如果你发现了bug或有功能建议，请：

1. 搜索现有的[Issues](https://github.com/your-username/kong-ai-books/issues)，确保问题未被报告
2. 使用适当的Issue模板创建新Issue
3. 提供详细的问题描述和重现步骤

### 提交代码

1. **Fork项目**
   ```bash
   # Fork项目到你的GitHub账户
   # 然后克隆到本地
   git clone https://github.com/your-username/kong-ai-books.git
   cd kong-ai-books
   ```

2. **创建分支**
   ```bash
   # 从main分支创建新分支
   git checkout -b feature/your-feature-name
   
   # 分支命名规范:
   # feature/功能名称 - 新功能
   # fix/问题描述 - 修复bug
   # docs/文档类型 - 文档更新
   # refactor/重构内容 - 代码重构
   ```

3. **开发和测试**
   ```bash
   # 安装依赖并启动开发环境
   docker-compose up -d
   
   # 进行开发
   # ...
   
   # 运行测试
   cd frontend && npm test
   cd backend && ./gradlew test
   ```

4. **提交代码**
   ```bash
   # 添加文件
   git add .
   
   # 提交 (使用规范的提交信息)
   git commit -m "feat: add AI-powered book recommendation feature"
   
   # 推送到你的Fork
   git push origin feature/your-feature-name
   ```

5. **创建Pull Request**
   - 在GitHub上创建Pull Request
   - 填写PR模板
   - 等待代码审查

## 📝 代码规范

### Java代码规范

```java
// 类命名: PascalCase
public class BookService {
    
    // 常量: UPPER_SNAKE_CASE
    private static final String DEFAULT_LANGUAGE = "zh-CN";
    
    // 方法命名: camelCase
    public BookDTO getBookDetail(String bookId) {
        // 方法实现
    }
    
    // 注释规范
    /**
     * 获取书籍详情
     * 
     * @param bookId 书籍ID
     * @return 书籍详情DTO
     * @throws ResourceNotFoundException 当书籍不存在时
     */
}
```

### TypeScript代码规范

```typescript
// 接口命名: PascalCase
interface BookSearchProps {
  onSearch: (query: string) => void;
  placeholder?: string;
}

// 组件命名: PascalCase
export const BookSearch: React.FC<BookSearchProps> = ({ onSearch, placeholder }) => {
  // 变量命名: camelCase
  const [searchQuery, setSearchQuery] = useState('');
  
  // 常量: UPPER_SNAKE_CASE
  const MAX_QUERY_LENGTH = 100;
  
  return (
    // JSX实现
  );
};
```

### 提交信息规范

使用[Conventional Commits](https://www.conventionalcommits.org/)规范：

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

#### 类型说明

- `feat`: 新功能
- `fix`: 修复bug
- `docs`: 仅文档更改
- `style`: 不影响代码含义的更改（空格、格式等）
- `refactor`: 既不修复bug也不添加功能的代码更改
- `perf`: 提高性能的代码更改
- `test`: 添加缺失的测试或修正现有测试
- `chore`: 对构建过程或辅助工具的更改

#### 示例

```
feat(ai): add semantic search functionality

- Implement vector similarity search
- Add embedding generation for documents
- Integrate with Chroma vector database

Closes #123
```

## 🧪 测试要求

### 单元测试

- 新功能必须包含单元测试
- 测试覆盖率不低于80%
- 使用有意义的测试名称

```java
// Java测试示例
@Test
@DisplayName("应该能够根据ID获取书籍详情")
void shouldGetBookDetailById() {
    // Given
    String bookId = "test-book-id";
    Book book = createTestBook();
    when(bookRepository.findById(any())).thenReturn(Optional.of(book));
    
    // When
    BookDetailDTO result = bookService.getBookDetail(bookId);
    
    // Then
    assertThat(result.getTitle()).isEqualTo(book.getTitle());
}
```

```typescript
// TypeScript测试示例
describe('BookCard', () => {
  it('should display book information correctly', () => {
    const mockBook = createMockBook();
    render(<BookCard book={mockBook} />);
    
    expect(screen.getByText(mockBook.title)).toBeInTheDocument();
    expect(screen.getByText(mockBook.author)).toBeInTheDocument();
  });
});
```

### 集成测试

```java
@SpringBootTest
@TestcontainerS
class BookControllerIntegrationTest {
    
    @Test
    void shouldCreateBookSuccessfully() {
        // 集成测试逻辑
    }
}
```

## 📋 Pull Request检查清单

在提交Pull Request前，请确保：

### 代码质量
- [ ] 代码遵循项目编码规范
- [ ] 没有明显的代码异味
- [ ] 变量和方法命名清晰
- [ ] 添加了必要的注释

### 功能完整性
- [ ] 功能按照需求正确实现
- [ ] 处理了边界情况和错误情况
- [ ] API设计符合RESTful规范
- [ ] 前端组件可复用且易维护

### 测试覆盖
- [ ] 添加了单元测试
- [ ] 测试覆盖了主要功能路径
- [ ] 集成测试通过
- [ ] 手动测试验证功能正确

### 文档更新
- [ ] 更新了相关API文档
- [ ] 更新了组件文档
- [ ] 更新了README (如有必要)
- [ ] 添加了变更日志

### 安全检查
- [ ] 没有硬编码敏感信息
- [ ] 输入验证充分
- [ ] 权限检查正确
- [ ] 没有SQL注入风险

## 🏷️ 发布流程

### 版本号规范

使用[Semantic Versioning](https://semver.org/)：

- `MAJOR.MINOR.PATCH`
- 主版本号：不兼容的API更改
- 次版本号：向后兼容的功能添加
- 修订号：向后兼容的问题修正

### 发布步骤

1. **更新版本号**
   ```bash
   # 更新package.json (前端)
   npm version patch|minor|major
   
   # 更新build.gradle (后端)
   # version = '1.1.0'
   ```

2. **创建发布分支**
   ```bash
   git checkout -b release/v1.1.0
   ```

3. **更新变更日志**
   ```bash
   # 更新CHANGELOG.md
   # 记录新功能、修复和破坏性更改
   ```

4. **创建Release PR**
   - 合并到main分支
   - 创建Git tag
   - 发布GitHub Release

## 🔒 安全政策

### 报告安全漏洞

如果你发现了安全漏洞，请：

1. **不要**在公开的Issue中报告
2. 发送邮件到：security@kong-ai-books.com
3. 包含漏洞详细描述和重现步骤
4. 我们会在24小时内回复

### 安全最佳实践

- 定期更新依赖项
- 使用安全的编码实践
- 进行安全测试
- 遵循OWASP指南

## 🙋‍♂️ 获取帮助

### 开发问题

- 查看[开发指南](DEVELOPMENT.md)
- 搜索现有的Issues
- 在Discussion中提问

### 技术支持

- 邮箱：support@kong-ai-books.com
- 文档：查看docs/目录下的详细文档
- 社区：加入我们的开发者社群

## 🎉 贡献者

感谢所有为项目做出贡献的开发者！

<!-- 贡献者列表将自动更新 -->

## 📄 许可证

通过贡献代码，你同意你的贡献将在MIT许可证下发布。

---

再次感谢你的贡献！让我们一起构建更好的Kong AI Books！ 🚀