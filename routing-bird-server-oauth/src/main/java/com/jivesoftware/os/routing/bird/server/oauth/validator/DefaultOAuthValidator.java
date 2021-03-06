/*
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-$year$ Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package com.jivesoftware.os.routing.bird.server.oauth.validator;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.server.oauth.AuthValidationException;
import com.jivesoftware.os.routing.bird.server.oauth.OAuthHttpRequestRejiggering;
import com.jivesoftware.os.routing.bird.server.oauth.OAuthSecretManager;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.glassfish.jersey.oauth1.signature.OAuth1Parameters;
import org.glassfish.jersey.oauth1.signature.OAuth1Request;
import org.glassfish.jersey.oauth1.signature.OAuth1Secrets;
import org.glassfish.jersey.oauth1.signature.OAuth1Signature;
import org.glassfish.jersey.oauth1.signature.OAuth1SignatureException;

/**
 * Note that to fully implement OAuth, we need to check for nonce-timestamp
 * uniqueness. We are deliberately deferring this to a future release.
 * <p>
 * <p>
 * When behind a loadbalancer you will likely need to add the following rules.
 * <p>
 * HTTP_HTTPS_X-Forward_set_header
 * <p>
 * This is needed in the LB for this to work! when HTTP_REQUEST { if
 * {([TCP::local_port] == 80) and !( [HTTP::header "X-Forwarded-Proto"] eq
 * "http") }{ HTTP::header insert X-Forwarded-Proto "http" HTTP::header insert
 * X-Forwarded-Port "80" } elseif {([TCP::local_port] == 443) and !(
 * [HTTP::header "X-Forwarded-Proto"] eq "https") } { HTTP::header insert
 * X-Forwarded-Proto "https" HTTP::header insert X-Forwarded-Port "443" } }
 */
public class DefaultOAuthValidator implements AuthValidator<OAuth1Signature, OAuth1Request> {

    public final static String LB_PROTO_HEADER = "x-forwarded-proto";
    public final static String LB_PORT_HEADER = "x-forwarded-port";
    private final static MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final ScheduledExecutorService newScheduledThreadPool;
    private final long validatorCheckForRemovedSecretsEveryNMillis;
    private final OAuthSecretManager secretManager;
    private final long timestampAgeLimitMillis;
    private final boolean doLoadBalancerRejiggering;
    private final boolean doLoadBalancerPortRejiggering;

    public DefaultOAuthValidator(ScheduledExecutorService newScheduledThreadPool,
        long validatorCheckForRemovedSecretsEveryNMillis,
        OAuthSecretManager secretManager,
        long timestampAgeLimitMillis,
        boolean doLoadBalancerRejiggering,
        boolean doLoadBalancerPortRejiggering) {

        this.newScheduledThreadPool = newScheduledThreadPool;
        this.validatorCheckForRemovedSecretsEveryNMillis = validatorCheckForRemovedSecretsEveryNMillis;
        this.secretManager = secretManager;
        this.timestampAgeLimitMillis = timestampAgeLimitMillis;
        this.doLoadBalancerRejiggering = doLoadBalancerRejiggering;
        this.doLoadBalancerPortRejiggering = doLoadBalancerPortRejiggering;
    }

    @Override
    synchronized public void start() {
        newScheduledThreadPool.scheduleWithFixedDelay(() -> {
            try {
                expireSecretCacheIfNecessary();
            } catch (Exception x) {
                // oh well we tried
            }
        },
            validatorCheckForRemovedSecretsEveryNMillis,
            validatorCheckForRemovedSecretsEveryNMillis,
            TimeUnit.MILLISECONDS);
    }

    @Override
    synchronized public void stop() {
        if (newScheduledThreadPool != null) {
            newScheduledThreadPool.shutdownNow();
        }
    }

    @Override
    public AuthValidationResult isValid(OAuth1Signature oAuth1Signature, OAuth1Request request) throws AuthValidationException {
        if (doLoadBalancerRejiggering) {
            LOG.trace("request will be rejiggered");
            LOG.inc("oauth>rejiggeredRequest");
            List<String> originalProtocol = request.getHeaderValues(LB_PROTO_HEADER);
            if (originalProtocol != null && !originalProtocol.isEmpty()) {
                try {
                    List<String> originalPort = doLoadBalancerPortRejiggering ? request.getHeaderValues(LB_PORT_HEADER) : null;
                    int lbPort = (originalPort == null || originalPort.isEmpty()) ? -1 : Integer.parseInt(originalPort.get(0));
                    request = new OAuthHttpRequestRejiggering(request, originalProtocol.get(0), lbPort);
                } catch (NumberFormatException e) {
                    throw new AuthValidationException("Bad loadbalancer forwarded port", e);
                }
            }
        }

        OAuth1Parameters params = new OAuth1Parameters();
        params.readRequest(request);

        String consumerKey = params.getConsumerKey();
        if (consumerKey == null) {
            LOG.warn("Missing consumerKey");
            return new AuthValidationResult(null, false);
        }

        // Check that the timestamp has not expired. Note: oauth timestamp is in seconds ...
        String timestampStr = params.getTimestamp();
        if (timestampStr == null) {
            LOG.warn("Missing timestamp for request by consumerKey:{}", consumerKey);
            return new AuthValidationResult(null, false);
        }

        long oauthTimeStamp = Long.parseLong(timestampStr) * 1000L;
        long now = System.currentTimeMillis();
        if (Math.abs(now - oauthTimeStamp) > timestampAgeLimitMillis) {
            LOG.warn("Timestamp out of range for request by consumerKey:{} timestamp:{}msec delta:{}msec", consumerKey, oauthTimeStamp, now - oauthTimeStamp);
            LOG.inc("oauth>error>outsideTimeRange");
            LOG.inc("oauth>consumerKey>" + consumerKey + ">error>outsideTimeRange");
            throw new AuthValidationException("The request timestamp is outside the allowable range. Please ensure you are running NTP.");
        }

        String secret = secretManager.getSecret(consumerKey);
        if (secret == null) {
            LOG.warn("Secret for consumerKey:{} is null", consumerKey);
            LOG.inc("oauth>secrets>missing");
            LOG.inc("oauth>consumerKey>" + consumerKey + ">secrets>missing");
            throw new AuthValidationException("Failed to locate secret for consumerKey:" + consumerKey);
        }

        OAuth1Secrets secrets = new OAuth1Secrets();
        secrets.setConsumerSecret(secret);
        secrets.setTokenSecret(secret);

        try {
            boolean verify = oAuth1Signature.verify(request, params, secrets);
            if (verify) {
                return new AuthValidationResult(consumerKey, true);
            } else {
                LOG.warn("OAuth signature verification failed for consumerKey:{}", consumerKey);
                LOG.inc("oauth>error>verificationFailed");
                LOG.inc("oauth>consumerKey>" + consumerKey + ">error>verificationFailed");
                return new AuthValidationResult(null, false);
            }
        } catch (OAuth1SignatureException e) {
            LOG.warn("OAuth signature verification failed for consumerKey:{} exception:{}", consumerKey, e.getClass().getSimpleName());
            LOG.inc("oauth>error>verificationError");
            LOG.inc("oauth>consumerKey>" + consumerKey + ">error>verificationError");
            throw new AuthValidationException("Oauth signature verification error.");
        }
    }

    public void expireSecretCacheIfNecessary() throws Exception {
        secretManager.verifyLastSecretRemovalTime();
    }

    @Override
    public void clearSecretCache() {
        secretManager.clearCache();
    }
}
