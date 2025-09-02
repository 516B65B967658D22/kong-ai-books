# 后端架构设计 - Kong AI Books

## 技术栈概览

### 核心框架
- **Spring Boot 3.2+**: 主应用框架
- **Spring AI 0.8+**: AI能力集成
- **Spring Security 6+**: 安全认证
- **Spring Data JPA**: 数据访问层
- **Spring WebFlux**: 响应式编程支持

### 数据存储
- **PostgreSQL 15+**: 主数据库
- **Redis 7+**: 缓存和会话存储
- **Chroma/Pinecone**: 向量数据库
- **MinIO/S3**: 文件存储

### 中间件
- **RabbitMQ**: 消息队列
- **Elasticsearch**: 全文搜索 (可选)
- **Prometheus + Grafana**: 监控
- **Zipkin**: 链路追踪

## 模块架构设计

```
kong-ai-books-backend/
├── kong-ai-books-api/              # API网关和控制器层
├── kong-ai-books-core/             # 核心业务逻辑
│   ├── book-service/               # 书籍管理服务
│   ├── user-service/               # 用户管理服务
│   ├── ai-service/                 # AI服务
│   └── search-service/             # 搜索服务
├── kong-ai-books-common/           # 公共模块
│   ├── security/                   # 安全配置
│   ├── config/                     # 配置管理
│   ├── exception/                  # 异常处理
│   └── utils/                      # 工具类
├── kong-ai-books-data/             # 数据访问层
│   ├── repository/                 # 数据仓库
│   ├── entity/                     # 实体类
│   └── migration/                  # 数据库迁移
└── kong-ai-books-integration/      # 外部集成
    ├── ai-client/                  # AI客户端
    ├── storage-client/             # 存储客户端
    └── notification/               # 通知服务
```

## API层设计

### 1. 控制器设计

```java
// api/controller/BookController.java
@RestController
@RequestMapping("/api/v1/books")
@Slf4j
@Validated
public class BookController {
    
    private final BookService bookService;
    private final ReadingProgressService progressService;
    
    @GetMapping
    public ResponseEntity<PageResult<BookDTO>> getBooks(
            @Valid BookQueryRequest request,
            Pageable pageable) {
        
        log.info("Fetching books with query: {}", request);
        PageResult<BookDTO> result = bookService.getBooks(request, pageable);
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/{bookId}")
    public ResponseEntity<BookDetailDTO> getBookDetail(
            @PathVariable @NotBlank String bookId) {
        
        BookDetailDTO book = bookService.getBookDetail(bookId);
        return ResponseEntity.ok(book);
    }
    
    @GetMapping("/{bookId}/content")
    public ResponseEntity<BookContentDTO> getBookContent(
            @PathVariable @NotBlank String bookId,
            @RequestParam @Min(1) int page) {
        
        BookContentDTO content = bookService.getBookContent(bookId, page);
        return ResponseEntity.ok(content);
    }
    
    @PostMapping("/{bookId}/progress")
    public ResponseEntity<Void> updateReadingProgress(
            @PathVariable @NotBlank String bookId,
            @RequestBody @Valid ReadingProgressRequest request,
            Authentication authentication) {
        
        String userId = authentication.getName();
        progressService.updateProgress(userId, bookId, request);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookDTO> uploadBook(
            @RequestParam("file") MultipartFile file,
            @RequestParam("metadata") @Valid BookMetadataRequest metadata) {
        
        BookDTO book = bookService.uploadBook(file, metadata);
        return ResponseEntity.status(HttpStatus.CREATED).body(book);
    }
}

// api/controller/AIController.java
@RestController
@RequestMapping("/api/v1/ai")
@Slf4j
public class AIController {
    
    private final AISearchService aiSearchService;
    private final ChatService chatService;
    private final RecommendationService recommendationService;
    
    @PostMapping("/search")
    public ResponseEntity<AISearchResponse> intelligentSearch(
            @RequestBody @Valid AISearchRequest request,
            Authentication authentication) {
        
        String userId = authentication.getName();
        log.info("AI search request from user {}: {}", userId, request.getQuery());
        
        AISearchResponse response = aiSearchService.search(request, userId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/chat")
    public Flux<ServerSentEvent<ChatResponseChunk>> chatWithAI(
            @RequestBody @Valid ChatRequest request,
            Authentication authentication) {
        
        String userId = authentication.getName();
        log.info("AI chat request from user {}", userId);
        
        return chatService.chatStream(request, userId)
            .map(chunk -> ServerSentEvent.<ChatResponseChunk>builder()
                .data(chunk)
                .build());
    }
    
    @GetMapping("/recommendations/{userId}")
    public ResponseEntity<List<BookRecommendationDTO>> getRecommendations(
            @PathVariable @NotBlank String userId,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        
        List<BookRecommendationDTO> recommendations = 
            recommendationService.getPersonalizedRecommendations(userId, limit);
        return ResponseEntity.ok(recommendations);
    }
}
```

