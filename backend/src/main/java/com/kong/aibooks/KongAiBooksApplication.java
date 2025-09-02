package com.kong.aibooks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Kong AI Books 主应用程序
 * 
 * 功能特性:
 * - 在线书籍管理和阅读
 * - AI智能搜索和问答
 * - RAG (检索增强生成) 技术支持
 * - 用户个性化服务
 * 
 * @author Kong AI Books Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableTransactionManagement
public class KongAiBooksApplication {

    public static void main(String[] args) {
        SpringApplication.run(KongAiBooksApplication.class, args);
    }
}