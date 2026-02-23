package net.fosterlink.fosterlinkbackend.config.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RatelimitInterceptor implements HandlerInterceptor {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return true;
        }

        String key = resolveKey(request, rateLimit.keyType());
        String bucketKey = key + ":" + handlerMethod.getMethod().getName();

        Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> createBucket(rateLimit));

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            return true;
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(probe.getNanosToWaitForRefill() / 1_000_000_000));
            response.getWriter().write("Rate limit exceeded");
            return false;
        }
    }

    private String resolveKey(HttpServletRequest request, String keyType) {
        if ("USER".equals(keyType)) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
                return "user:" + authentication.getName();
            }
        }
        String ip = request.getHeader("X-FORWARDED-FOR");
        return "ip:" + (ip != null ? ip.split(",")[0].trim() : request.getRemoteAddr());
    }

    private Bucket createBucket(RateLimit rateLimit) {
        var builder = Bucket.builder();

        // Add sustained rate limit (always present)
        builder.addLimit(Bandwidth.builder()
                .capacity(rateLimit.requests())
                .refillGreedy(rateLimit.requests(), Duration.ofSeconds(rateLimit.durationSeconds()))
                .build());

        // Add burst limit if configured (burstRequests > 0)
        if (rateLimit.burstRequests() > 0) {
            builder.addLimit(Bandwidth.builder()
                    .capacity(rateLimit.burstRequests())
                    .refillGreedy(rateLimit.burstRequests(), Duration.ofSeconds(rateLimit.burstDurationSeconds()))
                    .build());
        }

        return builder.build();
    }
}
