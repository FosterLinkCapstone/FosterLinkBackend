package net.fosterlink.fosterlinkbackend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.jsonwebtoken.Claims;
import net.fosterlink.fosterlinkbackend.models.auth.CachedUserData;
import net.fosterlink.fosterlinkbackend.models.auth.LoggedInUser;
import net.fosterlink.fosterlinkbackend.service.BanStatusService;
import net.fosterlink.fosterlinkbackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private UserService userService;
    @Autowired
    private BanStatusService banStatusService;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        /*if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }*/

        String jwt = getJwtFromRequest(request);

        Claims claims = jwt != null ? jwtTokenProvider.parseClaimsOrNull(jwt) : null;

        if (claims != null) {
            String username = claims.getSubject();
            Integer tv = claims.get(JwtTokenProvider.CLAIM_TOKEN_VERSION, Integer.class);

            // Reject tokens that are missing the tv (token version) claim entirely —
            // a missing claim cannot be safely compared and indicates a legacy or malformed token.
            if (tv == null) {
                filterChain.doFilter(request, response);
                return;
            }

            CachedUserData cached = userService.loadCachedData(username);
            if (cached == null) {
                filterChain.doFilter(request, response);
                return;
            }

            if (banStatusService.isBanned(cached.databaseId())) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            if (cached.authTokenVersion() == tv) {
                // Construct a LoggedInUser without the BCrypt hash — the hash is not needed
                // after authentication; storing it in a heap-visible object would expose it
                // to heap dumps. The password field is set to "" intentionally (GAP-06).
                LoggedInUser loggedIn = new LoggedInUser(
                        cached.databaseId(), cached.email(), cached.authTokenVersion(),
                        "", cached.roles(), true, true, true, true, cached.restricted());
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(loggedIn, null, loggedIn.getAuthorities());
                auth.setDetails(new WebAuthenticationDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