### 2. 异常处理

```java
// common/exception/GlobalExceptionHandler.java
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BookNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBookNotFound(BookNotFoundException e) {
        log.warn("Book not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse.builder()
                .code("BOOK_NOT_FOUND")
                .message(e.getMessage())
                .timestamp(Instant.now())
                .build());
    }
    
    @ExceptionHandler(AIServiceException.class)
    public ResponseEntity<ErrorResponse> handleAIServiceException(AIServiceException e) {
        log.error("AI service error: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(ErrorResponse.builder()
                .code("AI_SERVICE_ERROR")
                .message("AI服务暂时不可用，请稍后重试")
                .timestamp(Instant.now())
                .build());
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException e) {
        
        List<String> errors = e.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.toList());
            
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.builder()
                .code("VALIDATION_ERROR")
                .message("请求参数验证失败")
                .details(errors)
                .timestamp(Instant.now())
                .build());
    }
}
```

## 服务层设计

### 1. 书籍服务

```java
// core/book-service/BookService.java
@Service
@Transactional
@Slf4j
public class BookService {
    
    private final BookRepository bookRepository;
    private final BookContentRepository contentRepository;
    private final FileStorageService fileStorageService;
    private final AIEmbeddingService embeddingService;
    private final CacheManager cacheManager;
    
    @Cacheable(value = "books", key = "#request.toString() + #pageable.toString()")
    public PageResult<BookDTO> getBooks(BookQueryRequest request, Pageable pageable) {
        log.debug("Fetching books with query: {}", request);
        
        Specification<Book> spec = BookSpecification.build(request);
        Page<Book> bookPage = bookRepository.findAll(spec, pageable);
        
        List<BookDTO> bookDTOs = bookPage.getContent().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
            
        return PageResult.<BookDTO>builder()
            .content(bookDTOs)
            .totalElements(bookPage.getTotalElements())
            .totalPages(bookPage.getTotalPages())
            .currentPage(bookPage.getNumber())
            .build();
    }
    
    @Cacheable(value = "book-detail", key = "#bookId")
    public BookDetailDTO getBookDetail(String bookId) {
        Book book = bookRepository.findById(UUID.fromString(bookId))
            .orElseThrow(() -> new BookNotFoundException("Book not found: " + bookId));
            
        return convertToDetailDTO(book);
    }
    
    @Cacheable(value = "book-content", key = "#bookId + '-' + #page")
    public BookContentDTO getBookContent(String bookId, int page) {
        BookContent content = contentRepository.findByBookIdAndPageNumber(
            UUID.fromString(bookId), page)
            .orElseThrow(() -> new BookContentNotFoundException(
                "Content not found for book: " + bookId + ", page: " + page));
                
        return BookContentDTO.builder()
            .pageNumber(content.getPageNumber())
            .content(content.getContent())
            .wordCount(content.getWordCount())
            .build();
    }
    
    @Async
    public CompletableFuture<BookDTO> uploadBook(MultipartFile file, BookMetadataRequest metadata) {
        log.info("Starting book upload: {}", metadata.getTitle());
        
        try {
            // 1. 存储文件
            String filePath = fileStorageService.store(file);
            
            // 2. 提取文本内容
            String textContent = extractTextContent(file);
            
            // 3. 创建书籍记录
            Book book = Book.builder()
                .title(metadata.getTitle())
                .author(metadata.getAuthor())
                .isbn(metadata.getIsbn())
                .categoryId(metadata.getCategoryId())
                .description(metadata.getDescription())
                .filePath(filePath)
                .fileSize(file.getSize())
                .build();
                
            book = bookRepository.save(book);
            
            // 4. 分页处理内容
            List<BookContent> contents = splitIntoPages(textContent, book.getId());
            contentRepository.saveAll(contents);
            
            // 5. 异步生成向量嵌入
            embeddingService.generateEmbeddingsAsync(book.getId(), textContent);
            
            log.info("Book upload completed: {}", book.getId());
            return CompletableFuture.completedFuture(convertToDTO(book));
            
        } catch (Exception e) {
            log.error("Book upload failed: {}", e.getMessage(), e);
            throw new BookUploadException("Failed to upload book", e);
        }
    }
    
    private String extractTextContent(MultipartFile file) {
        // PDF/EPUB文本提取逻辑
        String fileName = file.getOriginalFilename();
        
        if (fileName.endsWith(".pdf")) {
            return pdfTextExtractor.extract(file);
        } else if (fileName.endsWith(".epub")) {
            return epubTextExtractor.extract(file);
        } else {
            throw new UnsupportedFileTypeException("Unsupported file type: " + fileName);
        }
    }
}
```

