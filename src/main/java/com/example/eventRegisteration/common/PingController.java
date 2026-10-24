package com.example.eventRegisteration.common;

import java.util.Map;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Smoke test for Phase 0. Delete once Phase 1 is in. */
@RestController
@RequestMapping("/api/ping")
public class  PingController {

    private final JdbcTemplate jdbc;
    private final StringRedisTemplate redis;

    public PingController(JdbcTemplate jdbc, StringRedisTemplate redis) {
        this.jdbc = jdbc;
        this.redis = redis;
    }

    @GetMapping
    public Map<String, Object> ping() {
        Integer tables = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()", Integer.class);
        redis.opsForValue().set("ping", "pong");
        return Map.of("mysqlTables", tables, "redis", redis.opsForValue().get("ping"));
    }
}
