package com.zzz.zerocode.ai.config;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "app.ai.redis")
public class RedisMemoryProperties {
    @NotBlank
    private String host = "127.0.0.1";
    @Min(1) @Max(65535)
    private int port = 6379;
    private String user;
    @ToString.Exclude
    private String password;
    @NotBlank
    private String keyPrefix = "zerocode:agent:memory:";
    @Min(1)
    private long ttlSeconds = 604800;
    private boolean ssl = false;
}
