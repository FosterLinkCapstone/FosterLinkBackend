package net.fosterlink.fosterlinkbackend.mail;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a mail-sending method so that an aspect checks the user's email preferences before sending.
 * <p>
 * Convention: the first parameter of the annotated method must be the recipient's user id ({@code int} or {@code Integer}).
 * If the user has {@code unsubscribeAll} set, or has opted out of this email type in {@code dont_send_email},
 * the method is not invoked and returns {@code null} (for void methods this is ignored).
 * <p>
 * The {@link #value()} must match a row in the {@code email_type} table (e.g. {@code "ROLE_ASSIGNED"}, {@code "REGISTRATION_THANK_YOU"}).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckEmailPreference {

    /**
     * Email type name used to look up the type in {@code email_type} and to check {@code dont_send_email}.
     */
    String value();

    String uiName() default "";

    boolean canDisable() default true;

    /**
     * Zero-based index of the method parameter that holds the recipient user id.
     * Default 0 (first parameter).
     */
    int userIdParamIndex() default 0;
}
