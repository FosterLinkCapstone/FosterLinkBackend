package net.fosterlink.fosterlinkbackend.mail;

import net.fosterlink.fosterlinkbackend.mail.lists.MailingList;
import net.fosterlink.fosterlinkbackend.repositories.EmailTypeRepository;
import net.fosterlink.fosterlinkbackend.repositories.MailingListRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Component
public class EmailBootstrap {

    private static final Logger log = LoggerFactory.getLogger(EmailBootstrap.class);

    private @Autowired ApplicationContext applicationContext;
    private @Autowired EmailTypeRepository emailTypeRepository;
    private @Autowired JdbcTemplate jdbcTemplate;
    private @Autowired MailingListRepository mailingListRepository;


    @EventListener(ApplicationReadyEvent.class)
    public void ensureEmailTypesExist() {
        Set<EmailTypeAnnotationMeta> emailTypeAnnotationMetas = new HashSet<>();
        Set<String> mailingListNames = new HashSet<>();
        String[] beanNames = applicationContext.getBeanDefinitionNames();

        for (String beanName : beanNames) {
            try {
                Object bean = applicationContext.getBean(beanName);
                Class<?> targetClass = AopUtils.getTargetClass(bean);
                for (Class<?> c = targetClass; c != null && c != Object.class; c = c.getSuperclass()) {
                    MailingList mailingList = c.getAnnotation(MailingList.class);
                    if (mailingList != null) {
                        mailingListNames.add(c.getSimpleName());
                    }
                    for (Method method : c.getDeclaredMethods()) {
                        CheckEmailPreference ann = method.getAnnotation(CheckEmailPreference.class);
                        if (ann != null && ann.value() != null && !ann.value().isBlank()) {
                            emailTypeAnnotationMetas.add(new EmailTypeAnnotationMeta(ann.value(), Objects.equals(ann.uiName(), "") ? ann.value() : ann.uiName(), ann.canDisable()));
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Skipping bean {} when scanning for @CheckEmailPreference: {}", beanName, e.getMessage());
            }
        }

        for (EmailTypeAnnotationMeta name : emailTypeAnnotationMetas) {
            if (emailTypeRepository.findByName(name.getName()).isEmpty()) {
                jdbcTemplate.update("INSERT INTO email_type (name, ui_name, can_disable) VALUES (?, ?, ?)", name.getName(), name.getUiName(), name.isCanDisable());
                log.info("Inserted missing email_type: {}", name.getName());
            }
        }
        for (String mailingListName : mailingListNames) {
            if (!mailingListRepository.existsByName(mailingListName)) {
                jdbcTemplate.update("INSERT INTO mailing_list (name) VALUES (?)",  mailingListName);
                log.info("Inserted missing mailing_list: {}", mailingListName);
            }
        }
    }
}
