package net.fosterlink.fosterlinkbackend.config.tokenauth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TokenAuth {

    int tokenParamIndex() default 0;
    int userIdParamIndex() default 1;
    String endpointName();
    /** When false, the token is validated but not deleted — use for idempotent actions like unsubscribe. */
    boolean consumeOnUse() default true;

}
