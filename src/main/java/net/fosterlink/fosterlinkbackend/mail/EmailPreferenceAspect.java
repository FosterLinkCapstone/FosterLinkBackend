package net.fosterlink.fosterlinkbackend.mail;

import net.fosterlink.fosterlinkbackend.entities.UserEntity;
import net.fosterlink.fosterlinkbackend.entities.EmailTypeEntity;
import net.fosterlink.fosterlinkbackend.repositories.DontSendEmailRepository;
import net.fosterlink.fosterlinkbackend.repositories.EmailTypeRepository;
import net.fosterlink.fosterlinkbackend.repositories.UserRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Aspect that intercepts methods annotated with {@link CheckEmailPreference}.
 * Before the method runs, checks whether the recipient has opted out (global or for this email type);
 * if so, the method is not invoked and the aspect returns null.
 */
@Aspect
@Component
public class EmailPreferenceAspect {

    private static final Logger log = LoggerFactory.getLogger(EmailPreferenceAspect.class);

    private final UserRepository userRepository;
    private final EmailTypeRepository emailTypeRepository;
    private final DontSendEmailRepository dontSendEmailRepository;

    public EmailPreferenceAspect(UserRepository userRepository,
                                EmailTypeRepository emailTypeRepository,
                                DontSendEmailRepository dontSendEmailRepository) {
        this.userRepository = userRepository;
        this.emailTypeRepository = emailTypeRepository;
        this.dontSendEmailRepository = dontSendEmailRepository;
    }

    @Around("@annotation(checkEmailPreference)")
    public Object checkPreferenceAndProceed(ProceedingJoinPoint joinPoint, CheckEmailPreference checkEmailPreference) throws Throwable {
        int userId = resolveUserId(joinPoint, checkEmailPreference);
        if (userId <= 0) {
            log.warn("CheckEmailPreference: could not resolve userId for method {}, proceeding anyway", joinPoint.getSignature().getName());
            return joinPoint.proceed();
        }

        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.debug("CheckEmailPreference: user id {} not found, proceeding", userId);
            return joinPoint.proceed();
        }
        if (userOpt.get().isUnsubscribeAll()) {
            log.debug("CheckEmailPreference: user {} has unsubscribeAll, skipping email type {}", userId, checkEmailPreference.value());
            return null;
        }

        Optional<EmailTypeEntity> typeOpt = emailTypeRepository.findByName(checkEmailPreference.value());
        if (typeOpt.isEmpty()) {
            log.warn("CheckEmailPreference: email type '{}' not found in email_type table, proceeding", checkEmailPreference.value());
            return joinPoint.proceed();
        }

        if (dontSendEmailRepository.existsByUserIdAndEmailTypeId(userId, typeOpt.get().getId())) {
            log.debug("CheckEmailPreference: user {} opted out of {}, skipping", userId, checkEmailPreference.value());
            return null;
        }

        return joinPoint.proceed();
    }

    private static int resolveUserId(ProceedingJoinPoint joinPoint, CheckEmailPreference checkEmailPreference) {
        Object[] args = joinPoint.getArgs();
        int index = checkEmailPreference.userIdParamIndex();
        if (index < 0 || index >= args.length) {
            return 0;
        }
        Object arg = args[index];
        if (arg instanceof Integer) {
            return (Integer) arg;
        }
        if (arg instanceof Number) {
            return ((Number) arg).intValue();
        }
        return 0;
    }
}
