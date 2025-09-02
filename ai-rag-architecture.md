# AI/RAG系统架构设计 - Kong AI Books

## RAG系统概述

RAG (Retrieval-Augmented Generation) 系统是Kong AI Books的核心AI能力，提供智能搜索、问答和推荐功能。

### 核心特性
- **智能文档检索**: 基于语义相似性的精准检索
- **上下文感知生成**: 结合检索结果的智能回答
- **多模态支持**: 文本、图片、表格内容理解
- **个性化推荐**: 基于用户行为的智能推荐

## RAG系统架构

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   文档摄取       │    │   向量检索       │    │   生成增强       │
│  Document       │───►│   Vector        │───►│   Generation    │
│  Ingestion      │    │   Retrieval     │    │   Enhancement   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ 文本预处理       │    │ 相似性搜索       │    │ 提示词工程       │
│ 分块策略        │    │ 重排序算法       │    │ 上下文构建       │
│ 向量化          │    │ 混合搜索        │    │ 响应后处理       │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 文档处理流水线

### 1. 文档摄取服务

```java
// ai-service/DocumentIngestionService.java
@Service
@Slf4j
public class DocumentIngestionService {
    
    private final DocumentLoader documentLoader;
    private final DocumentSplitter documentSplitter;
    private final EmbeddingClient embeddingClient;
    private final VectorStore vectorStore;
    private final DocumentMetadataEnricher metadataEnricher;
    
    @Async("aiTaskExecutor")
    public CompletableFuture<Void> ingestDocument(String bookId, String filePath) {
        log.info("Starting document ingestion for book: {}", bookId);
        
        try {
            // 1. 加载文档
            List<Document> documents = loadDocument(filePath);
            
            // 2. 文档预处理
            documents = preprocessDocuments(documents, bookId);
            
            // 3. 文档分块
            List<Document> chunks = splitDocuments(documents);
            
            // 4. 丰富元数据
            chunks = enrichMetadata(chunks, bookId);
            
            // 5. 生成向量嵌入
            generateEmbeddings(chunks);
            
            // 6. 存储到向量数据库
            vectorStore.add(chunks);
            
            log.info("Document ingestion completed for book: {}", bookId);
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            log.error("Document ingestion failed for book: {}", bookId, e);
            throw new CompletionException(e);
        }
    }
    
    private List<Document> loadDocument(String filePath) {
        if (filePath.endsWith(".pdf")) {
            return new PagePdfDocumentReader(filePath).get();
        } else if (filePath.endsWith(".epub")) {
            return new EpubDocumentReader(filePath).get();
        } else if (filePath.endsWith(".txt")) {
            return new TextDocumentReader(filePath).get();
        } else {
            throw new UnsupportedDocumentTypeException("Unsupported file type: " + filePath);
        }
    }
    
    private List<Document> preprocessDocuments(List<Document> documents, String bookId) {
        return documents.stream()
            .map(doc -> {
                String content = doc.getContent();
                
                // 清理文本
                content = cleanText(content);
                
                // 提取结构化信息
                content = extractStructuredInfo(content);
                
                // 添加书籍上下文
                Map<String, Object> metadata = new HashMap<>(doc.getMetadata());
                metadata.put("bookId", bookId);
                metadata.put("processedAt", Instant.now().toString());
                
                return new Document(content, metadata);
            })
            .collect(Collectors.toList());
    }
    
    private List<Document> splitDocuments(List<Document> documents) {
        // 使用语义分块策略
        TokenTextSplitter splitter = new TokenTextSplitter(
            500,  // 块大小
            100   // 重叠大小
        );
        
        return documents.stream()
            .flatMap(doc -> splitter.split(doc).stream())
            .collect(Collectors.toList());
    }
    
    private String cleanText(String text) {
        return text
            .replaceAll("\\s+", " ")           // 规范化空白字符
            .replaceAll("[\\x00-\\x1F]", "")   // 移除控制字符
            .replaceAll("\\p{Cntrl}", "")      // 移除其他控制字符
            .trim();
    }
}
```

### 2. 智能检索服务

```java
// ai-service/RetrievalService.java
@Service
@Slf4j
public class RetrievalService {
    
    private final VectorStore vectorStore;
    private final EmbeddingClient embeddingClient;
    private final ElasticsearchTemplate elasticsearchTemplate;
    private final RerankingService rerankingService;
    
    public List<Document> hybridSearch(String query, SearchContext context) {
        log.debug("Performing hybrid search for query: {}", query);
        
        // 1. 向量搜索
        List<Document> vectorResults = vectorSearch(query, context);
        
        // 2. 关键词搜索 (可选)
        List<Document> keywordResults = keywordSearch(query, context);
        
        // 3. 混合结果
        List<Document> hybridResults = mergeResults(vectorResults, keywordResults);
        
        // 4. 重排序
        List<Document> rerankedResults = rerankingService.rerank(query, hybridResults);
        
        log.debug("Hybrid search returned {} results", rerankedResults.size());
        return rerankedResults;
    }
    
    private List<Document> vectorSearch(String query, SearchContext context) {
        try {
            // 构建搜索请求
            SearchRequest.Builder builder = SearchRequest.query(query)
                .withTopK(context.getTopK() * 2) // 获取更多结果用于重排序
                .withSimilarityThreshold(context.getSimilarityThreshold());
            
            // 添加过滤条件
            if (context.getBookId() != null) {
                builder.withFilterExpression("bookId == '" + context.getBookId() + "'");
            }
            
            if (context.getCategory() != null) {
                builder.withFilterExpression("category == '" + context.getCategory() + "'");
            }
            
            SearchRequest searchRequest = builder.build();
            
            // 执行向量搜索
            List<Document> results = vectorStore.similaritySearch(searchRequest);
            
            log.debug("Vector search returned {} results", results.size());
            return results;
            
        } catch (Exception e) {
            log.error("Vector search failed: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    private List<Document> keywordSearch(String query, SearchContext context) {
        if (elasticsearchTemplate == null) {
            return Collections.emptyList();
        }
        
        try {
            // 构建Elasticsearch查询
            NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery(query)
                    .field("content", 2.0f)
                    .field("title", 3.0f)
                    .field("author", 1.5f)
                    .type(MultiMatchQueryBuilder.Type.BEST_FIELDS)
                    .fuzziness(Fuzziness.AUTO))
                .withPageable(PageRequest.of(0, context.getTopK()));
            
            // 添加过滤条件
            if (context.getBookId() != null) {
                queryBuilder.withFilter(QueryBuilders.termQuery("bookId", context.getBookId()));
            }
            
            SearchHits<DocumentIndex> searchHits = elasticsearchTemplate.search(
                queryBuilder.build(), DocumentIndex.class);
            
            return searchHits.stream()
                .map(hit -> convertToDocument(hit.getContent()))
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("Keyword search failed: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    private List<Document> mergeResults(List<Document> vectorResults, List<Document> keywordResults) {
        // 使用RRF (Reciprocal Rank Fusion) 算法合并结果
        Map<String, Document> documentMap = new HashMap<>();
        Map<String, Double> scoreMap = new HashMap<>();
        
        // 向量搜索结果评分
        for (int i = 0; i < vectorResults.size(); i++) {
            Document doc = vectorResults.get(i);
            String docId = doc.getId();
            double score = 1.0 / (i + 1); // RRF评分
            
            documentMap.put(docId, doc);
            scoreMap.put(docId, scoreMap.getOrDefault(docId, 0.0) + score);
        }
        
        // 关键词搜索结果评分
        for (int i = 0; i < keywordResults.size(); i++) {
            Document doc = keywordResults.get(i);
            String docId = doc.getId();
            double score = 1.0 / (i + 1); // RRF评分
            
            documentMap.put(docId, doc);
            scoreMap.put(docId, scoreMap.getOrDefault(docId, 0.0) + score * 0.7); // 权重调整
        }
        
        // 按评分排序
        return scoreMap.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .map(entry -> documentMap.get(entry.getKey()))
            .collect(Collectors.toList());
    }
}
```