### 2. AI服务集成

```java
// core/ai-service/AIService.java
@Service
@Slf4j
public class AIService {
    
    private final ChatClient chatClient;
    private final EmbeddingClient embeddingClient;
    private final VectorStore vectorStore;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    
    @Value("${spring.ai.openai.chat.options.model:gpt-3.5-turbo}")
    private String chatModel;
    
    @Value("${app.ai.max-tokens:2048}")
    private int maxTokens;
    
    public AISearchResponse intelligentSearch(AISearchRequest request, String userId) {
        log.info("Processing AI search for user {}: {}", userId, request.getQuery());
        
        try {
            // 1. 记录搜索日志
            logSearchRequest(userId, request);
            
            // 2. 向量化查询
            List<Double> queryEmbedding = embeddingClient.embed(request.getQuery());
            
            // 3. 相似性搜索
            SearchRequest searchRequest = SearchRequest.query(request.getQuery())
                .withTopK(request.getTopK())
                .withSimilarityThreshold(0.7);
                
            List<Document> similarDocs = vectorStore.similaritySearch(searchRequest);
            
            // 4. 构建上下文
            String context = buildSearchContext(similarDocs);
            
            // 5. 生成AI响应
            String prompt = buildSearchPrompt(request.getQuery(), context);
            ChatResponse response = chatClient.call(new Prompt(prompt, 
                OpenAiChatOptions.builder()
                    .withModel(chatModel)
                    .withMaxTokens(maxTokens)
                    .withTemperature(0.7f)
                    .build()));
            
            // 6. 构建响应
            return AISearchResponse.builder()
                .answer(response.getResult().getOutput().getContent())
                .sources(extractBookSources(similarDocs))
                .confidence(calculateConfidence(similarDocs))
                .tokensUsed(response.getMetadata().getUsage().getTotalTokens())
                .build();
                
        } catch (Exception e) {
            log.error("AI search failed for user {}: {}", userId, e.getMessage(), e);
            throw new AIServiceException("AI搜索服务暂时不可用", e);
        }
    }
    
    public Flux<ChatResponseChunk> chatStream(ChatRequest request, String userId) {
        return Flux.create(sink -> {
            try {
                // 1. 获取或创建对话
                Conversation conversation = getOrCreateConversation(
                    request.getConversationId(), userId, request.getBookId());
                
                // 2. 保存用户消息
                Message userMessage = saveUserMessage(conversation.getId(), request.getMessage());
                
                // 3. 构建对话上下文
                String conversationContext = buildConversationContext(conversation.getId());
                
                // 4. 流式AI响应
                String prompt = buildChatPrompt(request.getMessage(), conversationContext);
                
                chatClient.stream(new Prompt(prompt))
                    .doOnNext(chatResponse -> {
                        ChatResponseChunk chunk = ChatResponseChunk.builder()
                            .content(chatResponse.getResult().getOutput().getContent())
                            .type("content")
                            .timestamp(Instant.now())
                            .build();
                        sink.next(chunk);
                    })
                    .doOnComplete(() -> {
                        // 保存AI响应
                        String fullResponse = // 聚合所有chunks
                        saveAssistantMessage(conversation.getId(), fullResponse);
                        
                        sink.next(ChatResponseChunk.builder()
                            .type("done")
                            .timestamp(Instant.now())
                            .build());
                        sink.complete();
                    })
                    .doOnError(error -> {
                        log.error("Chat stream error: {}", error.getMessage(), error);
                        sink.error(new AIServiceException("对话服务出现错误", error));
                    })
                    .subscribe();
                    
            } catch (Exception e) {
                sink.error(new AIServiceException("无法启动对话", e));
            }
        });
    }
    
    private String buildSearchPrompt(String query, String context) {
        return String.format("""
            你是一个专业的图书助手。基于以下书籍内容，回答用户的问题。
            
            相关书籍内容：
            %s
            
            用户问题：%s
            
            请提供准确、有用的回答，如果内容中没有相关信息，请诚实说明。
            回答要简洁明了，重点突出。
            """, context, query);
    }
    
    private String buildChatPrompt(String message, String conversationContext) {
        return String.format("""
            你是一个智能书籍助手，正在与用户进行对话。
            
            对话历史：
            %s
            
            用户消息：%s
            
            请基于对话历史和书籍内容，提供有帮助的回答。
            保持对话的连贯性和上下文感知。
            """, conversationContext, message);
    }
}
```

