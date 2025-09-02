package com.kong.aibooks.data.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

/**
 * 书籍实体类
 */
@Entity
@Table(name = "books")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book extends BaseEntity {

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "author", nullable = false, length = 200)
    private String author;

    @Column(name = "isbn", unique = true, length = 20)
    private String isbn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_url", length = 500)
    private String coverUrl;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "language", length = 10)
    private String language;

    @Column(name = "publisher", length = 200)
    private String publisher;

    @Column(name = "published_year")
    private Integer publishedYear;

    @Column(name = "rating")
    private Double rating;

    @Column(name = "download_count")
    private Long downloadCount = 0L;

    @Column(name = "view_count")
    private Long viewCount = 0L;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private BookStatus status = BookStatus.ACTIVE;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BookContent> contents;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReadingRecord> readingRecords;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Bookmark> bookmarks;

    public enum BookStatus {
        ACTIVE, INACTIVE, PROCESSING, ERROR
    }
}