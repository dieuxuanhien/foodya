package com.foodya.backend.infrastructure.security;

import com.foodya.backend.application.dto.TokenClaims;
import com.foodya.backend.application.ports.out.TokenPort;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;

import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StompJwtChannelInterceptorTests {

    @Test
    void connectWithValidAccessTokenSetsStompPrincipal() {
        UUID userId = UUID.randomUUID();
        TokenPort tokenPort = mock(TokenPort.class);
        when(tokenPort.parseClaims("good-token")).thenReturn(new TokenClaims(
                userId.toString(),
                "jti",
                OffsetDateTime.now().plusMinutes(5),
                Map.of(
                        TokenPort.CLAIM_TOKEN_TYPE, TokenPort.TOKEN_TYPE_ACCESS,
                        TokenPort.CLAIM_ROLE, "CUSTOMER"
                )
        ));
        StompJwtChannelInterceptor interceptor = new StompJwtChannelInterceptor(tokenPort);

        Message<?> result = interceptor.preSend(connectMessage("Bearer good-token"), mock(MessageChannel.class));

        Principal principal = assertInstanceOf(
                Principal.class,
                result.getHeaders().get(SimpMessageHeaderAccessor.USER_HEADER)
        );
        assertEquals(userId.toString(), principal.getName());
    }

    @Test
    void connectWithoutBearerTokenIsRejected() {
        StompJwtChannelInterceptor interceptor = new StompJwtChannelInterceptor(mock(TokenPort.class));

        assertThrows(
                AccessDeniedException.class,
                () -> interceptor.preSend(connectMessage(null), mock(MessageChannel.class))
        );
    }

    private Message<byte[]> connectMessage(String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authorization != null) {
            accessor.setNativeHeader("Authorization", authorization);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
