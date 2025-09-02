package com.kong.aibooks.data.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * 书籍内容实体类 (分页存储)
 */
@Entity
@Table(name = "book_contents", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"book_id", "page_number"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookContent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "page_number", nullable = false)
    private Integer pageNumber;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "word_count")
    private Integer wordCount;

    @Column(name = "chapter_title", length = 500)
    private String chapterTitle;

    @Column(name = "section_title", length = 500)
    private String sectionTitle;

    @Column(name = "is_processed")
    private Boolean isProcessed = false;

    @Column(name = "vector_id", length = 100)
    private String vectorId;
}