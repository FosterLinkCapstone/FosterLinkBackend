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