### 3. 重排序服务

```java
// ai-service/RerankingService.java
@Service
@Slf4j
public class RerankingService {
    
    private final ChatClient chatClient;
    private final SentenceTransformerClient sentenceTransformer;
    
    public List<Document> rerank(String query, List<Document> documents) {
        if (documents.size() <= 3) {
            return documents; // 少量文档无需重排序
        }
        
        try {
            // 使用Cross-Encoder模型进行重排序
            return crossEncoderRerank(query, documents);
        } catch (Exception e) {
            log.warn("Reranking failed, falling back to original order: {}", e.getMessage());
            return documents;
        }
    }
    
    private List<Document> crossEncoderRerank(String query, List<Document> documents) {
        // 计算查询与文档的相关性分数
        List<DocumentScore> scores = documents.stream()
            .map(doc -> {
                double score = calculateRelevanceScore(query, doc.getContent());
                return new DocumentScore(doc, score);
            })
            .collect(Collectors.toList());
        
        // 按分数排序
        scores.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        
        return scores.stream()
            .map(DocumentScore::getDocument)
            .collect(Collectors.toList());
    }
    
    private double calculateRelevanceScore(String query, String content) {
        // 使用预训练的Cross-Encoder模型
        String prompt = String.format(
            "Rate the relevance of the following content to the query on a scale of 0-1.\n" +
            "Query: %s\n" +
            "Content: %s\n" +
            "Relevance score:", 
            query, content.substring(0, Math.min(content.length(), 500))
        );
        
        try {
            ChatResponse response = chatClient.call(new Prompt(prompt));
            String scoreText = response.getResult().getOutput().getContent().trim();
            return Double.parseDouble(scoreText);
        } catch (Exception e) {
            log.warn("Failed to calculate relevance score, using default: {}", e.getMessage());
            return 0.5; // 默认中等相关性
        }
    }
    
    @Data
    @AllArgsConstructor
    private static class DocumentScore {
        private Document document;
        private double score;
    }
}
```

## 提示词工程

### 1. 提示词模板管理

```java
// ai-service/PromptTemplateService.java
@Service
@Slf4j
public class PromptTemplateService {
    
    private final Map<String, PromptTemplate> templates = new HashMap<>();
    
    @PostConstruct
    public void initializeTemplates() {
        // 搜索提示词模板
        templates.put("search", new PromptTemplate("""
            你是一个专业的图书助手，专门帮助用户查找和理解书籍内容。
            
            基于以下相关书籍片段，回答用户的问题：
            
            {context}
            
            用户问题：{query}
            
            回答要求：
            1. 基于提供的书籍内容回答，不要编造信息
            2. 如果内容中没有相关信息，请诚实说明
            3. 提供具体的书籍来源和页码引用
            4. 回答要简洁明了，重点突出
            5. 使用中文回答
            
            回答：
            """));
        
        // 对话提示词模板
        templates.put("chat", new PromptTemplate("""
            你是Kong AI Books的智能助手，正在与用户进行关于书籍的对话。
            
            {book_context}
            
            对话历史：
            {conversation_history}
            
            当前用户消息：{user_message}
            
            请基于书籍内容和对话历史，提供有帮助的回答。保持对话的连贯性和上下文感知。
            """));
        
        // 推荐提示词模板
        templates.put("recommendation", new PromptTemplate("""
            基于用户的阅读历史和偏好，推荐相关书籍。
            
            用户阅读历史：
            {reading_history}
            
            用户偏好分析：
            {user_preferences}
            
            候选书籍：
            {candidate_books}
            
            请从候选书籍中选择最适合的5本书，并说明推荐理由。
            """));
    }
    
    public String buildPrompt(String templateName, Map<String, Object> variables) {
        PromptTemplate template = templates.get(templateName);
        if (template == null) {
            throw new IllegalArgumentException("Unknown template: " + templateName);
        }
        
        return template.render(variables);
    }
    
    public Prompt createSearchPrompt(String query, List<Document> context) {
        String contextText = context.stream()
            .map(doc -> formatDocumentForContext(doc))
            .collect(Collectors.joining("\n\n"));
        
        Map<String, Object> variables = Map.of(
            "query", query,
            "context", contextText
        );
        
        String promptText = buildPrompt("search", variables);
        
        return new Prompt(promptText, OpenAiChatOptions.builder()
            .withModel("gpt-4")
            .withTemperature(0.3f)
            .withMaxTokens(1024)
            .build());
    }
    
    private String formatDocumentForContext(Document doc) {
        Map<String, Object> metadata = doc.getMetadata();
        
        return String.format("""
            【来源】%s - 第%s页
            【内容】%s
            """,
            metadata.get("title"),
            metadata.get("pageNumber"),
            doc.getContent()
        );
    }
}
```

### 2. 上下文构建服务

```java
// ai-service/ContextBuilderService.java
@Service
@Slf4j
public class ContextBuilderService {
    
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final BookService bookService;
    
    public String buildConversationContext(UUID conversationId, int maxMessages) {
        List<Message> recentMessages = messageRepository
            .findByConversationIdOrderByCreatedAtDesc(conversationId, 
                PageRequest.of(0, maxMessages))
            .getContent();
        
        Collections.reverse(recentMessages); // 按时间正序
        
        return recentMessages.stream()
            .map(message -> String.format("%s: %s", 
                message.getRole().equals("user") ? "用户" : "助手",
                message.getContent()))
            .collect(Collectors.joining("\n"));
    }
    
    public String buildBookContext(UUID bookId) {
        Book book = bookService.getBook(bookId);
        
        return String.format("""
            当前讨论的书籍：
            书名：%s
            作者：%s
            分类：%s
            简介：%s
            """,
            book.getTitle(),
            book.getAuthor(),
            book.getCategory().getName(),
            book.getDescription()
        );
    }
    
    public SearchContext buildSearchContext(String query, String userId, UUID bookId) {
        SearchContext.Builder builder = SearchContext.builder()
            .query(query)
            .userId(userId)
            .topK(10)
            .similarityThreshold(0.7)
            .includeMetadata(true);
        
        if (bookId != null) {
            builder.bookId(bookId);
        }
        
        // 基于用户历史调整搜索参数
        adjustSearchParametersForUser(builder, userId);
        
        return builder.build();
    }
    
    private void adjustSearchParametersForUser(SearchContext.Builder builder, String userId) {
        // 分析用户搜索历史，调整搜索参数
        // 例如：经常搜索技术类书籍的用户，提高技术内容的权重
    }
}
```

## 向量数据库集成

### 1. 多向量存储支持

