package com.kong.aibooks.data.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * 书签实体类
 */
@Entity
@Table(name = "bookmarks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bookmark extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "page_number", nullable = false)
    private Integer pageNumber;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "text_selection", columnDefinition = "TEXT")
    private String textSelection;

    @Column(name = "position_info", columnDefinition = "TEXT")
    private String positionInfo;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private BookmarkType type = BookmarkType.BOOKMARK;

    public enum BookmarkType {
        BOOKMARK, HIGHLIGHT, NOTE
    }
}