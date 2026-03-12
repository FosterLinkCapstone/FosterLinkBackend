package net.fosterlink.fosterlinkbackend.config.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    int targetUserIdIndex() default 0;
    boolean usesTokenAuth() default false;
    String action();

}
