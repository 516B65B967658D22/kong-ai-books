package com.kong.aibooks.data.repository;

import com.kong.aibooks.data.entity.AiConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * AI对话记录数据访问接口
 */
@Repository
public interface AiConversationRepository extends JpaRepository<AiConversation, UUID> {

    /**
     * 查找用户的对话记录
     */
    Page<AiConversation> findByUserIdAndStatusOrderByUpdatedAtDesc(UUID userId, 
                                                                  AiConversation.ConversationStatus status, 
                                                                  Pageable pageable);

    /**
     * 查找用户关于特定书籍的对话
     */
    @Query("SELECT ac FROM AiConversation ac WHERE ac.user.id = :userId AND " +
           "ac.book.id = :bookId AND ac.status = 'ACTIVE' ORDER BY ac.updatedAt DESC")
    List<AiConversation> findBookConversations(@Param("userId") UUID userId, 
                                             @Param("bookId") UUID bookId);

    /**
     * 查找用户最近的对话
     */
    @Query("SELECT ac FROM AiConversation ac WHERE ac.user.id = :userId AND " +
           "ac.status = 'ACTIVE' AND ac.updatedAt >= :since ORDER BY ac.updatedAt DESC")
    List<AiConversation> findRecentConversations(@Param("userId") UUID userId, 
                                                @Param("since") LocalDateTime since,
                                                Pageable pageable);

    /**
     * 统计用户对话数量
     */
    @Query("SELECT COUNT(ac) FROM AiConversation ac WHERE ac.user.id = :userId AND ac.status = 'ACTIVE'")
    Long countUserConversations(@Param("userId") UUID userId);

    /**
     * 查找活跃对话 (有消息交互的)
     */
    @Query("SELECT ac FROM AiConversation ac WHERE ac.messageCount > 0 AND " +
           "ac.status = 'ACTIVE' ORDER BY ac.updatedAt DESC")
    Page<AiConversation> findActiveConversations(Pageable pageable);

    /**
     * 更新对话统计信息
     */
    @Query("UPDATE AiConversation ac SET ac.messageCount = ac.messageCount + 1, " +
           "ac.totalTokensUsed = ac.totalTokensUsed + :tokens WHERE ac.id = :conversationId")
    void updateConversationStats(@Param("conversationId") UUID conversationId, 
                                @Param("tokens") Integer tokens);
}