### 3. Spring AI配置

```java
// config/AIConfig.java
@Configuration
@EnableConfigurationProperties(AIProperties.class)
@Slf4j
public class AIConfig {
    
    @Bean
    @ConditionalOnProperty(name = "spring.ai.openai.api-key")
    public OpenAiChatClient openAiChatClient(AIProperties properties) {
        return new OpenAiChatClient(
            new OpenAiApi(properties.getOpenai().getApiKey()),
            OpenAiChatOptions.builder()
                .withModel(properties.getOpenai().getModel())
                .withTemperature(properties.getOpenai().getTemperature())
                .withMaxTokens(properties.getOpenai().getMaxTokens())
                .build()
        );
    }
    
    @Bean
    @ConditionalOnProperty(name = "spring.ai.openai.api-key")
    public OpenAiEmbeddingClient openAiEmbeddingClient(AIProperties properties) {
        return new OpenAiEmbeddingClient(
            new OpenAiApi(properties.getOpenai().getApiKey()),
            OpenAiEmbeddingOptions.builder()
                .withModel("text-embedding-ada-002")
                .build()
        );
    }
    
    @Bean
    @ConditionalOnProperty(name = "app.vector-store.type", havingValue = "chroma")
    public ChromaVectorStore chromaVectorStore(EmbeddingClient embeddingClient) {
        return new ChromaVectorStore(embeddingClient, "http://localhost:8000");
    }
    
    @Bean
    @ConditionalOnProperty(name = "app.vector-store.type", havingValue = "pinecone")
    public PineconeVectorStore pineconeVectorStore(
            EmbeddingClient embeddingClient,
            AIProperties properties) {
        return new PineconeVectorStore(
            embeddingClient,
            properties.getPinecone().getApiKey(),
            properties.getPinecone().getEnvironment(),
            properties.getPinecone().getIndexName()
        );
    }
}

// config/AIProperties.java
@ConfigurationProperties(prefix = "app.ai")
@Data
public class AIProperties {
    
    private OpenAI openai = new OpenAI();
    private Pinecone pinecone = new Pinecone();
    private VectorStore vectorStore = new VectorStore();
    
    @Data
    public static class OpenAI {
        private String apiKey;
        private String model = "gpt-3.5-turbo";
        private Float temperature = 0.7f;
        private Integer maxTokens = 2048;
    }
    
    @Data
    public static class Pinecone {
        private String apiKey;
        private String environment;
        private String indexName;
    }
    
    @Data
    public static class VectorStore {
        private String type = "chroma"; // chroma, pinecone
        private String url = "http://localhost:8000";
    }
}
```

## 数据访问层设计

### 1. 实体类设计

```java
// data/entity/Book.java
@Entity
@Table(name = "books")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Book {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, length = 500)
    private String title;
    
    @Column(nullable = false, length = 200)
    private String author;
    
    @Column(unique = true, length = 20)
    private String isbn;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "cover_url", length = 500)
    private String coverUrl;
    
    @Column(name = "file_path", length = 500)
    private String filePath;
    
    @Column(name = "page_count")
    private Integer pageCount;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BookContent> contents = new ArrayList<>();
    
    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReadingRecord> readingRecords = new ArrayList<>();
}

// data/entity/BookContent.java
@Entity
@Table(name = "book_contents", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"book_id", "page_number"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookContent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;
    
    @Column(name = "page_number", nullable = false)
    private Integer pageNumber;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "word_count")
    private Integer wordCount;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}

// data/entity/Conversation.java
@Entity
@Table(name = "ai_conversations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    private Book book;
    
    @Column(length = 200)
    private String title;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    private List<Message> messages = new ArrayList<>();
}
```

### 2. Repository设计

