package com.play.stream.Starjams.UserService.config;

import com.clickhouse.jdbc.ClickHouseDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Properties;

@Configuration
public class ClickHouseConfig {

    @Value("${clickhouse.url:jdbc:clickhouse://localhost:8123/starjamz}")
    private String url;

    @Bean
    public DataSource clickHouseDataSource() throws SQLException {
        return new ClickHouseDataSource(url, new Properties());
    }
}
