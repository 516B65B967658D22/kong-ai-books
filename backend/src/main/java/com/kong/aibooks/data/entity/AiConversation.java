package com.kong.aibooks.data.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

/**
 * AI对话记录实体类
 */
@Entity
@Table(name = "ai_conversations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiConversation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    private Book book;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private ConversationType type = ConversationType.GENERAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ConversationStatus status = ConversationStatus.ACTIVE;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AiMessage> messages;

    @Column(name = "total_tokens_used")
    private Integer totalTokensUsed = 0;

    @Column(name = "message_count")
    private Integer messageCount = 0;

    public enum ConversationType {
        GENERAL, BOOK_SPECIFIC, SEARCH, RECOMMENDATION
    }

    public enum ConversationStatus {
        ACTIVE, ARCHIVED, DELETED
    }
}