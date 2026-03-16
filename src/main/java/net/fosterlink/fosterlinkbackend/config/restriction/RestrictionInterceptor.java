package net.fosterlink.fosterlinkbackend.config.restriction;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.fosterlink.fosterlinkbackend.models.auth.LoggedInUser;
import net.fosterlink.fosterlinkbackend.util.JwtUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RestrictionInterceptor implements HandlerInterceptor {

    /*
     * Safety invariant: this interceptor silently passes unauthenticated requests through on
     * @DisallowRestricted endpoints (the isLoggedIn() guard below). This is safe ONLY because
     * every @DisallowRestricted endpoint is also protected by Spring Security authentication
     * (i.e. it must NOT appear in SecurityConfig.publicEndpoints). If an endpoint were both
     * @DisallowRestricted and publicly accessible, a restricted user could reach it anonymously.
     *
     * When adding new endpoints:
     *   - Never combine @DisallowRestricted with a path listed in SecurityConfig.publicEndpoints.
     *   - The integration test RestrictionInterceptorInvariantTest enforces this at CI time.
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        DisallowRestricted annotation = handlerMethod.getMethodAnnotation(DisallowRestricted.class);
        if (annotation == null) {
            return true;
        }

        if (!JwtUtil.isLoggedIn()) {
            return true;
        }

        LoggedInUser user = JwtUtil.getLoggedInUser();
        if (user != null && user.isRestricted()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        return true;
    }

}
