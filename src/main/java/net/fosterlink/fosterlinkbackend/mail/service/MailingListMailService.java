package net.fosterlink.fosterlinkbackend.mail.service;

import net.fosterlink.fosterlinkbackend.entities.UserEntity;
import net.fosterlink.fosterlinkbackend.mail.CheckEmailPreference;
import net.fosterlink.fosterlinkbackend.repositories.MailingListMemberRepository;
import net.fosterlink.fosterlinkbackend.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

@Service
public class MailingListMailService {

    private static final Logger log = LoggerFactory.getLogger(MailingListMailService.class);

    private final MailingListMemberRepository mailingListMemberRepository;
    private final UserRepository userRepository;

    public MailingListMailService(MailingListMemberRepository mailingListMemberRepository,
                                  UserRepository userRepository) {
        this.mailingListMemberRepository = mailingListMemberRepository;
        this.userRepository = userRepository;
    }

    public void sendToMailingList(String mailingListName, Consumer<UserEntity> sendFn) {
        List<Integer> userIds = mailingListMemberRepository.findUserIdsByMailingListName(mailingListName);
        if (userIds.isEmpty()) {
            log.debug("MailingListMailService: no members found for list '{}'", mailingListName);
            return;
        }
        log.debug("MailingListMailService: dispatching to {} member(s) of '{}'", userIds.size(), mailingListName);
        userRepository.findAllById(userIds).forEach(sendFn);
    }
}
