package net.fosterlink.fosterlinkbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.ForwardedHeaderFilter;

@Component
public class AzureConfig {

    /**
     * Registers Spring's ForwardedHeaderFilter so that X-Forwarded-For, X-Forwarded-Proto,
     * and related headers set by Azure Front Door are honoured for redirect URLs and client-IP
     * extraction used by the rate limiter.
     *
     * SECURITY NOTE: This filter trusts ALL X-Forwarded-* headers unconditionally. An attacker
     * who can reach the application directly (bypassing Azure Front Door) can spoof these headers
     * and defeat IP-based rate limiting. Azure App Service network rules MUST be configured to
     * block inbound traffic that does not originate from Azure Front Door's service tag
     * (AzureFrontDoor.Backend) to close this bypass window.
     *
     * TODO: Spring's ForwardedHeaderFilter supports a trusted-proxy allowlist in newer framework
     * versions. Evaluate upgrading and configuring a fixed set of trusted Azure Front Door egress
     * CIDRs so that spoofed forwarded headers from untrusted sources are silently dropped rather
     * than honoured, providing defence-in-depth at the application layer.
     */
    @Bean
    ForwardedHeaderFilter forwardedHeaderFilter() {
        return new ForwardedHeaderFilter();
    }
}