```java
// data/repository/BookRepository.java
@Repository
public interface BookRepository extends JpaRepository<Book, UUID>, JpaSpecificationExecutor<Book> {
    
    Optional<Book> findByIsbn(String isbn);
    
    @Query("SELECT b FROM Book b WHERE b.category.id = :categoryId ORDER BY b.createdAt DESC")
    Page<Book> findByCategoryId(@Param("categoryId") UUID categoryId, Pageable pageable);
    
    @Query("SELECT b FROM Book b WHERE " +
           "LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(b.author) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(b.description) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Book> searchBooks(@Param("query") String query);
    
    @Query("SELECT COUNT(rr) FROM ReadingRecord rr WHERE rr.book.id = :bookId")
    Long getReadersCount(@Param("bookId") UUID bookId);
    
    @Modifying
    @Query("UPDATE Book b SET b.pageCount = :pageCount WHERE b.id = :bookId")
    void updatePageCount(@Param("bookId") UUID bookId, @Param("pageCount") Integer pageCount);
}

// data/repository/BookContentRepository.java
@Repository
public interface BookContentRepository extends JpaRepository<BookContent, UUID> {
    
    Optional<BookContent> findByBookIdAndPageNumber(UUID bookId, Integer pageNumber);
    
    @Query("SELECT bc FROM BookContent bc WHERE bc.book.id = :bookId ORDER BY bc.pageNumber")
    List<BookContent> findByBookIdOrderByPageNumber(@Param("bookId") UUID bookId);
    
    @Query("SELECT MAX(bc.pageNumber) FROM BookContent bc WHERE bc.book.id = :bookId")
    Optional<Integer> findMaxPageNumberByBookId(@Param("bookId") UUID bookId);
    
    @Query("SELECT bc FROM BookContent bc WHERE bc.book.id = :bookId AND " +
           "bc.pageNumber BETWEEN :startPage AND :endPage ORDER BY bc.pageNumber")
    List<BookContent> findByBookIdAndPageRange(
        @Param("bookId") UUID bookId, 
        @Param("startPage") Integer startPage, 
        @Param("endPage") Integer endPage);
}

// data/repository/ConversationRepository.java
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    
    @Query("SELECT c FROM Conversation c WHERE c.user.id = :userId ORDER BY c.updatedAt DESC")
    Page<Conversation> findByUserIdOrderByUpdatedAtDesc(@Param("userId") UUID userId, Pageable pageable);
    
    @Query("SELECT c FROM Conversation c WHERE c.user.id = :userId AND c.book.id = :bookId")
    Optional<Conversation> findByUserIdAndBookId(@Param("userId") UUID userId, @Param("bookId") UUID bookId);
    
    @Modifying
    @Query("DELETE FROM Conversation c WHERE c.user.id = :userId AND c.id = :conversationId")
    void deleteByUserIdAndId(@Param("userId") UUID userId, @Param("conversationId") UUID conversationId);
}
```

## 安全配置

### 1. Spring Security配置

```java
// security/SecurityConfig.java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {
    
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final JwtTokenProvider jwtTokenProvider;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(jwtAccessDeniedHandler))
            .authorizeHttpRequests(auth -> auth
                // 公开端点
                .requestMatchers(HttpMethod.GET, "/api/v1/books/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/health/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                
                // AI功能需要认证
                .requestMatchers("/api/v1/ai/**").hasRole("USER")
                
                // 管理功能需要管理员权限
                .requestMatchers(HttpMethod.POST, "/api/v1/books").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/books/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/books/**").hasRole("ADMIN")
                
                .anyRequest().authenticated())
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard")
                .failureUrl("/login?error"))
            .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), 
                UsernamePasswordAuthenticationFilter.class)
            .build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}

// security/JwtTokenProvider.java
@Component
@Slf4j
public class JwtTokenProvider {
    
    @Value("${app.jwt.secret}")
    private String jwtSecret;
    
    @Value("${app.jwt.expiration:86400000}") // 24小时
    private long jwtExpirationMs;
    
    public String generateToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Date expiryDate = new Date(System.currentTimeMillis() + jwtExpirationMs);
        
        return Jwts.builder()
            .setSubject(userPrincipal.getId().toString())
            .setIssuedAt(new Date())
            .setExpiration(expiryDate)
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
    }
    
    public String getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody();
        return claims.getSubject();
    }
    
    public boolean validateToken(String authToken) {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(authToken);
            return true;
        } catch (SignatureException ex) {
            log.error("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty");
        }
        return false;
    }
}
```

## 缓存策略

### 1. Redis缓存配置

```java
// config/CacheConfig.java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(config)
            .withCacheConfiguration("books", config.entryTtl(Duration.ofHours(1)))
            .withCacheConfiguration("book-content", config.entryTtl(Duration.ofHours(6)))
            .withCacheConfiguration("user-profile", config.entryTtl(Duration.ofMinutes(30)))
            .withCacheConfiguration("ai-search", config.entryTtl(Duration.ofMinutes(15)))
            .build();
    }
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}

// service/CacheService.java
@Service
@Slf4j
public class CacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheManager cacheManager;
    
    public void cacheUserSession(String userId, UserSession session) {
        String key = "user_session:" + userId;
        redisTemplate.opsForValue().set(key, session, Duration.ofHours(24));
    }
    
    public Optional<UserSession> getUserSession(String userId) {
        String key = "user_session:" + userId;
        UserSession session = (UserSession) redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(session);
    }
    
    public void cacheAIResponse(String query, AISearchResponse response) {
        String key = "ai_response:" + DigestUtils.md5DigestAsHex(query.getBytes());
        redisTemplate.opsForValue().set(key, response, Duration.ofMinutes(30));
    }
    
    public void invalidateBookCache(String bookId) {
        Cache bookCache = cacheManager.getCache("books");
        Cache contentCache = cacheManager.getCache("book-content");
        
        if (bookCache != null) {
            bookCache.evict(bookId);
        }
        
        if (contentCache != null) {
            // 清除该书籍的所有页面缓存
            redisTemplate.delete(
                redisTemplate.keys("book-content::" + bookId + "-*")
            );
        }
    }
}
```

