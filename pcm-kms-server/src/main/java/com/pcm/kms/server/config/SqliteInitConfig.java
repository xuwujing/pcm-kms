package com.pcm.kms.server.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * SQLite 数据库自动初始化配置
 * <p>
 * SQLite 模式下 Flyway 不可用，因此通过启动时检测表是否存在，
 * 不存在则执行 sql/sqlite/V1__init.sql 初始化建表。
 */
@Slf4j
@Configuration
public class SqliteInitConfig {

    private final DataSource dataSource;

    @Value("${kms.datasource.mode:mysql}")
    private String datasourceMode;

    public SqliteInitConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void init() {
        if (!"sqlite".equalsIgnoreCase(datasourceMode)) {
            return;
        }

        try {
            // 检测表是否已存在
            if (isTableExists()) {
                log.info("SQLite 数据库表已存在，跳过初始化");
                return;
            }

            log.info("SQLite 数据库表不存在，开始执行初始化脚本...");
            executeInitSql();
            log.info("SQLite 数据库初始化完成");

        } catch (Exception e) {
            log.error("SQLite 数据库初始化失败", e);
            throw new RuntimeException("SQLite 初始化失败", e);
        }
    }

    private boolean isTableExists() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT count(*) FROM sqlite_master WHERE type='table' AND name='kms_client_app'")) {
            return rs.next() && rs.getInt(1) > 0;
        } catch (Exception e) {
            log.warn("检测 SQLite 表是否存在时异常: {}", e.getMessage());
            return false;
        }
    }

    private void executeInitSql() throws Exception {
        ClassPathResource resource = new ClassPathResource("sql/sqlite/V1__init.sql");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String sql = reader.lines().collect(Collectors.joining("\n"));

            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                // SQLite 不支持一次执行多条语句，按分号拆分
                String[] statements = sql.split(";");
                for (String s : statements) {
                    String trimmed = s.trim();
                    if (!trimmed.isEmpty()) {
                        stmt.execute(trimmed);
                    }
                }
            }
        }
    }
}