```java
// ai-service/VectorStoreManager.java
@Service
@Slf4j
public class VectorStoreManager {
    
    private final Map<String, VectorStore> vectorStores;
    private final String primaryVectorStore;
    
    public VectorStoreManager(
            @Autowired(required = false) ChromaVectorStore chromaVectorStore,
            @Autowired(required = false) PineconeVectorStore pineconeVectorStore,
            @Value("${app.ai.vector-store.type:chroma}") String primaryType) {
        
        this.vectorStores = new HashMap<>();
        this.primaryVectorStore = primaryType;
        
        if (chromaVectorStore != null) {
            vectorStores.put("chroma", chromaVectorStore);
        }
        
        if (pineconeVectorStore != null) {
            vectorStores.put("pinecone", pineconeVectorStore);
        }
        
        log.info("Initialized vector stores: {}, primary: {}", 
            vectorStores.keySet(), primaryVectorStore);
    }
    
    public VectorStore getPrimaryVectorStore() {
        VectorStore store = vectorStores.get(primaryVectorStore);
        if (store == null) {
            throw new IllegalStateException("Primary vector store not available: " + primaryVectorStore);
        }
        return store;
    }
    
    public List<Document> similaritySearch(SearchRequest request) {
        VectorStore store = getPrimaryVectorStore();
        
        try {
            return store.similaritySearch(request);
        } catch (Exception e) {
            log.error("Primary vector store search failed, trying fallback", e);
            
            // 尝试备用向量存储
            for (Map.Entry<String, VectorStore> entry : vectorStores.entrySet()) {
                if (!entry.getKey().equals(primaryVectorStore)) {
                    try {
                        log.info("Using fallback vector store: {}", entry.getKey());
                        return entry.getValue().similaritySearch(request);
                    } catch (Exception fallbackException) {
                        log.warn("Fallback vector store also failed: {}", entry.getKey());
                    }
                }
            }
            
            throw new VectorStoreException("All vector stores failed", e);
        }
    }
    
    public void add(List<Document> documents) {
        VectorStore store = getPrimaryVectorStore();
        
        try {
            store.add(documents);
            log.debug("Added {} documents to vector store", documents.size());
        } catch (Exception e) {
            log.error("Failed to add documents to vector store", e);
            throw new VectorStoreException("Failed to add documents", e);
        }
    }
}
```

### 2. Chroma集成

```java
// integration/ChromaVectorStoreClient.java
@Component
@ConditionalOnProperty(name = "app.ai.vector-store.type", havingValue = "chroma")
@Slf4j
public class ChromaVectorStoreClient {
    
    private final WebClient webClient;
    private final String chromaUrl;
    
    public ChromaVectorStoreClient(@Value("${app.ai.vector-store.url}") String chromaUrl) {
        this.chromaUrl = chromaUrl;
        this.webClient = WebClient.builder()
            .baseUrl(chromaUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }
    
    public void createCollection(String collectionName, Map<String, Object> metadata) {
        ChromaCreateCollectionRequest request = ChromaCreateCollectionRequest.builder()
            .name(collectionName)
            .metadata(metadata)
            .build();
        
        try {
            webClient.post()
                .uri("/api/v1/collections")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
                
            log.info("Created Chroma collection: {}", collectionName);
        } catch (Exception e) {
            log.error("Failed to create Chroma collection: {}", collectionName, e);
            throw new VectorStoreException("Failed to create collection", e);
        }
    }
    
    public void addDocuments(String collectionName, List<ChromaDocument> documents) {
        ChromaAddRequest request = ChromaAddRequest.builder()
            .documents(documents)
            .build();
        
        try {
            webClient.post()
                .uri("/api/v1/collections/{collection_name}/add", collectionName)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
                
            log.debug("Added {} documents to collection: {}", documents.size(), collectionName);
        } catch (Exception e) {
            log.error("Failed to add documents to collection: {}", collectionName, e);
            throw new VectorStoreException("Failed to add documents", e);
        }
    }
    
    public List<ChromaQueryResult> query(String collectionName, ChromaQueryRequest request) {
        try {
            return webClient.post()
                .uri("/api/v1/collections/{collection_name}/query", collectionName)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(ChromaQueryResult.class)
                .collectList()
                .block();
        } catch (Exception e) {
            log.error("Failed to query collection: {}", collectionName, e);
            throw new VectorStoreException("Failed to query collection", e);
        }
    }
}
```

## AI模型管理

### 1. 模型选择策略

```java
// ai-service/ModelSelectionService.java
@Service
@Slf4j
public class ModelSelectionService {
    
    private final Map<String, ChatClient> chatClients;
    private final ModelPerformanceTracker performanceTracker;
    
    public ModelSelectionService(
            @Autowired(required = false) OpenAiChatClient openAiChatClient,
            @Autowired(required = false) AnthropicChatClient anthropicChatClient,
            ModelPerformanceTracker performanceTracker) {
        
        this.chatClients = new HashMap<>();
        this.performanceTracker = performanceTracker;
        
        if (openAiChatClient != null) {
            chatClients.put("openai", openAiChatClient);
        }
        
        if (anthropicChatClient != null) {
            chatClients.put("anthropic", anthropicChatClient);
        }
    }
    
    public ChatClient selectOptimalModel(AITaskType taskType, String query) {
        switch (taskType) {
            case SEARCH:
                return selectSearchModel(query);
            case CHAT:
                return selectChatModel(query);
            case SUMMARIZATION:
                return selectSummarizationModel(query);
            default:
                return getDefaultModel();
        }
    }
    
    private ChatClient selectSearchModel(String query) {
        // 对于复杂查询，使用更强的模型
        if (isComplexQuery(query)) {
            return chatClients.get("openai"); // GPT-4
        } else {
            return chatClients.get("openai"); // GPT-3.5-turbo
        }
    }
    
    private boolean isComplexQuery(String query) {
        // 分析查询复杂度
        return query.length() > 100 || 
               query.contains("比较") || 
               query.contains("分析") ||
               query.contains("总结");
    }
    
    public enum AITaskType {
        SEARCH, CHAT, SUMMARIZATION, RECOMMENDATION
    }
}
```

### 2. 模型性能监控

```java
// ai-service/ModelPerformanceTracker.java
@Service
@Slf4j
public class ModelPerformanceTracker {
    
    private final MeterRegistry meterRegistry;
    private final RedisTemplate<String, Object> redisTemplate;
    
    public void recordModelUsage(String modelName, String taskType, 
                                long responseTimeMs, int tokensUsed, boolean success) {
        
        // 记录Prometheus指标
        Timer.builder("ai.model.response.time")
            .tag("model", modelName)
            .tag("task_type", taskType)
            .tag("success", String.valueOf(success))
            .register(meterRegistry)
            .record(responseTimeMs, TimeUnit.MILLISECONDS);
        
        Counter.builder("ai.model.requests.total")
            .tag("model", modelName)
            .tag("task_type", taskType)
            .tag("success", String.valueOf(success))
            .register(meterRegistry)
            .increment();
        
        Gauge.builder("ai.model.tokens.used")
            .tag("model", modelName)
            .tag("task_type", taskType)
            .register(meterRegistry, tokensUsed, Number::doubleValue);
        
        // 记录详细性能数据到Redis
        String key = String.format("model_perf:%s:%s:%s", 
            modelName, taskType, LocalDate.now());
        
        ModelPerformanceData data = ModelPerformanceData.builder()
            .modelName(modelName)
            .taskType(taskType)
            .responseTimeMs(responseTimeMs)
            .tokensUsed(tokensUsed)
            .success(success)
            .timestamp(Instant.now())
            .build();
        
        redisTemplate.opsForList().leftPush(key, data);
        redisTemplate.expire(key, Duration.ofDays(7)); // 保留7天
    }
    
    public ModelPerformanceStats getModelStats(String modelName, String taskType, Duration period) {
        String key = String.format("model_perf:%s:%s:*", modelName, taskType);
        
        // 从Redis聚合性能数据
        List<ModelPerformanceData> data = getPerformanceData(key, period);
        
        return ModelPerformanceStats.builder()
            .modelName(modelName)
            .taskType(taskType)
            .totalRequests(data.size())
            .successRate(calculateSuccessRate(data))
            .averageResponseTime(calculateAverageResponseTime(data))
            .averageTokensUsed(calculateAverageTokensUsed(data))
            .build();
    }
}
```

