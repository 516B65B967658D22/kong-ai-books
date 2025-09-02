package com.kong.aibooks.data.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 阅读记录实体类
 */
@Entity
@Table(name = "reading_records",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "book_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReadingRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "current_page", nullable = false)
    private Integer currentPage = 1;

    @Column(name = "progress_percentage", precision = 5, scale = 2)
    private BigDecimal progressPercentage = BigDecimal.ZERO;

    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;

    @Column(name = "reading_time_minutes")
    private Integer readingTimeMinutes = 0;

    @Column(name = "total_pages_read")
    private Integer totalPagesRead = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ReadingStatus status = ReadingStatus.READING;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public enum ReadingStatus {
        NOT_STARTED, READING, COMPLETED, PAUSED
    }
}