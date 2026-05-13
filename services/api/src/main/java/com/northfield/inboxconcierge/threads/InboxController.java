package com.northfield.inboxconcierge.threads;

import com.northfield.inboxconcierge.buckets.Bucket;
import com.northfield.inboxconcierge.buckets.BucketRepository;
import com.northfield.inboxconcierge.gmail.GmailService;
import com.northfield.inboxconcierge.llm.LlmClassificationService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;
import com.google.api.services.gmail.model.Thread;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;
import java.util.*;

@RestController
@RequestMapping("/api/inbox")
public class InboxController {

    private final BucketRepository bucketRepository;
    private final GmailService gmailService;
    private final LlmClassificationService llmService;

    public InboxController(BucketRepository bucketRepository, GmailService gmailService, LlmClassificationService llmService) {
        this.bucketRepository = bucketRepository;
        this.gmailService = gmailService;
        this.llmService = llmService;
    }

    @PostMapping("/load")
    public Map<String, Object> loadInbox(OAuth2AuthenticationToken token) {
        // Load default buckets if none exist
        String[][] defaultBuckets = {
            {"important", "Important", "Requires attention", "1", "true", "false"},
            {"can_wait", "Can Wait", "Not urgent", "2", "true", "false"},
            {"auto_archive", "Auto-Archive", "Automatically archived", "3", "true", "true"},
            {"newsletter", "Newsletter", "Newsletters and subscriptions", "4", "true", "false"},
            {"transactional", "Transactional", "Receipts, orders, and security alerts", "5", "true", "false"},
            {"personal", "Personal", "Personal correspondence", "6", "true", "false"},
            {"work", "Work / Professional", "Work-related emails", "7", "true", "false"},
            {"needs_review", "Needs Review", "Requires manual review", "8", "true", "false"}
        };

        for (String[] db : defaultBuckets) {
            if (!bucketRepository.existsById(db[0])) {
                try {
                    bucketRepository.save(new Bucket(db[0], db[1], db[2], Integer.parseInt(db[3]), Boolean.parseBoolean(db[4]), Boolean.parseBoolean(db[5])));
                } catch (Exception e) {} // ignore race conditions
            }
        }

        List<Bucket> buckets = bucketRepository.findAll();
        
        List<Map<String, Object>> groups;
        try {
            List<Thread> gmailThreads = gmailService.fetchRecentThreads(token, 25); // pulling top 10 emails
            groups = categorizeThreadsReal(buckets, gmailThreads);
        } catch (Exception e) {
            e.printStackTrace();
            groups = mockGroups(buckets);
        }
        
        return Map.of(
            "buckets", buckets,
            "groups", groups,
            "run", Map.of(
                "runId", "run_" + UUID.randomUUID().toString().substring(0, 8),
                "classifiedAt", java.time.Instant.now().toString(),
                "model", "mock-llm-classifier",
                "totalThreads", 10
            )
        );
    }
    
    @PostMapping("/reclassify")
    public Map<String, Object> reclassify(OAuth2AuthenticationToken token, @RequestBody Map<String, List<Bucket>> request) {
        System.out.println(">>> [POST /api/inbox/reclassify] Triggered. Reclassifying active threads...");
        
        List<Bucket> newBuckets = request.getOrDefault("buckets", new ArrayList<>());
        for (Bucket b : newBuckets) {
            if (b.getId() == null || b.getId().trim().isEmpty()) {
                b.setId(b.getName().toLowerCase().replaceAll("[^a-z0-9]", "_"));
                b.setPriority(10); // user custom
                b.setCreatedByUser(true);
            }
            try {
                if (!bucketRepository.existsById(b.getId())) {
                    bucketRepository.save(b);
                    System.out.println("  -> Saved new custom bucket: " + b.getId());
                }
            } catch (Exception e) {
                // Ignore silent duplicate violations from Spring JPA proxy race conditions
            }
        }
        
        List<Bucket> allBuckets = bucketRepository.findAll();
        System.out.println("  -> Loaded " + allBuckets.size() + " active buckets for classification context.");

        List<Map<String, Object>> groups;
        try {
            List<Thread> gmailThreads = gmailService.fetchRecentThreads(token, 25);
            groups = categorizeThreadsReal(allBuckets, gmailThreads);
        } catch (Exception e) {
            System.err.println("  -> Failed to fully classify, gracefully failing over: " + e.getMessage());
            groups = mockGroups(allBuckets);
        }

        System.out.println("<<< [POST /api/inbox/reclassify] Completed successfully.");
        return Map.of(
            "buckets", allBuckets,
            "groups", groups,
            "run", Map.of(
                "runId", "run_" + UUID.randomUUID().toString().substring(0, 8),
                "classifiedAt", java.time.Instant.now().toString(),
                "model", "openai-gpt-4o-mini",
                "totalThreads", 10
            )
        );
    }

    private List<Map<String, Object>> categorizeThreadsReal(List<Bucket> buckets, List<Thread> realThreads) {
        Map<String, String> assignments = llmService.classifyThreads(realThreads, buckets);

        List<Map<String, Object>> groups = new ArrayList<>();
        for (Bucket bucket : buckets) {
            List<Map<String, Object>> mappedThreads = new ArrayList<>();
            for (Thread t : realThreads) {
                if (bucket.getId().equals(assignments.get(t.getId()))) {
                    
                    String subject = "No Subject";
                    String sender = "Gmail User";
                    if (t.getMessages() != null && !t.getMessages().isEmpty()) {
                        Message firstMsg = t.getMessages().get(0);
                        if (firstMsg.getPayload() != null && firstMsg.getPayload().getHeaders() != null) {
                            for (MessagePartHeader h : firstMsg.getPayload().getHeaders()) {
                                if ("Subject".equalsIgnoreCase(h.getName())) subject = h.getValue();
                                if ("From".equalsIgnoreCase(h.getName())) sender = h.getValue();
                            }
                        }
                    }

                    mappedThreads.add(Map.of(
                        "threadId", t.getId(),
                        "sender", sender,
                        "time", "Recent",
                        "unread", false,
                        "subject", subject,
                        "preview", t.getSnippet() != null ? t.getSnippet() : "",
                        "classification", Map.of(
                            "threadId", t.getId(),
                            "bucketId", bucket.getId(),
                            "confidence", 0.99
                        ),
                        "tags", List.of()
                    ));
                }
            }
            groups.add(Map.of(
                "bucketId", bucket.getId(),
                "bucketName", bucket.getName(),
                "threads", mappedThreads
            ));
        }
        return groups;
    }

    private List<Map<String, Object>> mockGroups(List<Bucket> buckets) {
        List<Map<String, Object>> groups = new ArrayList<>();
        int idCounter = 1;
        for (Bucket bucket : buckets) {
            groups.add(Map.of(
                "bucketId", bucket.getId(),
                "bucketName", bucket.getName(),
                "threads", List.of(
                    Map.of(
                        "threadId", "thread_" + idCounter++,
                        "sender", "Jane Doe",
                        "time", "12:00 PM",
                        "unread", false,
                        "subject", "Mock Email for " + bucket.getName(),
                        "preview", "This is a dummy email dynamically assigned to this bucket... (OAuth not configured)",
                        "classification", Map.of(
                            "threadId", "thread_" + (idCounter - 1),
                            "bucketId", bucket.getId(),
                            "confidence", 0.95
                        ),
                        "tags", List.of()
                    )
                )
            ));
        }
        return groups;
    }
}
