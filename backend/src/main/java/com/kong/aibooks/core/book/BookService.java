package com.kong.aibooks.core.book;

import com.kong.aibooks.core.book.dto.*;
import com.kong.aibooks.data.entity.Book;
import com.kong.aibooks.data.repository.BookRepository;
import com.kong.aibooks.data.repository.BookContentRepository;
import com.kong.aibooks.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 书籍管理服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;
    private final BookContentRepository bookContentRepository;
    private final BookMapper bookMapper;

    /**
     * 获取书籍列表
     */
    public PageResult<BookDTO> getBooks(BookQueryRequest request, Pageable pageable) {
        log.info("Fetching books with query: {}", request);
        
        Page<Book> books;
        
        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            if (request.getCategoryId() != null) {
                books = bookRepository.searchBooksByCategory(
                    request.getCategoryId(), 
                    request.getKeyword().trim(), 
                    pageable
                );
            } else {
                books = bookRepository.searchBooks(request.getKeyword().trim(), pageable);
            }
        } else if (request.getCategoryId() != null) {
            books = bookRepository.findByCategoryId(request.getCategoryId(), pageable);
        } else if (request.isPopular()) {
            books = bookRepository.findPopularBooks(pageable);
        } else if (request.isLatest()) {
            books = bookRepository.findLatestBooks(pageable);
        } else {
            books = bookRepository.findAll(pageable);
        }
        
        List<BookDTO> bookDTOs = books.getContent().stream()
            .map(bookMapper::toDTO)
            .toList();
        
        return PageResult.<BookDTO>builder()
            .content(bookDTOs)
            .page(books.getNumber())
            .size(books.getSize())
            .totalElements(books.getTotalElements())
            .totalPages(books.getTotalPages())
            .first(books.isFirst())
            .last(books.isLast())
            .build();
    }

    /**
     * 获取书籍详情
     */
    public BookDetailDTO getBookDetail(String bookId) {
        log.info("Fetching book detail for ID: {}", bookId);
        
        Book book = bookRepository.findById(UUID.fromString(bookId))
            .orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + bookId));
        
        // 增加浏览量
        incrementViewCount(book.getId());
        
        return bookMapper.toDetailDTO(book);
    }

    /**
     * 获取书籍内容
     */
    public BookContentDTO getBookContent(String bookId, int pageNumber) {
        log.info("Fetching book content for ID: {}, page: {}", bookId, pageNumber);
        
        UUID bookUuid = UUID.fromString(bookId);
        
        // 验证书籍存在
        if (!bookRepository.existsById(bookUuid)) {
            throw new ResourceNotFoundException("Book not found with ID: " + bookId);
        }
        
        var bookContent = bookContentRepository.findByBookIdAndPageNumber(bookUuid, pageNumber)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format("Content not found for book %s, page %d", bookId, pageNumber)));
        
        return bookMapper.toContentDTO(bookContent);
    }

    /**
     * 获取书籍章节目录
     */
    public List<ChapterDTO> getBookChapters(String bookId) {
        log.info("Fetching chapters for book ID: {}", bookId);
        
        UUID bookUuid = UUID.fromString(bookId);
        List<Object[]> chapterData = bookContentRepository.findChapterTitles(bookUuid);
        
        return chapterData.stream()
            .map(data -> ChapterDTO.builder()
                .title((String) data[0])
                .startPage((Integer) data[1])
                .build())
            .toList();
    }

    /**
     * 搜索书籍内容
     */
    public List<BookContentSearchResult> searchBookContent(String bookId, String searchText) {
        log.info("Searching content in book {}: {}", bookId, searchText);
        
        UUID bookUuid = UUID.fromString(bookId);
        var contents = bookContentRepository.searchInBookContent(bookUuid, searchText);
        
        return contents.stream()
            .map(content -> BookContentSearchResult.builder()
                .pageNumber(content.getPageNumber())
                .content(content.getContent())
                .chapterTitle(content.getChapterTitle())
                .build())
            .toList();
    }

    /**
     * 获取相似书籍推荐
     */
    public List<BookDTO> getSimilarBooks(String bookId, int limit) {
        log.info("Finding similar books for ID: {}", bookId);
        
        Book book = bookRepository.findById(UUID.fromString(bookId))
            .orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + bookId));
        
        UUID categoryId = book.getCategory() != null ? book.getCategory().getId() : null;
        
        List<Book> similarBooks = bookRepository.findSimilarBooks(
            categoryId, 
            book.getAuthor(), 
            book.getId(),
            Pageable.ofSize(limit)
        );
        
        return similarBooks.stream()
            .map(bookMapper::toDTO)
            .toList();
    }

    /**
     * 增加书籍浏览量
     */
    @Transactional
    public void incrementViewCount(UUID bookId) {
        bookRepository.incrementViewCount(bookId);
    }

    /**
     * 创建新书籍
     */
    @Transactional
    public BookDTO createBook(CreateBookRequest request) {
        log.info("Creating new book: {}", request.getTitle());
        
        Book book = Book.builder()
            .title(request.getTitle())
            .author(request.getAuthor())
            .isbn(request.getIsbn())
            .description(request.getDescription())
            .coverUrl(request.getCoverUrl())
            .filePath(request.getFilePath())
            .language(request.getLanguage())
            .publisher(request.getPublisher())
            .publishedYear(request.getPublishedYear())
            .status(Book.BookStatus.PROCESSING)
            .build();
        
        book = bookRepository.save(book);
        log.info("Book created with ID: {}", book.getId());
        
        return bookMapper.toDTO(book);
    }

    /**
     * 更新书籍信息
     */
    @Transactional
    public BookDTO updateBook(String bookId, UpdateBookRequest request) {
        log.info("Updating book ID: {}", bookId);
        
        Book book = bookRepository.findById(UUID.fromString(bookId))
            .orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + bookId));
        
        // 更新字段
        if (request.getTitle() != null) {
            book.setTitle(request.getTitle());
        }
        if (request.getAuthor() != null) {
            book.setAuthor(request.getAuthor());
        }
        if (request.getDescription() != null) {
            book.setDescription(request.getDescription());
        }
        if (request.getCoverUrl() != null) {
            book.setCoverUrl(request.getCoverUrl());
        }
        
        book = bookRepository.save(book);
        return bookMapper.toDTO(book);
    }

    /**
     * 删除书籍
     */
    @Transactional
    public void deleteBook(String bookId) {
        log.info("Deleting book ID: {}", bookId);
        
        UUID bookUuid = UUID.fromString(bookId);
        if (!bookRepository.existsById(bookUuid)) {
            throw new ResourceNotFoundException("Book not found with ID: " + bookId);
        }
        
        bookRepository.deleteById(bookUuid);
        log.info("Book deleted: {}", bookId);
    }
}