package com.foodya.backend.infrastructure.security;

import com.foodya.backend.application.dto.TokenClaims;
import com.foodya.backend.application.ports.out.TokenPort;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class StompJwtChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenPort tokenPort;

    public StompJwtChannelInterceptor(TokenPort tokenPort) {
        this.tokenPort = tokenPort;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }

        String authorization = accessor.getFirstNativeHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new AccessDeniedException("missing websocket access token");
        }

        try {
            TokenClaims claims = tokenPort.parseClaims(authorization.substring(BEARER_PREFIX.length()));
            if (!TokenPort.TOKEN_TYPE_ACCESS.equals(claims.getString(TokenPort.CLAIM_TOKEN_TYPE))) {
                throw new AccessDeniedException("invalid websocket token type");
            }
            String role = claims.getString(TokenPort.CLAIM_ROLE);
            accessor.setUser(new StompUserPrincipal(UUID.fromString(claims.subject()), role));
            return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
        } catch (AccessDeniedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AccessDeniedException("invalid websocket access token", ex);
        }
    }
}
