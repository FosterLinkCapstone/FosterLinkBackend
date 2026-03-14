package net.fosterlink.fosterlinkbackend.config.audit;

import net.fosterlink.fosterlinkbackend.config.tokenauth.TokenAuthAspect;
import net.fosterlink.fosterlinkbackend.entities.AuditLogEntity;
import net.fosterlink.fosterlinkbackend.repositories.AuditLogRepository;
import net.fosterlink.fosterlinkbackend.util.JwtUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Objects;

@Aspect
@Component
public class AuditLogAspect {

    private static final Logger log = LoggerFactory.getLogger(TokenAuthAspect.class);

    private @Autowired AuditLogRepository auditLogRepository;

    @Around("@annotation(auditLog)")
    public Object auditLog(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {

        Object[] args = joinPoint.getArgs();
        Object rawTargetUserId = args[auditLog.targetUserIdIndex()];

        int userId;
        if (rawTargetUserId instanceof Number n) {
            userId = n.intValue();
        } else {
            try {
                userId = Integer.parseInt(rawTargetUserId.toString());
            } catch (NumberFormatException e) {
                log.warn("Could not audit \"{}\": \"{}\" was not a valid user id", auditLog.action(), rawTargetUserId);
                return joinPoint.proceed();
            }
        }
        Integer actingUserId;
        if (auditLog.usesTokenAuth()) {
            actingUserId = null;
        } else {
            try {
                actingUserId = Objects.requireNonNull(JwtUtil.getLoggedInUser()).getDatabaseId();
            } catch (NullPointerException e) {
                log.warn("Could not audit \"{}\": acting user did not produce a valid database ID", auditLog.action());
                return joinPoint.proceed();
            }
        }


        AuditLogEntity auditLogEntity = new AuditLogEntity();
        auditLogEntity.setAction(auditLog.action());
        auditLogEntity.setActingUserId(actingUserId);
        auditLogEntity.setTargetUserId(userId);
        auditLogEntity.setCreatedAt(new Date());

        auditLogRepository.save(auditLogEntity);

        return joinPoint.proceed();
    }

}
