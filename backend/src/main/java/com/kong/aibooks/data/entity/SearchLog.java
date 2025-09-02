package com.kong.aibooks.data.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * 搜索日志实体类
 */
@Entity
@Table(name = "search_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "query", nullable = false, columnDefinition = "TEXT")
    private String query;

    @Enumerated(EnumType.STRING)
    @Column(name = "search_type", length = 20)
    private SearchType searchType;

    @Column(name = "results_count")
    private Integer resultsCount;

    @Column(name = "response_time_ms")
    private Integer responseTimeMs;

    @Column(name = "user_ip", length = 45)
    private String userIp;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "filters_applied", columnDefinition = "TEXT")
    private String filtersApplied;

    public enum SearchType {
        TRADITIONAL, AI_SEMANTIC, HYBRID
    }
}