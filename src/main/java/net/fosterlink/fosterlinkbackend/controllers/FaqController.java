package net.fosterlink.fosterlinkbackend.controllers;

import net.fosterlink.fosterlinkbackend.entities.FAQApprovalEntity;
import net.fosterlink.fosterlinkbackend.entities.FaqEntity;
import net.fosterlink.fosterlinkbackend.entities.UserEntity;
import net.fosterlink.fosterlinkbackend.models.rest.FaqResponse;
import net.fosterlink.fosterlinkbackend.models.web.faq.ApproveFaqModel;
import net.fosterlink.fosterlinkbackend.repositories.FAQApprovalRepository;
import net.fosterlink.fosterlinkbackend.repositories.FAQRepository;
import net.fosterlink.fosterlinkbackend.repositories.UserRepository;
import net.fosterlink.fosterlinkbackend.repositories.mappers.FaqMapper;
import net.fosterlink.fosterlinkbackend.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/v1/faq/")
public class FaqController {

    private @Autowired FAQRepository fAQRepository;
    private @Autowired FaqMapper faqMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FAQApprovalRepository fAQApprovalRepository;


    @GetMapping("/all")
    public ResponseEntity<?> getAllFaqs() {
        return ResponseEntity.ok(faqMapper.allApprovedPreviews());
    }
    @GetMapping("/content")
    public ResponseEntity<?> getContentFor(@RequestParam int id) {
        Optional<FaqEntity> faq = fAQRepository.findById(id);
        if (faq.isPresent()) {
            return ResponseEntity.ok(faq.get().getContent());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingFaqs() {
        if (JwtUtil.isLoggedIn()) {
            UserEntity user = userRepository.findByEmail(JwtUtil.getLoggedInEmail());
            if (user.isAdministrator()) {
                return ResponseEntity.ok(faqMapper.allPendingPreviews());
            } else {
                return ResponseEntity.status(403).build();
            }
        } else {
            return ResponseEntity.status(403).build();
        }
    }
    @PostMapping("/approve")
    public ResponseEntity<?> approveFaq(@RequestBody ApproveFaqModel faq) {
        if (JwtUtil.isLoggedIn()) {
            UserEntity user = userRepository.findByEmail(JwtUtil.getLoggedInEmail());
            if (user.isAdministrator()) {
                Optional<FaqEntity> faqEntity = fAQRepository.findById(faq.getId());
                if (faqEntity.isPresent()) {
                    FAQApprovalEntity entity = new FAQApprovalEntity();
                    entity.setApproved(faq.isApproved());
                    entity.setApproved_by_id(user.getId());
                    entity.setFaq_id(faqEntity.get().getId());
                    fAQApprovalRepository.save(entity);
                    return ResponseEntity.ok().build();
                } else {
                    return ResponseEntity.notFound().build();
                }
            } else {
                return ResponseEntity.status(403).build();
            }
        } else {
            return ResponseEntity.status(403).build();
        }
    }
}