## 异步处理和消息队列

### 1. 异步任务配置

```java
// config/AsyncConfig.java
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean(name = "taskExecutor")
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-task-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
    
    @Bean(name = "aiTaskExecutor")
    public TaskExecutor aiTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ai-task-");
        executor.initialize();
        return executor;
    }
}

// service/AsyncBookProcessingService.java
@Service
@Slf4j
public class AsyncBookProcessingService {
    
    private final VectorStore vectorStore;
    private final EmbeddingClient embeddingClient;
    private final BookContentRepository contentRepository;
    
    @Async("aiTaskExecutor")
    public CompletableFuture<Void> generateBookEmbeddings(UUID bookId) {
        log.info("Starting embedding generation for book: {}", bookId);
        
        try {
            List<BookContent> contents = contentRepository.findByBookIdOrderByPageNumber(bookId);
            
            for (BookContent content : contents) {
                // 生成向量嵌入
                List<Double> embedding = embeddingClient.embed(content.getContent());
                
                // 存储到向量数据库
                Document document = new Document(content.getContent(), 
                    Map.of(
                        "bookId", bookId.toString(),
                        "pageNumber", content.getPageNumber(),
                        "contentId", content.getId().toString()
                    ));
                document.setEmbedding(embedding);
                
                vectorStore.add(List.of(document));
            }
            
            log.info("Embedding generation completed for book: {}", bookId);
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            log.error("Failed to generate embeddings for book: {}", bookId, e);
            throw new CompletionException(e);
        }
    }
    
    @Async("taskExecutor")
    public CompletableFuture<Void> processBookUpload(MultipartFile file, BookMetadataRequest metadata) {
        // 异步处理书籍上传
        // 包括文件存储、文本提取、内容分页等
        return CompletableFuture.completedFuture(null);
    }
}
```

### 2. 消息队列集成

```java
// config/RabbitMQConfig.java
@Configuration
@EnableRabbit
public class RabbitMQConfig {
    
    public static final String BOOK_PROCESSING_QUEUE = "book.processing";
    public static final String AI_EMBEDDING_QUEUE = "ai.embedding";
    public static final String NOTIFICATION_QUEUE = "notification";
    
    @Bean
    public Queue bookProcessingQueue() {
        return QueueBuilder.durable(BOOK_PROCESSING_QUEUE).build();
    }
    
    @Bean
    public Queue aiEmbeddingQueue() {
        return QueueBuilder.durable(AI_EMBEDDING_QUEUE).build();
    }
    
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE).build();
    }
    
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        return template;
    }
}

// service/MessageProducerService.java
@Service
@Slf4j
public class MessageProducerService {
    
    private final RabbitTemplate rabbitTemplate;
    
    public void sendBookProcessingMessage(BookProcessingMessage message) {
        log.info("Sending book processing message: {}", message);
        rabbitTemplate.convertAndSend(BOOK_PROCESSING_QUEUE, message);
    }
    
    public void sendEmbeddingGenerationMessage(EmbeddingMessage message) {
        log.info("Sending embedding generation message: {}", message);
        rabbitTemplate.convertAndSend(AI_EMBEDDING_QUEUE, message);
    }
}

// listener/BookProcessingListener.java
@Component
@Slf4j
public class BookProcessingListener {
    
    private final AsyncBookProcessingService bookProcessingService;
    
    @RabbitListener(queues = BOOK_PROCESSING_QUEUE)
    public void handleBookProcessing(BookProcessingMessage message) {
        log.info("Received book processing message: {}", message);
        
        try {
            bookProcessingService.processBookUpload(message.getBookId(), message.getFilePath());
            log.info("Book processing completed: {}", message.getBookId());
        } catch (Exception e) {
            log.error("Book processing failed: {}", message.getBookId(), e);
            // 发送失败通知
        }
    }
    
    @RabbitListener(queues = AI_EMBEDDING_QUEUE)
    public void handleEmbeddingGeneration(EmbeddingMessage message) {
        log.info("Received embedding generation message: {}", message);
        
        try {
            bookProcessingService.generateBookEmbeddings(message.getBookId()).get();
            log.info("Embedding generation completed: {}", message.getBookId());
        } catch (Exception e) {
            log.error("Embedding generation failed: {}", message.getBookId(), e);
        }
    }
}
```