## 智能推荐系统

### 1. 推荐算法实现

```java
// ai-service/RecommendationService.java
@Service
@Slf4j
public class RecommendationService {
    
    private final UserBehaviorAnalyzer behaviorAnalyzer;
    private final ContentBasedRecommender contentRecommender;
    private final CollaborativeFilteringRecommender collaborativeRecommender;
    private final ChatClient chatClient;
    
    public List<BookRecommendation> getPersonalizedRecommendations(String userId, int limit) {
        log.info("Generating personalized recommendations for user: {}", userId);
        
        try {
            // 1. 分析用户行为
            UserProfile userProfile = behaviorAnalyzer.analyzeUser(userId);
            
            // 2. 多策略推荐
            List<BookRecommendation> contentBased = contentRecommender
                .recommend(userProfile, limit);
            
            List<BookRecommendation> collaborative = collaborativeRecommender
                .recommend(userProfile, limit);
            
            // 3. AI增强推荐
            List<BookRecommendation> aiEnhanced = enhanceWithAI(
                userProfile, contentBased, collaborative);
            
            // 4. 融合和排序
            List<BookRecommendation> finalRecommendations = mergeRecommendations(
                contentBased, collaborative, aiEnhanced, limit);
            
            log.info("Generated {} recommendations for user: {}", 
                finalRecommendations.size(), userId);
            
            return finalRecommendations;
            
        } catch (Exception e) {
            log.error("Failed to generate recommendations for user: {}", userId, e);
            return getFallbackRecommendations(limit);
        }
    }
    
    private List<BookRecommendation> enhanceWithAI(UserProfile userProfile,
                                                   List<BookRecommendation> contentBased,
                                                   List<BookRecommendation> collaborative) {
        
        String prompt = buildRecommendationPrompt(userProfile, contentBased, collaborative);
        
        try {
            ChatResponse response = chatClient.call(new Prompt(prompt));
            return parseAIRecommendations(response.getResult().getOutput().getContent());
        } catch (Exception e) {
            log.warn("AI recommendation enhancement failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
    
    private String buildRecommendationPrompt(UserProfile userProfile,
                                           List<BookRecommendation> contentBased,
                                           List<BookRecommendation> collaborative) {
        
        return String.format("""
            基于用户画像和现有推荐结果，提供优化的书籍推荐。
            
            用户画像：
            - 阅读偏好：%s
            - 阅读历史：%s
            - 活跃时间：%s
            
            基于内容的推荐：
            %s
            
            协同过滤推荐：
            %s
            
            请分析这些推荐的合理性，并提供最终的推荐列表（包含书籍ID和推荐理由）。
            """,
            userProfile.getPreferences(),
            userProfile.getReadingHistory(),
            userProfile.getActiveHours(),
            formatRecommendations(contentBased),
            formatRecommendations(collaborative)
        );
    }
}
```

### 2. 用户行为分析

```java
// ai-service/UserBehaviorAnalyzer.java
@Service
@Slf4j
public class UserBehaviorAnalyzer {
    
    private final ReadingRecordRepository readingRecordRepository;
    private final SearchLogRepository searchLogRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    
    public UserProfile analyzeUser(String userId) {
        log.debug("Analyzing user behavior for: {}", userId);
        
        // 1. 阅读行为分析
        ReadingBehavior readingBehavior = analyzeReadingBehavior(userId);
        
        // 2. 搜索行为分析
        SearchBehavior searchBehavior = analyzeSearchBehavior(userId);
        
        // 3. 偏好分析
        UserPreferences preferences = analyzePreferences(userId);
        
        // 4. 活跃模式分析
        ActivityPattern activityPattern = analyzeActivityPattern(userId);
        
        return UserProfile.builder()
            .userId(userId)
            .readingBehavior(readingBehavior)
            .searchBehavior(searchBehavior)
            .preferences(preferences)
            .activityPattern(activityPattern)
            .lastAnalyzedAt(Instant.now())
            .build();
    }
    
    private ReadingBehavior analyzeReadingBehavior(String userId) {
        List<ReadingRecord> records = readingRecordRepository
            .findByUserIdAndLastReadAtAfter(
                UUID.fromString(userId), 
                LocalDateTime.now().minusDays(30)
            );
        
        return ReadingBehavior.builder()
            .totalBooksRead(records.size())
            .averageReadingTime(calculateAverageReadingTime(records))
            .preferredCategories(extractPreferredCategories(records))
            .readingSpeed(calculateReadingSpeed(records))
            .completionRate(calculateCompletionRate(records))
            .build();
    }
    
    private SearchBehavior analyzeSearchBehavior(String userId) {
        List<SearchLog> searchLogs = searchLogRepository
            .findByUserIdAndCreatedAtAfter(
                UUID.fromString(userId),
                LocalDateTime.now().minusDays(30)
            );
        
        return SearchBehavior.builder()
            .totalSearches(searchLogs.size())
            .averageQueryLength(calculateAverageQueryLength(searchLogs))
            .preferredSearchType(extractPreferredSearchType(searchLogs))
            .commonKeywords(extractCommonKeywords(searchLogs))
            .searchFrequency(calculateSearchFrequency(searchLogs))
            .build();
    }
}
```

## 实时AI对话

### 1. WebSocket配置

```java
// config/WebSocketConfig.java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    private final AIChatWebSocketHandler aiChatHandler;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(aiChatHandler, "/ws/ai/chat")
            .setAllowedOrigins("*") // 生产环境需要限制
            .withSockJS();
    }
}

// websocket/AIChatWebSocketHandler.java
@Component
@Slf4j
public class AIChatWebSocketHandler extends TextWebSocketHandler {
    
    private final ChatService chatService;
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = extractUserId(session);
        activeSessions.put(userId, session);
        log.info("WebSocket connection established for user: {}", userId);
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String userId = extractUserId(session);
        ChatRequest request = objectMapper.readValue(message.getPayload(), ChatRequest.class);
        
        log.info("Received chat message from user: {}", userId);
        
        // 流式处理AI响应
        chatService.chatStream(request, userId)
            .doOnNext(chunk -> {
                try {
                    String responseJson = objectMapper.writeValueAsString(chunk);
                    session.sendMessage(new TextMessage(responseJson));
                } catch (Exception e) {
                    log.error("Failed to send WebSocket message", e);
                }
            })
            .doOnError(error -> {
                try {
                    ErrorResponse errorResponse = ErrorResponse.builder()
                        .code("AI_ERROR")
                        .message("AI服务暂时不可用")
                        .build();
                    String errorJson = objectMapper.writeValueAsString(errorResponse);
                    session.sendMessage(new TextMessage(errorJson));
                } catch (Exception e) {
                    log.error("Failed to send error message", e);
                }
            })
            .subscribe();
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = extractUserId(session);
        activeSessions.remove(userId);
        log.info("WebSocket connection closed for user: {}", userId);
    }
    
    private String extractUserId(WebSocketSession session) {
        // 从session中提取用户ID (通过JWT token)
        String token = session.getUri().getQuery(); // 简化实现
        return jwtTokenProvider.getUserIdFromToken(token);
    }
}
```

### 2. 流式响应处理

