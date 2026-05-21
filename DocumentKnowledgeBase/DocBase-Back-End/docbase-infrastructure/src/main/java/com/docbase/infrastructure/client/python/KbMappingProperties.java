package com.docbase.infrastructure.client.python;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "docbase.ai.kb-mapping")
public class KbMappingProperties {

    private Integer defaultKbId = 5;
    private Map<Long, Integer> categoryMappings = new HashMap<>();
}
