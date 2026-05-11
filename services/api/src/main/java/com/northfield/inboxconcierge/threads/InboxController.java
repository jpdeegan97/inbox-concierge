package com.northfield.inboxconcierge.threads;

import com.northfield.inboxconcierge.buckets.Bucket;
import com.northfield.inboxconcierge.buckets.BucketRepository;
import com.northfield.inboxconcierge.gmail.GmailService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;
import com.google.api.services.gmail.model.Thread;
import java.util.*;

@RestController
@RequestMapping("/api/inbox")
public class InboxController {

    private final BucketRepository bucketRepository;
    private final GmailService gmailService;

    public InboxController(BucketRepository bucketRepository, GmailService gmailService) {
        this.bucketRepository = bucketRepository;
        this.gmailService = gmailService;
    }

    @PostMapping("/load")
    public Map<String, Object> loadInbox(OAuth2AuthenticationToken token) {
        // Load default buckets if none exist
        if (bucketRepository.count() == 0) {
            bucketRepository.save(new Bucket("important", "Important", "Requires attention", 1, true, false));
            bucketRepository.save(new Bucket("can_wait", "Can Wait", "Not urgent", 2, true, false));
        }

        List<Bucket> buckets = bucketRepository.findAll();
        
        List<Map<String, Object>> groups;
        try {
            List<Thread> gmailThreads = gmailService.fetchRecentThreads(token, 10); // limited for demo
            groups = categorizeThreads(buckets, gmailThreads);
        } catch (Exception e) {
            // Fallback to mock if oauth is missing or fails (for MVP ease)
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
        List<Bucket> newBuckets = request.getOrDefault("buckets", new ArrayList<>());
        for (Bucket b : newBuckets) {
            if (b.getId() == null || b.getId().isEmpty()) {
                b.setId(UUID.randomUUID().toString());
            }
        }
        bucketRepository.saveAll(newBuckets);
        
        List<Bucket> allBuckets = bucketRepository.findAll();

        List<Map<String, Object>> groups;
        try {
            List<Thread> gmailThreads = gmailService.fetchRecentThreads(token, 10);
            groups = categorizeThreads(allBuckets, gmailThreads);
        } catch (Exception e) {
            groups = mockGroups(allBuckets);
        }

        return Map.of(
            "buckets", allBuckets,
            "groups", groups,
            "run", Map.of(
                "runId", "run_" + UUID.randomUUID().toString().substring(0, 8),
                "classifiedAt", java.time.Instant.now().toString(),
                "model", "mock-llm-classifier",
                "totalThreads", 10
            )
        );
    }

    private List<Map<String, Object>> categorizeThreads(List<Bucket> buckets, List<Thread> realThreads) {
        List<Map<String, Object>> groups = new ArrayList<>();
        int i = 0;
        for (Bucket bucket : buckets) {
            List<Map<String, Object>> mappedThreads = new ArrayList<>();
            // Distribute real threads naively across buckets for demo since LLM is not real
            if (i < realThreads.size()) {
                Thread t = realThreads.get(i);
                mappedThreads.add(Map.of(
                    "threadId", t.getId(),
                    "sender", "Gmail User",
                    "time", "Just now",
                    "unread", false,
                    "subject", t.getSnippet() != null ? t.getSnippet().substring(0, Math.min(20, t.getSnippet().length())) : "No Subject",
                    "preview", t.getSnippet() != null ? t.getSnippet() : "",
                    "classification", Map.of(
                        "threadId", t.getId(),
                        "bucketId", bucket.getId(),
                        "confidence", 0.99
                    ),
                    "tags", List.of()
                ));
            }
            groups.add(Map.of(
                "bucketId", bucket.getId(),
                "bucketName", bucket.getName(),
                "threads", mappedThreads
            ));
            i++;
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