```java
// ai-service/StreamingChatService.java
@Service
@Slf4j
public class StreamingChatService {
    
    private final ChatClient chatClient;
    private final RetrievalService retrievalService;
    private final ContextBuilderService contextBuilderService;
    
    public Flux<ChatResponseChunk> chatStream(ChatRequest request, String userId) {
        return Flux.create(sink -> {
            try {
                // 1. 构建搜索上下文
                SearchContext searchContext = contextBuilderService
                    .buildSearchContext(request.getMessage(), userId, request.getBookId());
                
                // 2. 检索相关文档
                List<Document> relevantDocs = retrievalService
                    .hybridSearch(request.getMessage(), searchContext);
                
                // 3. 构建对话上下文
                String conversationContext = contextBuilderService
                    .buildConversationContext(request.getConversationId(), 10);
                
                // 4. 创建提示词
                Prompt prompt = createStreamingPrompt(request, relevantDocs, conversationContext);
                
                // 5. 流式调用AI模型
                AtomicReference<String> fullResponse = new AtomicReference<>("");
                AtomicInteger chunkCount = new AtomicInteger(0);
                
                chatClient.stream(prompt)
                    .doOnNext(chatResponse -> {
                        String content = chatResponse.getResult().getOutput().getContent();
                        fullResponse.updateAndGet(current -> current + content);
                        
                        ChatResponseChunk chunk = ChatResponseChunk.builder()
                            .id(UUID.randomUUID().toString())
                            .content(content)
                            .type("content")
                            .chunkIndex(chunkCount.getAndIncrement())
                            .timestamp(Instant.now())
                            .build();
                        
                        sink.next(chunk);
                    })
                    .doOnComplete(() -> {
                        // 发送完成信号
                        ChatResponseChunk finalChunk = ChatResponseChunk.builder()
                            .id(UUID.randomUUID().toString())
                            .type("done")
                            .sources(extractSources(relevantDocs))
                            .tokensUsed(calculateTokensUsed(fullResponse.get()))
                            .timestamp(Instant.now())
                            .build();
                        
                        sink.next(finalChunk);
                        sink.complete();
                        
                        // 异步保存对话记录
                        saveConversationAsync(request, fullResponse.get(), userId);
                    })
                    .doOnError(error -> {
                        log.error("Streaming chat error: {}", error.getMessage(), error);
                        sink.error(new AIServiceException("对话服务出现错误", error));
                    })
                    .subscribe();
                    
            } catch (Exception e) {
                log.error("Failed to start chat stream: {}", e.getMessage(), e);
                sink.error(new AIServiceException("无法启动对话", e));
            }
        });
    }
    
    private Prompt createStreamingPrompt(ChatRequest request, 
                                       List<Document> relevantDocs, 
                                       String conversationContext) {
        
        Map<String, Object> variables = Map.of(
            "user_message", request.getMessage(),
            "relevant_docs", formatDocumentsForPrompt(relevantDocs),
            "conversation_history", conversationContext,
            "book_context", request.getBookId() != null ? 
                contextBuilderService.buildBookContext(request.getBookId()) : ""
        );
        
        String promptText = promptTemplateService.buildPrompt("chat", variables);
        
        return new Prompt(promptText, OpenAiChatOptions.builder()
            .withModel("gpt-4")
            .withTemperature(0.7f)
            .withMaxTokens(1024)
            .withStream(true)
            .build());
    }
}
```

## 知识库管理

### 1. 知识库更新服务

```java
// ai-service/KnowledgeBaseService.java
@Service
@Slf4j
public class KnowledgeBaseService {
    
    private final VectorStore vectorStore;
    private final DocumentIngestionService ingestionService;
    private final BookRepository bookRepository;
    
    @Scheduled(cron = "0 2 * * * *") // 每天凌晨2点执行
    public void updateKnowledgeBase() {
        log.info("Starting knowledge base update");
        
        try {
            // 1. 查找需要更新的书籍
            List<Book> booksToUpdate = findBooksNeedingUpdate();
            
            // 2. 批量处理
            List<CompletableFuture<Void>> futures = booksToUpdate.stream()
                .map(book -> ingestionService.ingestDocument(
                    book.getId().toString(), book.getFilePath()))
                .collect(Collectors.toList());
            
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
            
            // 3. 优化向量索引
            optimizeVectorIndex();
            
            log.info("Knowledge base update completed, processed {} books", booksToUpdate.size());
            
        } catch (Exception e) {
            log.error("Knowledge base update failed", e);
        }
    }
    
    public void rebuildKnowledgeBase() {
        log.info("Starting knowledge base rebuild");
        
        try {
            // 1. 清空现有向量数据
            vectorStore.delete(Collections.emptyList()); // 清空所有
            
            // 2. 重新处理所有书籍
            List<Book> allBooks = bookRepository.findAll();
            
            for (Book book : allBooks) {
                try {
                    ingestionService.ingestDocument(
                        book.getId().toString(), book.getFilePath()).get();
                } catch (Exception e) {
                    log.error("Failed to process book during rebuild: {}", book.getId(), e);
                }
            }
            
            log.info("Knowledge base rebuild completed");
            
        } catch (Exception e) {
            log.error("Knowledge base rebuild failed", e);
            throw new KnowledgeBaseException("Failed to rebuild knowledge base", e);
        }
    }
    
    private List<Book> findBooksNeedingUpdate() {
        // 查找最近添加或修改的书籍
        LocalDateTime since = LocalDateTime.now().minusDays(1);
        return bookRepository.findByUpdatedAtAfter(since);
    }
    
    private void optimizeVectorIndex() {
        // 向量索引优化逻辑
        log.info("Optimizing vector index");
        // 实现依赖于具体的向量数据库
    }
}
```

### 2. 向量数据同步

```java
// ai-service/VectorSyncService.java
@Service
@Slf4j
public class VectorSyncService {
    
    private final VectorStore primaryVectorStore;
    private final VectorStore backupVectorStore;
    private final DocumentRepository documentRepository;
    
    @Scheduled(fixedRate = 3600000) // 每小时同步一次
    public void syncVectorStores() {
        if (backupVectorStore == null) {
            return; // 没有备用存储
        }
        
        log.info("Starting vector store synchronization");
        
        try {
            // 1. 获取需要同步的文档
            List<Document> documentsToSync = getDocumentsToSync();
            
            if (documentsToSync.isEmpty()) {
                log.debug("No documents to sync");
                return;
            }
            
            // 2. 同步到备用存储
            backupVectorStore.add(documentsToSync);
            
            // 3. 更新同步状态
            updateSyncStatus(documentsToSync);
            
            log.info("Vector store synchronization completed, synced {} documents", 
                documentsToSync.size());
            
        } catch (Exception e) {
            log.error("Vector store synchronization failed", e);
        }
    }
    
    private List<Document> getDocumentsToSync() {
        // 获取最近添加但未同步的文档
        LocalDateTime since = getLastSyncTime();
        return documentRepository.findDocumentsAddedAfter(since);
    }
    
    public void validateVectorStoreConsistency() {
        log.info("Validating vector store consistency");
        
        try {
            // 随机抽样验证
            List<String> sampleQueries = Arrays.asList(
                "机器学习", "数据结构", "算法", "编程", "人工智能"
            );
            
            for (String query : sampleQueries) {
                SearchRequest request = SearchRequest.query(query).withTopK(5);
                
                List<Document> primaryResults = primaryVectorStore.similaritySearch(request);
                List<Document> backupResults = backupVectorStore != null ? 
                    backupVectorStore.similaritySearch(request) : Collections.emptyList();
                
                double consistency = calculateConsistency(primaryResults, backupResults);
                
                if (consistency < 0.8) {
                    log.warn("Low consistency detected for query '{}': {}", query, consistency);
                }
            }
            
            log.info("Vector store consistency validation completed");
            
        } catch (Exception e) {
            log.error("Vector store consistency validation failed", e);
        }
    }
}
```

## AI服务监控

### 1. AI服务健康监控

