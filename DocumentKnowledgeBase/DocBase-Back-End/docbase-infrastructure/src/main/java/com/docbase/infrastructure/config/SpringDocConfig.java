package com.docbase.infrastructure.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI configuration.
 */
@Configuration
public class SpringDocConfig {

    @Bean
    public OpenAPI knowledgeBaseApi() {
        return new OpenAPI()
            .info(new Info()
                .title("DocBase 企业文档知识库系统 API")
                .description("企业文档知识库系统后端接口文档")
                .version("v1.0.0")
                .license(new License().name("MIT").url("https://docbase.local")))
            .externalDocs(new ExternalDocumentation()
                .description("DocBase 企业文档知识库系统说明")
                .url("https://docbase.local"));
    }
}
