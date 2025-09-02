package com.kong.aibooks.data.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * AI消息实体类
 */
@Entity
@Table(name = "ai_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private AiConversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private MessageRole role;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "tokens_used")
    private Integer tokensUsed;

    @Column(name = "response_time_ms")
    private Integer responseTimeMs;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "context_sources", columnDefinition = "TEXT")
    private String contextSources;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private MessageStatus status = MessageStatus.COMPLETED;

    public enum MessageRole {
        USER, ASSISTANT, SYSTEM
    }

    public enum MessageStatus {
        PENDING, PROCESSING, COMPLETED, ERROR
    }
}