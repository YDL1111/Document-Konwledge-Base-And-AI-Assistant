package com.docbase.infrastructure.client.python;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "docbase.ai.python")
public class PythonAiProperties {

    private String baseUrl = "http://localhost:8000";
    private String apiKey;
    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 30000;
}
