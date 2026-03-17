package net.fosterlink.fosterlinkbackend.config.restriction;

import net.fosterlink.fosterlinkbackend.config.SecurityConfig;
import net.fosterlink.fosterlinkbackend.controllers.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Enforces the RestrictionInterceptor safety invariant:
 * no endpoint annotated with @DisallowRestricted may also appear in SecurityConfig.publicEndpoints.
 *
 * If that invariant were violated, a restricted user could reach the endpoint anonymously
 * (the interceptor passes unauthenticated requests through).
 */
class RestrictionInterceptorInvariantTest {

    private static final List<Class<?>> CONTROLLER_CLASSES = List.of(
            AgencyController.class,
            UserController.class,
            ThreadController.class,
            FaqController.class,
            AccountDeletionController.class,
            MailController.class,
            AdminUserController.class,
            AdminUserContentController.class,
            TokenAuthController.class
    );

    @Test
    void disallowRestrictedEndpointsAreNotPublic() throws Exception {
        // Retrieve publicEndpoints from SecurityConfig via reflection.
        // The field is initialised at declaration time, so a bare instance (no Spring context needed) works.
        Field field = SecurityConfig.class.getDeclaredField("publicEndpoints");
        field.setAccessible(true);
        SecurityConfig config = new SecurityConfig();
        String[] publicEndpointsArray = (String[]) field.get(config);
        Set<String> publicEndpoints = new HashSet<>(Arrays.asList(publicEndpointsArray));

        List<String> violations = new ArrayList<>();

        for (Class<?> controllerClass : CONTROLLER_CLASSES) {
            String basePath = extractBasePath(controllerClass);

            for (Method method : controllerClass.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(DisallowRestricted.class)) continue;

                for (String methodPath : extractMethodPaths(method)) {
                    String fullPath = normalize(basePath + methodPath);
                    if (publicEndpoints.contains(fullPath)) {
                        violations.add(fullPath + " in " + controllerClass.getSimpleName() + "#" + method.getName());
                    }
                }
            }
        }

        assertTrue(violations.isEmpty(),
                "@DisallowRestricted endpoints must not appear in SecurityConfig.publicEndpoints.\n" +
                "Violations found:\n  " + String.join("\n  ", violations));
    }

    private String extractBasePath(Class<?> clazz) {
        RequestMapping rm = clazz.getAnnotation(RequestMapping.class);
        if (rm == null || rm.value().length == 0) return "";
        String path = rm.value()[0];
        // Strip trailing slash so we can cleanly append the method path
        return path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }

    private List<String> extractMethodPaths(Method method) {
        if (method.isAnnotationPresent(GetMapping.class))
            return pathsOf(method.getAnnotation(GetMapping.class).value());
        if (method.isAnnotationPresent(PostMapping.class))
            return pathsOf(method.getAnnotation(PostMapping.class).value());
        if (method.isAnnotationPresent(PutMapping.class))
            return pathsOf(method.getAnnotation(PutMapping.class).value());
        if (method.isAnnotationPresent(DeleteMapping.class))
            return pathsOf(method.getAnnotation(DeleteMapping.class).value());
        if (method.isAnnotationPresent(RequestMapping.class))
            return pathsOf(method.getAnnotation(RequestMapping.class).value());
        return List.of("");
    }

    private List<String> pathsOf(String[] values) {
        if (values.length == 0) return List.of("");
        return Arrays.asList(values);
    }

    /** Normalises double slashes that arise from concatenating a stripped base path and a method path. */
    private String normalize(String path) {
        return path.replace("//", "/");
    }
}
