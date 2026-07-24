package com.bop.youthpick.global.config;

import com.bop.youthpick.global.error.ErrorResponse;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 성공 응답의 실제 타입은 컨트롤러 반환 타입에서 springdoc이 자동으로 뽑아내지만(예: {@code ApiResponseListPolicyCardResponse}),
 * 에러 응답({@code global.error.ErrorResponse})은 예외 핸들러가 던지는 것이라 컨트롤러 시그니처에 드러나지 않는다. 그래서 스키마 등록과
 * 4XX/5XX 기본 응답 노출을 여기서 전역으로 채워준다.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH_SCHEME = "bearerAuth";
    private static final String ERROR_SCHEMA_NAME = "ErrorResponse";

    @Bean
    OpenAPI openAPI() {
        Components components =
                new Components().addSecuritySchemes(BEARER_AUTH_SCHEME, bearerAuthScheme());
        registerErrorSchema(components);

        return new OpenAPI()
                .info(apiInfo())
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH_SCHEME))
                .components(components);
    }

    @Bean
    OperationCustomizer errorResponseCustomizer() {
        Content errorContent =
                new Content()
                        .addMediaType(
                                "application/json",
                                new MediaType()
                                        .schema(
                                                new Schema<>()
                                                        .$ref(
                                                                "#/components/schemas/"
                                                                        + ERROR_SCHEMA_NAME)));

        return (operation, handlerMethod) -> {
            ApiResponses responses = operation.getResponses();
            responses.addApiResponse(
                    "4XX",
                    errorApiResponse(
                            errorContent,
                            "클라이언트 오류 — 공통 에러 응답 형식(ErrorResponse)으로 내려간다. code 필드로 원인을 구분한다(예: C001"
                                    + " 입력값 오류, A001 인증 실패)."));
            responses.addApiResponse(
                    "5XX",
                    errorApiResponse(
                            errorContent, "서버 내부 오류 — 공통 에러 응답 형식(ErrorResponse)으로 내려간다(S001)."));
            return operation;
        };
    }

    private ApiResponse errorApiResponse(Content content, String description) {
        return new ApiResponse().description(description).content(content);
    }

    private void registerErrorSchema(Components components) {
        ResolvedSchema resolvedSchema =
                ModelConverters.getInstance()
                        .resolveAsResolvedSchema(new AnnotatedType(ErrorResponse.class));
        components.addSchemas(ERROR_SCHEMA_NAME, resolvedSchema.schema);
        resolvedSchema.referencedSchemas.forEach(components::addSchemas);
    }

    private Info apiInfo() {
        return new Info()
                .title("YouthPick API")
                .description("청년 정책 추천 서비스 YouthPick 백엔드 API 문서")
                .version("v0.0.1");
    }

    private SecurityScheme bearerAuthScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");
    }
}
