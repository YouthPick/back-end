package com.bop.youthpick.policy.controller;

import com.bop.youthpick.auth.dto.AuthPrincipal;
import com.bop.youthpick.auth.exception.AuthErrorCode;
import com.bop.youthpick.auth.exception.AuthException;
import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.global.error.ErrorCode;
import com.bop.youthpick.policy.dto.PolicyChatErrorResponse;
import com.bop.youthpick.policy.dto.PolicyChatMessageCreateRequest;
import com.bop.youthpick.policy.service.PolicyChatService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.method.MethodValidationException;

@Controller
@RequiredArgsConstructor
public class PolicyChatMessageController {

    private static final Pattern SEND_DESTINATION =
            Pattern.compile("^/app/policies/([1-9]\\d*)/chat/messages$");
    private static final PolicyChatErrorResponse INVALID_INPUT =
            new PolicyChatErrorResponse("C001", "입력 형태가 올바르지 않습니다.");
    private static final PolicyChatErrorResponse SERVER_ERROR =
            new PolicyChatErrorResponse("S001", "서버 내부 오류가 발생했습니다.");

    private final PolicyChatService policyChatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/policies/{policyId}/chat/messages")
    public void send(
            @DestinationVariable Long policyId,
            @Valid @Payload PolicyChatMessageCreateRequest request,
            Principal principal) {
        AuthPrincipal authPrincipal = requirePrincipal(principal);
        policyChatService.send(policyId, authPrincipal.userId(), request);
    }

    @MessageExceptionHandler(Exception.class)
    public void handleException(
            Exception exception,
            Principal principal,
            @Header("simpDestination") String destination) {
        Matcher matcher = SEND_DESTINATION.matcher(destination);
        if (!matcher.matches() || principal == null) {
            return;
        }
        messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/policies/" + matcher.group(1) + "/chat/errors",
                errorResponse(exception));
    }

    private AuthPrincipal requirePrincipal(Principal principal) {
        if (!(principal instanceof Authentication authentication)
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AuthPrincipal authPrincipal)) {
            throw new AuthException(AuthErrorCode.UNAUTHORIZED);
        }
        return authPrincipal;
    }

    private PolicyChatErrorResponse errorResponse(Throwable throwable) {
        Throwable cause = throwable;
        while (cause != null) {
            if (cause instanceof CustomException customException) {
                ErrorCode errorCode = customException.getErrorCode();
                return new PolicyChatErrorResponse(errorCode.getCode(), errorCode.getMessage());
            }
            if (cause instanceof MethodArgumentNotValidException
                    || cause instanceof MethodValidationException) {
                return INVALID_INPUT;
            }
            cause = cause.getCause();
        }
        return SERVER_ERROR;
    }
}
