package com.hypto.iam.client.helper;

import com.hypto.iam.client.model.TokenResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import org.casbin.jcasbin.main.Enforcer;
import org.casbin.jcasbin.persist.file_adapter.FileAdapter;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class TokenUtil {
    static final String USER_CLAIM = "usr";
    static final String ENTITLEMENTS_CLAIM = "entitlements";
    static final String modelPath = Objects.requireNonNull(TokenUtil.class.getClassLoader()
            .getResource("casbin_model.conf")).getFile();

    public Jwt<Header, Claims> jwt;
    public Enforcer enforcer;
    public String principal;

    public TokenUtil(String token){
        String[] splitToken = token.split("\\.");
        String unsignedToken = splitToken[0] + "." + splitToken[1] + ".";
        this.jwt = Jwts.parserBuilder().build().parseClaimsJwt(unsignedToken);

        this.principal = jwt.getBody().get(USER_CLAIM, String.class);
        String entitlements = jwt.getBody().get(ENTITLEMENTS_CLAIM, String.class);

        this.enforcer = new Enforcer(modelPath, new FileAdapter(
                new ByteArrayInputStream(entitlements.getBytes(StandardCharsets.UTF_8))));
    }

    public TokenUtil(TokenResponse tokenResponse) {
        this(tokenResponse.getToken());
    }

    public boolean hasPermission(String resourceHrn, String actionHrn) {
        return enforcer.enforce(this.principal, resourceHrn, actionHrn);
    }
}
