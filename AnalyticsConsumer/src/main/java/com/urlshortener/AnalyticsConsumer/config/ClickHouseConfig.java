package com.urlshortener.AnalyticsConsumer.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

@Slf4j
@Configuration
public class ClickHouseConfig {

    @Value("${clickhouse.url}")
    private String url;

    @Value("${clickhouse.database}")
    private String database;

    @Value("${clickhouse.username}")
    private String username;

    @Value("${clickhouse.password}")
    private String password;

    private String jdbcUrl() {
        return "jdbc:ch:" + url + "/" + database + "?compress=0";
    }

    @Bean
    public Connection clickHouseConnection() throws Exception {
        log.info("Connecting to ClickHouse at {}", jdbcUrl());
        Connection connection = DriverManager.getConnection(jdbcUrl(), username, password);
        log.info("ClickHouse connected");
        ensureTableExists(connection);
        return connection;
    }

    private void ensureTableExists(Connection connection) throws Exception {
        String sql = """
                CREATE TABLE IF NOT EXISTS click_events (
                    short_code   String,
                    clicked_at   DateTime,
                    ip_address   String,
                    user_agent   String,
                    referer      String
                )
                ENGINE = MergeTree()
                ORDER BY (short_code, clicked_at)
                """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            log.info("ClickHouse table 'click_events' ready");
        }
    }
}