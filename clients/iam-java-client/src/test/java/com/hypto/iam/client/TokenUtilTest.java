package com.hypto.iam.client;
import com.hypto.iam.client.helper.TokenUtil;
import com.hypto.iam.client.helpers.TokenHelper;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.Test;

import java.util.Date;

public class TokenUtilTest{

    @Test
    public void testGetUserHrn() {
        final String organizationId = "wkqmk8N7EM";
        final String userHrn = String.format("hrn:%s::iam-user/name1",organizationId);
        final String sampleEntitlements = String.format(
                "p, hrn:%1$s::iam-policy/ROOT_USER_POLICY, hrn:%1$s, hrn:%1$s:*, allow\n" +
                "p, hrn:%1$s::iam-policy/ROOT_USER_POLICY, hrn:%1$s::*, hrn:%1$s::*, allow\n\n" +
                "g, %2$s, hrn:%1$s::iam-policy/ROOT_USER_POLICY\n", organizationId, userHrn);

        final String tokenStr = TokenHelper.generateJwtToken(userHrn, organizationId, sampleEntitlements, new Date(System.currentTimeMillis() + (1000 * 60 * 60 * 24)), new Date(System.currentTimeMillis() + (1000 * 60 * 60 * 24)));

        TokenUtil token = new TokenUtil(tokenStr);
        assert token.principal.equals(userHrn);
    }

    @Test(expected = ExpiredJwtException.class)
    public void failIfTokenExpired() {
        final String organizationId = "wkqmk8N7EM";
        final String userHrn = String.format("hrn:%s::iam-user/name1",organizationId);
        final String sampleEntitlements = String.format(
                "p, hrn:%1$s::iam-policy/ROOT_USER_POLICY, hrn:%1$s, hrn:%1$s:*, allow\n" +
                "p, hrn:%1$s::iam-policy/ROOT_USER_POLICY, hrn:%1$s::*, hrn:%1$s::*, allow\n\n" +
                "g, %2$s, hrn:%1$s::iam-policy/ROOT_USER_POLICY\n", organizationId, userHrn);

        final String tokenStr = TokenHelper.generateJwtToken(userHrn, organizationId, sampleEntitlements, new Date(System.currentTimeMillis() - (1000 * 60 * 60 * 24)), new Date(System.currentTimeMillis() - 100));

        new TokenUtil(tokenStr);
    }

    @Test
    public void testPermissions(){
        final String organizationId = "wkqmk8N7EM";
        final String userHrn = String.format("hrn:%s::iam-user/name1",organizationId);
        final String sampleEntitlements = String.format(
                "p, hrn:%1$s::iam-policy/policy_view_invoice2, hrn:%1$s::invoice/1, hrn:%1$s::invoice$view, deny\n" +
                "p, hrn:%1$s::iam-policy/policy_view_invoice2, hrn:%1$s::invoice/*, hrn:%1$s::invoice$view, allow\n" +
                "g, %2$s, hrn:%1$s::iam-policy/policy_view_invoice2", organizationId, userHrn);

        final String tokenStr = TokenHelper.generateJwtToken(userHrn, organizationId, sampleEntitlements);

        TokenUtil token = new TokenUtil(tokenStr);
        assert !token.hasPermission("hrn:wkqmk8N7EM::invoice/1", "hrn:wkqmk8N7EM::invoice$view");
        assert token.hasPermission("hrn:wkqmk8N7EM::invoice/2", "hrn:wkqmk8N7EM::invoice$view");
    }

}