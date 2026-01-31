package net.fosterlink.fosterlinkbackend.config.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    // Sustained rate limit
    int requests() default 50;
    int durationSeconds() default 60;

    // Burst rate limit (set to 0 to disable burst limiting)
    int burstRequests() default 0;
    int burstDurationSeconds() default 10;

    String keyType() default "IP"; // IP or USER

}
