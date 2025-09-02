package com.kong.aibooks.data.repository;

import com.kong.aibooks.data.entity.ReadingRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 阅读记录数据访问接口
 */
@Repository
public interface ReadingRecordRepository extends JpaRepository<ReadingRecord, UUID> {

    /**
     * 根据用户ID和书籍ID查找阅读记录
     */
    Optional<ReadingRecord> findByUserIdAndBookId(UUID userId, UUID bookId);

    /**
     * 查找用户的所有阅读记录
     */
    Page<ReadingRecord> findByUserIdOrderByLastReadAtDesc(UUID userId, Pageable pageable);

    /**
     * 查找用户正在阅读的书籍
     */
    @Query("SELECT rr FROM ReadingRecord rr WHERE rr.user.id = :userId AND " +
           "rr.status = 'READING' ORDER BY rr.lastReadAt DESC")
    List<ReadingRecord> findCurrentlyReading(@Param("userId") UUID userId);

    /**
     * 查找用户已完成的书籍
     */
    @Query("SELECT rr FROM ReadingRecord rr WHERE rr.user.id = :userId AND " +
           "rr.status = 'COMPLETED' ORDER BY rr.updatedAt DESC")
    Page<ReadingRecord> findCompletedBooks(@Param("userId") UUID userId, Pageable pageable);

    /**
     * 统计用户阅读统计
     */
    @Query("SELECT COUNT(rr), SUM(rr.readingTimeMinutes), AVG(rr.progressPercentage) " +
           "FROM ReadingRecord rr WHERE rr.user.id = :userId")
    Object[] getUserReadingStats(@Param("userId") UUID userId);

    /**
     * 查找最近阅读的书籍
     */
    @Query("SELECT rr FROM ReadingRecord rr WHERE rr.user.id = :userId AND " +
           "rr.lastReadAt >= :since ORDER BY rr.lastReadAt DESC")
    List<ReadingRecord> findRecentlyRead(@Param("userId") UUID userId, 
                                       @Param("since") LocalDateTime since,
                                       Pageable pageable);

    /**
     * 查找热门书籍 (基于阅读记录)
     */
    @Query("SELECT rr.book, COUNT(rr) as readCount FROM ReadingRecord rr " +
           "WHERE rr.lastReadAt >= :since " +
           "GROUP BY rr.book ORDER BY readCount DESC")
    List<Object[]> findPopularBooks(@Param("since") LocalDateTime since, Pageable pageable);

    /**
     * 更新阅读进度
     */
    @Query("UPDATE ReadingRecord rr SET rr.currentPage = :currentPage, " +
           "rr.progressPercentage = :progressPercentage, rr.lastReadAt = :lastReadAt " +
           "WHERE rr.user.id = :userId AND rr.book.id = :bookId")
    void updateProgress(@Param("userId") UUID userId, 
                       @Param("bookId") UUID bookId,
                       @Param("currentPage") Integer currentPage,
                       @Param("progressPercentage") java.math.BigDecimal progressPercentage,
                       @Param("lastReadAt") LocalDateTime lastReadAt);
}