```java
// monitoring/AIServiceMonitor.java
@Component
@Slf4j
public class AIServiceMonitor {
    
    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final MeterRegistry meterRegistry;
    
    @Scheduled(fixedRate = 60000) // 每分钟检查一次
    public void monitorAIServices() {
        // 1. 监控LLM服务
        monitorLLMService();
        
        // 2. 监控向量数据库
        monitorVectorStore();
        
        // 3. 监控嵌入服务
        monitorEmbeddingService();
    }
    
    private void monitorLLMService() {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            ChatResponse response = chatClient.call(
                new Prompt("Hello, this is a health check.")
            );
            
            if (response != null && response.getResult() != null) {
                sample.stop(Timer.builder("ai.llm.health.check")
                    .tag("status", "success")
                    .register(meterRegistry));
                
                Gauge.builder("ai.llm.health.status")
                    .register(meterRegistry, 1, Number::doubleValue);
            }
            
        } catch (Exception e) {
            sample.stop(Timer.builder("ai.llm.health.check")
                .tag("status", "failure")
                .register(meterRegistry));
            
            Gauge.builder("ai.llm.health.status")
                .register(meterRegistry, 0, Number::doubleValue);
            
            log.error("LLM service health check failed", e);
        }
    }
    
    private void monitorVectorStore() {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            List<Document> results = vectorStore.similaritySearch(
                SearchRequest.query("test").withTopK(1)
            );
            
            sample.stop(Timer.builder("ai.vector.health.check")
                .tag("status", "success")
                .register(meterRegistry));
            
            Gauge.builder("ai.vector.health.status")
                .register(meterRegistry, 1, Number::doubleValue);
            
            Gauge.builder("ai.vector.documents.count")
                .register(meterRegistry, getVectorStoreDocumentCount(), Number::doubleValue);
            
        } catch (Exception e) {
            sample.stop(Timer.builder("ai.vector.health.check")
                .tag("status", "failure")
                .register(meterRegistry));
            
            Gauge.builder("ai.vector.health.status")
                .register(meterRegistry, 0, Number::doubleValue);
            
            log.error("Vector store health check failed", e);
        }
    }
}
```

### 2. AI使用统计

```java
// monitoring/AIUsageStatistics.java
@Service
@Slf4j
public class AIUsageStatistics {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final MeterRegistry meterRegistry;
    
    public void recordAIUsage(String userId, AIUsageType type, 
                             int tokensUsed, long responseTimeMs) {
        
        String dateKey = LocalDate.now().toString();
        String userDailyKey = String.format("ai_usage:daily:%s:%s", dateKey, userId);
        String globalDailyKey = String.format("ai_usage:global:%s", dateKey);
        
        // 记录用户每日使用量
        AIUsageData userData = AIUsageData.builder()
            .userId(userId)
            .type(type)
            .tokensUsed(tokensUsed)
            .responseTimeMs(responseTimeMs)
            .timestamp(Instant.now())
            .build();
        
        redisTemplate.opsForList().leftPush(userDailyKey, userData);
        redisTemplate.expire(userDailyKey, Duration.ofDays(30));
        
        // 记录全局统计
        redisTemplate.opsForHash().increment(globalDailyKey, "total_requests", 1);
        redisTemplate.opsForHash().increment(globalDailyKey, "total_tokens", tokensUsed);
        redisTemplate.expire(globalDailyKey, Duration.ofDays(90));
        
        // 记录Prometheus指标
        Counter.builder("ai.usage.requests.total")
            .tag("user_id", userId)
            .tag("type", type.name())
            .register(meterRegistry)
            .increment();
        
        Timer.builder("ai.usage.response.time")
            .tag("type", type.name())
            .register(meterRegistry)
            .record(responseTimeMs, TimeUnit.MILLISECONDS);
    }
    
    public AIUsageSummary getUserDailyUsage(String userId, LocalDate date) {
        String key = String.format("ai_usage:daily:%s:%s", date, userId);
        
        List<Object> rawData = redisTemplate.opsForList().range(key, 0, -1);
        
        if (rawData == null || rawData.isEmpty()) {
            return AIUsageSummary.empty(userId, date);
        }
        
        List<AIUsageData> usageData = rawData.stream()
            .map(obj -> (AIUsageData) obj)
            .collect(Collectors.toList());
        
        return AIUsageSummary.builder()
            .userId(userId)
            .date(date)
            .totalRequests(usageData.size())
            .totalTokens(usageData.stream().mapToInt(AIUsageData::getTokensUsed).sum())
            .averageResponseTime(usageData.stream()
                .mapToLong(AIUsageData::getResponseTimeMs)
                .average()
                .orElse(0.0))
            .usageByType(groupUsageByType(usageData))
            .build();
    }
    
    public enum AIUsageType {
        SEARCH, CHAT, RECOMMENDATION, SUMMARIZATION
    }
}
```

## 高级AI功能

### 1. 智能摘要服务

```java
// ai-service/SummarizationService.java
@Service
@Slf4j
public class SummarizationService {
    
    private final ChatClient chatClient;
    private final BookContentRepository contentRepository;
    
    public BookSummary generateBookSummary(UUID bookId, SummaryType type) {
        log.info("Generating {} summary for book: {}", type, bookId);
        
        try {
            // 1. 获取书籍内容
            List<BookContent> contents = contentRepository
                .findByBookIdOrderByPageNumber(bookId);
            
            // 2. 根据摘要类型选择内容
            String contentToSummarize = selectContentForSummary(contents, type);
            
            // 3. 生成摘要
            String prompt = buildSummaryPrompt(contentToSummarize, type);
            ChatResponse response = chatClient.call(new Prompt(prompt));
            
            // 4. 构建摘要对象
            return BookSummary.builder()
                .bookId(bookId)
                .type(type)
                .summary(response.getResult().getOutput().getContent())
                .generatedAt(Instant.now())
                .tokensUsed(response.getMetadata().getUsage().getTotalTokens())
                .build();
                
        } catch (Exception e) {
            log.error("Failed to generate summary for book: {}", bookId, e);
            throw new SummarizationException("Failed to generate book summary", e);
        }
    }
    
    private String selectContentForSummary(List<BookContent> contents, SummaryType type) {
        switch (type) {
            case BRIEF:
                // 选择前几页和目录
                return contents.stream()
                    .limit(5)
                    .map(BookContent::getContent)
                    .collect(Collectors.joining("\n"));
                    
            case DETAILED:
                // 选择关键章节
                return selectKeyChapters(contents);
                
            case CHAPTER:
                // 按章节摘要
                return contents.stream()
                    .map(BookContent::getContent)
                    .collect(Collectors.joining("\n"));
                    
            default:
                throw new IllegalArgumentException("Unknown summary type: " + type);
        }
    }
    
    private String buildSummaryPrompt(String content, SummaryType type) {
        switch (type) {
            case BRIEF:
                return String.format("""
                    请为以下书籍内容生成一个简洁的摘要（200字以内）：
                    
                    %s
                    
                    摘要要求：
                    1. 突出主要观点和核心内容
                    2. 使用简洁明了的语言
                    3. 适合快速阅读和理解
                    """, content);
                    
            case DETAILED:
                return String.format("""
                    请为以下书籍内容生成详细摘要（800字以内）：
                    
                    %s
                    
                    摘要要求：
                    1. 包含主要章节和重点内容
                    2. 保持逻辑结构清晰
                    3. 包含关键概念和实例
                    4. 适合深入理解书籍内容
                    """, content);
                    
            default:
                return "请为以下内容生成摘要：\n" + content;
        }
    }
    
    public enum SummaryType {
        BRIEF, DETAILED, CHAPTER
    }
}
```

