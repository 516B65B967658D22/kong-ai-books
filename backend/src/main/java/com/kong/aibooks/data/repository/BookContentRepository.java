package com.kong.aibooks.data.repository;

import com.kong.aibooks.data.entity.BookContent;
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
 * 书籍内容数据访问接口
 */
@Repository
public interface BookContentRepository extends JpaRepository<BookContent, UUID> {

    /**
     * 根据书籍ID和页码查找内容
     */
    Optional<BookContent> findByBookIdAndPageNumber(UUID bookId, Integer pageNumber);

    /**
     * 根据书籍ID查找所有内容页
     */
    List<BookContent> findByBookIdOrderByPageNumber(UUID bookId);

    /**
     * 分页查询书籍内容
     */
    Page<BookContent> findByBookIdOrderByPageNumber(UUID bookId, Pageable pageable);

    /**
     * 查找书籍的页数范围
     */
    @Query("SELECT MIN(bc.pageNumber), MAX(bc.pageNumber) FROM BookContent bc WHERE bc.book.id = :bookId")
    Object[] findPageRange(@Param("bookId") UUID bookId);

    /**
     * 统计书籍总页数
     */
    @Query("SELECT COUNT(bc) FROM BookContent bc WHERE bc.book.id = :bookId")
    Long countPagesByBookId(@Param("bookId") UUID bookId);

    /**
     * 查找未处理的内容 (用于AI向量化)
     */
    @Query("SELECT bc FROM BookContent bc WHERE bc.isProcessed = false ORDER BY bc.book.id, bc.pageNumber")
    List<BookContent> findUnprocessedContent(Pageable pageable);

    /**
     * 标记内容为已处理
     */
    @Query("UPDATE BookContent bc SET bc.isProcessed = true, bc.vectorId = :vectorId WHERE bc.id = :contentId")
    void markAsProcessed(@Param("contentId") UUID contentId, @Param("vectorId") String vectorId);

    /**
     * 在书籍内容中搜索文本
     */
    @Query("SELECT bc FROM BookContent bc WHERE bc.book.id = :bookId AND " +
           "LOWER(bc.content) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
           "ORDER BY bc.pageNumber")
    List<BookContent> searchInBookContent(@Param("bookId") UUID bookId, 
                                        @Param("searchText") String searchText);

    /**
     * 获取章节标题列表
     */
    @Query("SELECT DISTINCT bc.chapterTitle, MIN(bc.pageNumber) FROM BookContent bc " +
           "WHERE bc.book.id = :bookId AND bc.chapterTitle IS NOT NULL " +
           "GROUP BY bc.chapterTitle ORDER BY MIN(bc.pageNumber)")
    List<Object[]> findChapterTitles(@Param("bookId") UUID bookId);
}