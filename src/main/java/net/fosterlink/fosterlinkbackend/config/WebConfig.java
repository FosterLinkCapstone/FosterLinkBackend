package net.fosterlink.fosterlinkbackend.config;

import net.fosterlink.fosterlinkbackend.config.ratelimit.RatelimitInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private RatelimitInterceptor ratelimitInterceptor;

    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(ratelimitInterceptor);
    }

}