### 2. 智能问答增强

```java
// ai-service/QuestionAnsweringService.java
@Service
@Slf4j
public class QuestionAnsweringService {
    
    private final RetrievalService retrievalService;
    private final ChatClient chatClient;
    private final QuestionClassifier questionClassifier;
    
    public QAResponse answerQuestion(String question, String userId, UUID bookId) {
        log.info("Processing question for user {}: {}", userId, question);
        
        try {
            // 1. 问题分类
            QuestionType questionType = questionClassifier.classify(question);
            
            // 2. 基于问题类型选择检索策略
            SearchContext searchContext = buildSearchContextForQuestion(
                question, questionType, userId, bookId);
            
            // 3. 检索相关文档
            List<Document> relevantDocs = retrievalService.hybridSearch(question, searchContext);
            
            // 4. 生成答案
            String answer = generateAnswer(question, questionType, relevantDocs);
            
            // 5. 提取引用来源
            List<SourceReference> sources = extractSourceReferences(relevantDocs);
            
            // 6. 计算置信度
            double confidence = calculateAnswerConfidence(question, relevantDocs, answer);
            
            return QAResponse.builder()
                .question(question)
                .answer(answer)
                .sources(sources)
                .confidence(confidence)
                .questionType(questionType)
                .responseTime(System.currentTimeMillis())
                .build();
                
        } catch (Exception e) {
            log.error("Failed to answer question: {}", question, e);
            throw new QuestionAnsweringException("Failed to process question", e);
        }
    }
    
    private String generateAnswer(String question, QuestionType type, List<Document> context) {
        String prompt = switch (type) {
            case FACTUAL -> buildFactualPrompt(question, context);
            case CONCEPTUAL -> buildConceptualPrompt(question, context);
            case COMPARATIVE -> buildComparativePrompt(question, context);
            case ANALYTICAL -> buildAnalyticalPrompt(question, context);
            default -> buildGenericPrompt(question, context);
        };
        
        ChatResponse response = chatClient.call(new Prompt(prompt, 
            OpenAiChatOptions.builder()
                .withModel(selectModelForQuestionType(type))
                .withTemperature(getTemperatureForQuestionType(type))
                .withMaxTokens(1024)
                .build()));
        
        return response.getResult().getOutput().getContent();
    }
    
    private String selectModelForQuestionType(QuestionType type) {
        return switch (type) {
            case ANALYTICAL, COMPARATIVE -> "gpt-4"; // 复杂分析使用更强模型
            default -> "gpt-3.5-turbo"; // 简单问题使用经济模型
        };
    }
    
    private float getTemperatureForQuestionType(QuestionType type) {
        return switch (type) {
            case FACTUAL -> 0.1f; // 事实性问题需要准确性
            case CREATIVE -> 0.8f; // 创意性问题需要多样性
            default -> 0.3f; // 平衡准确性和多样性
        };
    }
    
    public enum QuestionType {
        FACTUAL,      // 事实性问题
        CONCEPTUAL,   // 概念性问题  
        COMPARATIVE,  // 比较性问题
        ANALYTICAL,   // 分析性问题
        CREATIVE      // 创意性问题
    }
}
```

## 配置和部署

### 1. AI服务配置

```yaml
# application-ai.yml
app:
  ai:
    # OpenAI配置
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: ${OPENAI_BASE_URL:https://api.openai.com/v1}
      models:
        chat: gpt-4
        embedding: text-embedding-ada-002
        fallback-chat: gpt-3.5-turbo
      options:
        temperature: 0.7
        max-tokens: 2048
        timeout: 30s
    
    # 向量数据库配置
    vector-store:
      primary:
        type: chroma
        url: ${CHROMA_URL:http://localhost:8000}
        collection: kong-ai-books
        dimension: 1536
      backup:
        type: pinecone
        api-key: ${PINECONE_API_KEY}
        environment: ${PINECONE_ENVIRONMENT}
        index: kong-ai-books-backup
    
    # RAG配置
    rag:
      chunk-size: 500
      chunk-overlap: 100
      top-k: 10
      similarity-threshold: 0.7
      reranking:
        enabled: true
        model: cross-encoder/ms-marco-MiniLM-L-6-v2
    
    # 缓存配置
    cache:
      embedding-ttl: 24h
      search-result-ttl: 1h
      conversation-context-ttl: 30m
    
    # 限流配置
    rate-limit:
      search:
        requests-per-minute: 60
        burst-capacity: 10
      chat:
        requests-per-minute: 30
        burst-capacity: 5
```

### 2. 容器化部署

```dockerfile
# AI服务专用Dockerfile
FROM openjdk:17-jdk-slim

# 安装Python和AI依赖
RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    && rm -rf /var/lib/apt/lists/*

# 安装Python AI库
COPY requirements.txt /tmp/
RUN pip3 install -r /tmp/requirements.txt

WORKDIR /app

# 复制应用
COPY target/*.jar app.jar

# AI模型缓存目录
VOLUME ["/app/models"]

# 环境变量
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xmx2g -Xms1g"

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
```

```yaml
# docker-compose-ai.yml
version: '3.8'
services:
  kong-ai-books-backend:
    build: 
      context: ./backend
      dockerfile: Dockerfile.ai
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - CHROMA_URL=http://chroma:8000
      - DATABASE_URL=postgresql://postgres:5432/kong_ai_books
      - REDIS_URL=redis://redis:6379
    depends_on:
      - postgres
      - redis
      - chroma
    volumes:
      - ai_models:/app/models
    deploy:
      resources:
        limits:
          memory: 4G
          cpus: '2.0'
        reservations:
          memory: 2G
          cpus: '1.0'
  
  chroma:
    image: chromadb/chroma:latest
    ports:
      - "8000:8000"
    environment:
      - CHROMA_SERVER_HOST=0.0.0.0
      - CHROMA_SERVER_PORT=8000
    volumes:
      - chroma_data:/chroma/chroma
    deploy:
      resources:
        limits:
          memory: 2G
          cpus: '1.0'
  
  postgres:
    image: postgres:15
    environment:
      - POSTGRES_DB=kong_ai_books
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=${POSTGRES_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    deploy:
      resources:
        limits:
          memory: 1G
          cpus: '0.5'
  
  redis:
    image: redis:7-alpine
    volumes:
      - redis_data:/data
    deploy:
      resources:
        limits:
          memory: 512M
          cpus: '0.25'

volumes:
  ai_models:
  chroma_data:
  postgres_data:
  redis_data:
```

## 性能优化

### 1. 嵌入缓存策略