## 监控和可观测性

### 1. 应用监控

```java
// config/MonitoringConfig.java
@Configuration
public class MonitoringConfig {
    
    @Bean
    public MeterRegistry meterRegistry() {
        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }
    
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
    
    @Bean
    public CountedAspect countedAspect(MeterRegistry registry) {
        return new CountedAspect(registry);
    }
}

// service/MetricsService.java
@Service
@Slf4j
public class MetricsService {
    
    private final MeterRegistry meterRegistry;
    private final Counter aiRequestCounter;
    private final Timer aiResponseTimer;
    
    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.aiRequestCounter = Counter.builder("ai.requests.total")
            .description("Total AI requests")
            .register(meterRegistry);
        this.aiResponseTimer = Timer.builder("ai.response.time")
            .description("AI response time")
            .register(meterRegistry);
    }
    
    public void recordAIRequest(String userId, String query, boolean success) {
        aiRequestCounter.increment(
            Tags.of(
                "user_id", userId,
                "success", String.valueOf(success)
            )
        );
    }
    
    public Timer.Sample startAIResponseTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordAIResponseTime(Timer.Sample sample, String operation) {
        sample.stop(Timer.builder("ai.response.time")
            .tag("operation", operation)
            .register(meterRegistry));
    }
}
```

### 2. 健康检查

```java
// actuator/CustomHealthIndicator.java
@Component
public class AIServiceHealthIndicator implements HealthIndicator {
    
    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    
    @Override
    public Health health() {
        try {
            // 检查AI服务连通性
            ChatResponse response = chatClient.call(new Prompt("health check"));
            
            if (response != null) {
                return Health.up()
                    .withDetail("ai-service", "UP")
                    .withDetail("model", "gpt-3.5-turbo")
                    .build();
            } else {
                return Health.down()
                    .withDetail("ai-service", "DOWN")
                    .withDetail("reason", "No response from AI service")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("ai-service", "DOWN")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}

@Component
public class VectorStoreHealthIndicator implements HealthIndicator {
    
    private final VectorStore vectorStore;
    
    @Override
    public Health health() {
        try {
            // 执行简单的向量搜索测试
            List<Document> results = vectorStore.similaritySearch(
                SearchRequest.query("test").withTopK(1)
            );
            
            return Health.up()
                .withDetail("vector-store", "UP")
                .withDetail("documents-count", results.size())
                .build();
                
        } catch (Exception e) {
            return Health.down()
                .withDetail("vector-store", "DOWN")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

## 配置管理

### 1. 应用配置

```yaml
# application.yml
spring:
  application:
    name: kong-ai-books
  
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/kong_ai_books}
    username: ${DATABASE_USERNAME:postgres}
    password: ${DATABASE_PASSWORD:password}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        use_sql_comments: true
  
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
  
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: ${OPENAI_MODEL:gpt-3.5-turbo}
          temperature: 0.7
          max-tokens: 2048
      embedding:
        options:
          model: text-embedding-ada-002

