package net.fosterlink.fosterlinkbackend.config.tokenauth;

import net.fosterlink.fosterlinkbackend.entities.TokenAuthEntity;
import net.fosterlink.fosterlinkbackend.repositories.TokenAuthRepository;
import net.fosterlink.fosterlinkbackend.service.TokenAuthService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;


@Aspect
@Component
public class TokenAuthAspect {

    private static final Logger log = LoggerFactory.getLogger(TokenAuthAspect.class);

    private @Autowired TokenAuthRepository tokenAuthRepository;
    private @Autowired TokenAuthService tokenAuthService;

    @Around("@annotation(tokenAuth)")
    public Object tokenAuth(ProceedingJoinPoint joinPoint, TokenAuth tokenAuth) throws Throwable {

        Object[] args = joinPoint.getArgs();
        Object rawToken = args[tokenAuth.tokenParamIndex()];

        // Pattern-matching instanceof combines null check, type check, and cast.
        if (!(rawToken instanceof String token)) {
            log.error("TokenAuthAspect#tokenAuth argument at index {} is not a String (was: {})",
                    tokenAuth.tokenParamIndex(), rawToken);
            return ResponseEntity.badRequest().build();
        }

        Object rawUserId = args[tokenAuth.userIdParamIndex()];
        int userId;
        if (rawUserId instanceof Number n) {
            // Fast path: Spring/Jackson typically binds numeric params as Integer already.
            userId = n.intValue();
        } else {
            try {
                userId = Integer.parseInt(String.valueOf(rawUserId));
            } catch (NumberFormatException e) {
                userId = -1;
            }
        }

        String hashedToken = tokenAuthService.hashToken(token);

        // Look up the processId before consuming, so we can bulk-delete sibling tokens afterward.
        // The SELECT here does not introduce a TOCTOU race: the subsequent atomic DELETE is still
        // the exclusive gate. The SELECT is read-only and only used for cleanup.
        Optional<TokenAuthEntity> entityOpt = tokenAuthRepository.findByTokenAndEndpointNonExpired(hashedToken, tokenAuth.endpointName());
        String processId = entityOpt.map(TokenAuthEntity::getProcessId).orElse(null);

        // Atomically delete the token and check whether exactly one row was affected.
        int consumed = tokenAuthRepository.consumeValidToken(hashedToken, tokenAuth.endpointName(), userId);
        if (consumed == 0) return ResponseEntity.status(403).build();

        Object result = joinPoint.proceed(args);

        // Revoke all remaining sibling tokens that share the same processId (e.g. the other
        // founders' approve links for this same request) now that one has been acted upon.
        if (processId != null) {
            tokenAuthRepository.deleteAllByProcessId(processId);
        }

        return result;
    }

}