```java
// ai-service/EmbeddingCacheService.java
@Service
@Slf4j
public class EmbeddingCacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final EmbeddingClient embeddingClient;
    
    public List<Double> getOrGenerateEmbedding(String text) {
        String cacheKey = "embedding:" + DigestUtils.md5DigestAsHex(text.getBytes());
        
        // 尝试从缓存获取
        List<Double> cachedEmbedding = getCachedEmbedding(cacheKey);
        if (cachedEmbedding != null) {
            log.debug("Retrieved embedding from cache for text length: {}", text.length());
            return cachedEmbedding;
        }
        
        // 生成新的嵌入
        List<Double> embedding = embeddingClient.embed(text);
        
        // 缓存嵌入结果
        cacheEmbedding(cacheKey, embedding);
        
        log.debug("Generated and cached new embedding for text length: {}", text.length());
        return embedding;
    }
    
    @SuppressWarnings("unchecked")
    private List<Double> getCachedEmbedding(String key) {
        try {
            return (List<Double>) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Failed to retrieve cached embedding: {}", e.getMessage());
            return null;
        }
    }
    
    private void cacheEmbedding(String key, List<Double> embedding) {
        try {
            redisTemplate.opsForValue().set(key, embedding, Duration.ofHours(24));
        } catch (Exception e) {
            log.warn("Failed to cache embedding: {}", e.getMessage());
        }
    }
    
    @Scheduled(cron = "0 3 * * * *") // 每天凌晨3点清理
    public void cleanupExpiredEmbeddings() {
        log.info("Starting embedding cache cleanup");
        
        try {
            Set<String> keys = redisTemplate.keys("embedding:*");
            if (keys != null && !keys.isEmpty()) {
                // 批量检查过期时间并清理
                List<String> expiredKeys = keys.stream()
                    .filter(key -> {
                        Long ttl = redisTemplate.getExpire(key);
                        return ttl != null && ttl < 0;
                    })
                    .collect(Collectors.toList());
                
                if (!expiredKeys.isEmpty()) {
                    redisTemplate.delete(expiredKeys);
                    log.info("Cleaned up {} expired embedding cache entries", expiredKeys.size());
                }
            }
        } catch (Exception e) {
            log.error("Embedding cache cleanup failed", e);
        }
    }
}
```

### 2. 批量处理优化

```java
// ai-service/BatchProcessingService.java
@Service
@Slf4j
public class BatchProcessingService {
    
    private final EmbeddingClient embeddingClient;
    private final VectorStore vectorStore;
    
    @Value("${app.ai.batch.size:50}")
    private int batchSize;
    
    @Value("${app.ai.batch.max-concurrent:3}")
    private int maxConcurrentBatches;
    
    public CompletableFuture<Void> batchProcessDocuments(List<Document> documents) {
        log.info("Starting batch processing of {} documents", documents.size());
        
        // 分批处理
        List<List<Document>> batches = Lists.partition(documents, batchSize);
        
        // 控制并发数
        Semaphore semaphore = new Semaphore(maxConcurrentBatches);
        
        List<CompletableFuture<Void>> futures = batches.stream()
            .map(batch -> CompletableFuture.runAsync(() -> {
                try {
                    semaphore.acquire();
                    processBatch(batch);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                } finally {
                    semaphore.release();
                }
            }))
            .collect(Collectors.toList());
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenRun(() -> log.info("Batch processing completed for {} documents", documents.size()));
    }
    
    private void processBatch(List<Document> batch) {
        log.debug("Processing batch of {} documents", batch.size());
        
        try {
            // 批量生成嵌入
            List<String> texts = batch.stream()
                .map(Document::getContent)
                .collect(Collectors.toList());
            
            List<List<Double>> embeddings = embeddingClient.embed(texts);
            
            // 设置嵌入到文档
            for (int i = 0; i < batch.size(); i++) {
                batch.get(i).setEmbedding(embeddings.get(i));
            }
            
            // 批量存储到向量数据库
            vectorStore.add(batch);
            
            log.debug("Batch processing completed for {} documents", batch.size());
            
        } catch (Exception e) {
            log.error("Batch processing failed", e);
            throw new BatchProcessingException("Failed to process batch", e);
        }
    }
}
```

## 错误处理和降级策略

### 1. AI服务降级

```java
// ai-service/AIServiceFallback.java
@Service
@Slf4j
public class AIServiceFallback {
    
    private final ElasticsearchTemplate elasticsearchTemplate;
    private final BookRepository bookRepository;
    
    public AISearchResponse fallbackSearch(String query, String userId) {
        log.warn("Using fallback search for query: {}", query);
        
        try {
            // 使用传统搜索作为降级方案
            List<Book> books = bookRepository.searchBooks(query);
            
            List<SearchResult> results = books.stream()
                .limit(10)
                .map(this::convertToSearchResult)
                .collect(Collectors.toList());
            
            return AISearchResponse.builder()
                .answer("由于AI服务暂时不可用，为您提供传统搜索结果：")
                .results(results)
                .sources(Collections.emptyList())
                .confidence(0.5)
                .fallbackUsed(true)
                .build();
                
        } catch (Exception e) {
            log.error("Fallback search also failed", e);
            return createEmptyResponse();
        }
    }
    
    public Flux<ChatResponseChunk> fallbackChat(String message, String userId) {
        return Flux.just(
            ChatResponseChunk.builder()
                .content("抱歉，AI助手暂时不可用。您可以尝试使用搜索功能查找相关书籍内容。")
                .type("content")
                .timestamp(Instant.now())
                .build(),
            ChatResponseChunk.builder()
                .type("done")
                .timestamp(Instant.now())
                .build()
        );
    }
    
    private SearchResult convertToSearchResult(Book book) {
        return SearchResult.builder()
            .bookId(book.getId().toString())
            .title(book.getTitle())
            .author(book.getAuthor())
            .relevanceScore(0.5)
            .snippet(book.getDescription())
            .build();
    }
}
```

### 2. 断路器模式

```java
// ai-service/AIServiceCircuitBreaker.java
@Service
@Slf4j
public class AIServiceCircuitBreaker {
    
    private final CircuitBreaker chatCircuitBreaker;
    private final CircuitBreaker embeddingCircuitBreaker;
    private final AIServiceFallback fallbackService;
    
    public AIServiceCircuitBreaker(AIServiceFallback fallbackService) {
        this.fallbackService = fallbackService;
        
        // 配置聊天服务断路器
        this.chatCircuitBreaker = CircuitBreaker.ofDefaults("ai-chat");
        this.chatCircuitBreaker.getEventPublisher()
            .onStateTransition(event -> 
                log.info("Chat circuit breaker state transition: {} -> {}", 
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState()));
        
        // 配置嵌入服务断路器
        this.embeddingCircuitBreaker = CircuitBreaker.ofDefaults("ai-embedding");
    }
    
    public AISearchResponse searchWithCircuitBreaker(String query, String userId) {
        return chatCircuitBreaker.executeSupplier(() -> {
            // 正常AI搜索逻辑
            return aiSearchService.search(query, userId);
        }).recover(throwable -> {
            log.warn("AI search failed, using fallback: {}", throwable.getMessage());
            return fallbackService.fallbackSearch(query, userId);
        });
    }
    
    public Flux<ChatResponseChunk> chatWithCircuitBreaker(ChatRequest request, String userId) {
        return Flux.defer(() -> {
            if (chatCircuitBreaker.getState() == CircuitBreaker.State.OPEN) {
                log.warn("Chat circuit breaker is open, using fallback");
                return fallbackService.fallbackChat(request.getMessage(), userId);
            }
            
            return chatService.chatStream(request, userId)
                .doOnError(error -> chatCircuitBreaker.onError(
                    System.currentTimeMillis(), TimeUnit.MILLISECONDS, error))
                .doOnComplete(() -> chatCircuitBreaker.onSuccess(
                    System.currentTimeMillis(), TimeUnit.MILLISECONDS))
                .onErrorResume(error -> {
                    log.warn("Chat stream failed, using fallback: {}", error.getMessage());
                    return fallbackService.fallbackChat(request.getMessage(), userId);
                });
        });
    }
}
```

这个AI/RAG系统架构提供了:

1. **完整的RAG流水线**: 文档摄取、向量检索、生成增强
2. **多模型支持**: OpenAI、Anthropic等多种AI模型
3. **智能检索**: 混合搜索、重排序、上下文感知
4. **高性能**: 批量处理、缓存策略、异步处理
5. **高可用**: 断路器、降级策略、多存储备份
6. **可观测性**: 完整的监控、日志、性能追踪
7. **可扩展性**: 模块化设计、插件式架构

该架构能够支持大规模的AI应用场景，并具备良好的扩展性和可维护性。