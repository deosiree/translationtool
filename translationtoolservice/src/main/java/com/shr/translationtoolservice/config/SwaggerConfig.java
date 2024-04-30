package com.shr.translationtoolservice.config;

import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.oas.annotations.EnableOpenApi;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.Arrays;
import java.util.List;

import static java.util.Collections.singletonList;

/**
 * @Description Swagger配置类
 * @Author Corey
 * @Date 2022/8/5 18:35
 */
@Slf4j
@Configuration
@EnableOpenApi
public class SwaggerConfig {

    //是否开启swagger，正式环境一般是需要关闭的，可根据springboot的多环境配置进行设置
    @Value(value = "${swagger.enabled}")
    Boolean swaggerEnabled;

    private List<SecurityReference> defaultAuth() {
        AuthorizationScope authorizationScope
                = new AuthorizationScope("global", "accessEverything");
        AuthorizationScope[] authorizationScopes = new AuthorizationScope[1];
        authorizationScopes[0] = authorizationScope;
        return singletonList(
                new SecurityReference("token", authorizationScopes));
    }

    private SecurityContext securityContext() {
        return SecurityContext.builder()
                .securityReferences(defaultAuth())
                .build();
    }

    @Bean
    @SuppressWarnings("all")
    public Docket docket() {
        log.info("swagger.enabled: " + swaggerEnabled);
        return new Docket(DocumentationType.OAS_30)
                .apiInfo(apiInfo())
                .enable(swaggerEnabled)
                .groupName("Corey")
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.shr.translationtoolservice.controller")
                )
                .paths(PathSelectors.any())
                .build()
                .securityContexts(Arrays.asList(securityContext()))
                .securitySchemes(Arrays.asList(new ApiKey("token", "token", SecurityScheme.In.HEADER.name())));
    }

    @SuppressWarnings("all")
    public ApiInfo apiInfo() {
        return new ApiInfo(
                "词条翻译工具 api",
                "词条翻译工具 api",
                "V0.1",
                "/team",
                "Corey",
                "Apache 2.0",
                "null"
        );
    }
}