# 应用自定义配置
app:
  jwt:
    secret: ${JWT_SECRET:your-secret-key}
    expiration: 86400000 # 24小时
  
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      model: ${OPENAI_MODEL:gpt-3.5-turbo}
      temperature: 0.7
      max-tokens: 2048
    
    pinecone:
      api-key: ${PINECONE_API_KEY}
      environment: ${PINECONE_ENVIRONMENT:us-west1-gcp}
      index-name: ${PINECONE_INDEX:kong-ai-books}
    
    vector-store:
      type: ${VECTOR_STORE_TYPE:chroma}
      url: ${VECTOR_STORE_URL:http://localhost:8000}
  
  storage:
    type: ${STORAGE_TYPE:local} # local, minio, s3
    local:
      upload-dir: ${UPLOAD_DIR:./uploads}
    minio:
      endpoint: ${MINIO_ENDPOINT:http://localhost:9000}
      access-key: ${MINIO_ACCESS_KEY}
      secret-key: ${MINIO_SECRET_KEY}
      bucket-name: ${MINIO_BUCKET:kong-ai-books}

# 监控配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
  metrics:
    export:
      prometheus:
        enabled: true

# 日志配置
logging:
  level:
    com.kong.aibooks: DEBUG
    org.springframework.ai: DEBUG
    org.springframework.security: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/kong-ai-books.log
```

### 2. 环境特定配置

```yaml
# application-dev.yml
spring:
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: update
  
  ai:
    openai:
      chat:
        options:
          model: gpt-3.5-turbo # 开发环境使用较便宜的模型

app:
  ai:
    vector-store:
      type: chroma
      url: http://localhost:8000

logging:
  level:
    root: INFO
    com.kong.aibooks: DEBUG

---
# application-prod.yml
spring:
  jpa:
    show-sql: false
    hibernate:
      ddl-auto: validate
  
  ai:
    openai:
      chat:
        options:
          model: gpt-4 # 生产环境使用更强的模型

app:
  ai:
    vector-store:
      type: pinecone

logging:
  level:
    root: WARN
    com.kong.aibooks: INFO
```

## API文档配置

### OpenAPI配置

```java
// config/OpenAPIConfig.java
@Configuration
public class OpenAPIConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Kong AI Books API")
                .version("1.0")
                .description("智能书籍平台API文档")
                .contact(new Contact()
                    .name("Kong AI Books Team")
                    .email("support@kong-ai-books.com")))
            .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
            .components(new Components()
                .addSecuritySchemes("Bearer Authentication", 
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
```

## 部署配置

### 1. Docker配置

```dockerfile
# Dockerfile
FROM openjdk:17-jdk-slim

WORKDIR /app

# 复制依赖文件
COPY target/*.jar app.jar

# 创建非root用户
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# 启动应用
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# 暴露端口
EXPOSE 8080
```

### 2. Kubernetes部署

```yaml
# k8s/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: kong-ai-books-backend
  labels:
    app: kong-ai-books-backend
spec:
  replicas: 3
  selector:
    matchLabels:
      app: kong-ai-books-backend
  template:
    metadata:
      labels:
        app: kong-ai-books-backend
    spec:
      containers:
      - name: backend
        image: kong-ai-books/backend:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: database-secret
              key: url
        - name: OPENAI_API_KEY
          valueFrom:
            secretKeyRef:
              name: ai-secret
              key: openai-api-key
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10

---
apiVersion: v1
kind: Service
metadata:
  name: kong-ai-books-backend-service
spec:
  selector:
    app: kong-ai-books-backend
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
  type: ClusterIP
```

## 性能优化

### 1. 数据库优化

```java
// config/DatabaseConfig.java
@Configuration
public class DatabaseConfig {
    
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create()
            .type(HikariDataSource.class)
            .build();
    }
    
    @Bean
    @ConfigurationProperties("spring.datasource.readonly")
    public DataSource readOnlyDataSource() {
        return DataSourceBuilder.create()
            .type(HikariDataSource.class)
            .build();
    }
    
    @Bean
    public PlatformTransactionManager transactionManager() {
        return new JpaTransactionManager();
    }
}

// 读写分离配置
@Service
@Transactional(readOnly = true)
public class BookQueryService {
    
    @Autowired
    @Qualifier("readOnlyDataSource")
    private DataSource readOnlyDataSource;
    
    // 只读查询使用只读数据源
    public List<Book> findPopularBooks() {
        // 查询逻辑
    }
}
```

### 2. 缓存预热

```java
// service/CacheWarmupService.java
@Service
@Slf4j
public class CacheWarmupService {
    
    private final BookService bookService;
    private final CategoryService categoryService;
    
    @EventListener(ApplicationReadyEvent.class)
    public void warmupCache() {
        log.info("Starting cache warmup...");
        
        CompletableFuture.allOf(
            CompletableFuture.runAsync(this::warmupPopularBooks),
            CompletableFuture.runAsync(this::warmupCategories),
            CompletableFuture.runAsync(this::warmupRecommendations)
        ).join();
        
        log.info("Cache warmup completed");
    }
    
    private void warmupPopularBooks() {
        try {
            bookService.getPopularBooks(PageRequest.of(0, 20));
            log.debug("Popular books cache warmed up");
        } catch (Exception e) {
            log.warn("Failed to warm up popular books cache", e);
        }
    }
}
```

这个后端架构设计提供了:

1. **模块化设计**: 清晰的分层架构和模块划分
2. **Spring AI集成**: 完整的AI能力集成
3. **高性能**: 缓存、异步处理、数据库优化
4. **高可用**: 集群部署、健康检查、故障恢复
5. **安全性**: JWT认证、权限控制、数据保护
6. **可观测性**: 完整的监控、日志、链路追踪
7. **可扩展性**: 微服务架构、消息队列、水平扩展

该架构可以支持从单体应用到微服务的平滑演进。