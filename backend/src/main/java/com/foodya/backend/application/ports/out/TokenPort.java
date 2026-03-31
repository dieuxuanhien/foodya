package com.foodya.backend.application.ports.out;

import com.foodya.backend.application.dto.TokenClaims;
import com.foodya.backend.application.dto.UserAccountModel;

public interface TokenPort {

    String CLAIM_TOKEN_TYPE = "tokenType";
    String CLAIM_ROLE = "role";
    String TOKEN_TYPE_ACCESS = "ACCESS";
    String TOKEN_TYPE_REFRESH = "REFRESH";
    String TOKEN_TYPE_RESET = "RESET";

    String issueAccessToken(UserAccountModel user, String jti);

    String issueRefreshToken(UserAccountModel user, String jti, String family);

    String issueResetToken(UserAccountModel user, String jti, String challengeToken);

    TokenClaims parseClaims(String token);
}