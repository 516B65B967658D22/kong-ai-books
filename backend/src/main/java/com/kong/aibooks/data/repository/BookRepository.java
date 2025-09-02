package com.kong.aibooks.data.repository;

import com.kong.aibooks.data.entity.Book;
import com.kong.aibooks.data.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 书籍数据访问接口
 */
@Repository
public interface BookRepository extends JpaRepository<Book, UUID> {

    /**
     * 根据标题搜索书籍
     */
    Page<Book> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    /**
     * 根据作者搜索书籍
     */
    Page<Book> findByAuthorContainingIgnoreCase(String author, Pageable pageable);

    /**
     * 根据分类查找书籍
     */
    Page<Book> findByCategory(Category category, Pageable pageable);

    /**
     * 根据分类ID查找书籍
     */
    Page<Book> findByCategoryId(UUID categoryId, Pageable pageable);

    /**
     * 根据ISBN查找书籍
     */
    Optional<Book> findByIsbn(String isbn);

    /**
     * 查找热门书籍 (按浏览量排序)
     */
    @Query("SELECT b FROM Book b WHERE b.status = 'ACTIVE' ORDER BY b.viewCount DESC")
    Page<Book> findPopularBooks(Pageable pageable);

    /**
     * 查找最新添加的书籍
     */
    @Query("SELECT b FROM Book b WHERE b.status = 'ACTIVE' ORDER BY b.createdAt DESC")
    Page<Book> findLatestBooks(Pageable pageable);

    /**
     * 复合搜索 - 标题、作者、描述
     */
    @Query("SELECT DISTINCT b FROM Book b WHERE b.status = 'ACTIVE' AND " +
           "(LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(b.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Book> searchBooks(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 根据分类和关键词搜索
     */
    @Query("SELECT DISTINCT b FROM Book b WHERE b.status = 'ACTIVE' AND " +
           "b.category.id = :categoryId AND " +
           "(LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Book> searchBooksByCategory(@Param("categoryId") UUID categoryId, 
                                   @Param("keyword") String keyword, 
                                   Pageable pageable);

    /**
     * 统计分类下的书籍数量
     */
    @Query("SELECT COUNT(b) FROM Book b WHERE b.category.id = :categoryId AND b.status = 'ACTIVE'")
    Long countBooksByCategory(@Param("categoryId") UUID categoryId);

    /**
     * 增加浏览量
     */
    @Query("UPDATE Book b SET b.viewCount = b.viewCount + 1 WHERE b.id = :bookId")
    void incrementViewCount(@Param("bookId") UUID bookId);

    /**
     * 查找相似书籍 (根据分类和作者)
     */
    @Query("SELECT b FROM Book b WHERE b.status = 'ACTIVE' AND " +
           "(b.category.id = :categoryId OR b.author = :author) AND " +
           "b.id != :excludeBookId ORDER BY b.rating DESC")
    List<Book> findSimilarBooks(@Param("categoryId") UUID categoryId, 
                               @Param("author") String author,
                               @Param("excludeBookId") UUID excludeBookId,
                               Pageable pageable);
}