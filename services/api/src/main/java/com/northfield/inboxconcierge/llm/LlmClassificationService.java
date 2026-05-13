package com.northfield.inboxconcierge.llm;

import org.springframework.data.redis.core.StringRedisTemplate;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.gmail.model.Thread;
import com.northfield.inboxconcierge.buckets.Bucket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LlmClassificationService {

    @Value("${OPENAI_API_KEY:mock-key}")
    private String openAiApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final StringRedisTemplate redisTemplate;

    public LlmClassificationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // Semaphore strictly restricts max parallel HTTP connections to 5 simultaneously
    private final java.util.concurrent.Semaphore concurrencyThrottle = new java.util.concurrent.Semaphore(5);
    
    // Simple native Token Bucket throttle variable
    private long lastApiRequestTime = 0;

    // Forces a hard floor on the polling rate (150ms between ticks guarantees max ~400 Requests Per Minute)
    private synchronized void enforceTokenBucketRateLimit() throws InterruptedException {
        long now = System.currentTimeMillis();
        long minInterval = 150; 
        if (now - lastApiRequestTime < minInterval) {
            java.lang.Thread.sleep(minInterval - (now - lastApiRequestTime));
        }
        lastApiRequestTime = System.currentTimeMillis();
    }

    public Map<String, String> classifyThreads(List<Thread> threads, List<Bucket> buckets) {
        if (openAiApiKey.equals("mock-key")) {
            // Mock behavior if no key is provided
            Map<String, String> mockClassification = new HashMap<>();
            int i = 0;
            for (Thread t : threads) {
                mockClassification.put(t.getId(), buckets.get(i % buckets.size()).getId());
                i++;
            }
            return mockClassification;
        }

        String url = "https://api.openai.com/v1/chat/completions";

        List<Map<String, String>> bucketData = buckets.stream()
                .map(b -> Map.of("id", b.getId(), "name", b.getName(), "description", b.getDescription()))
                .collect(Collectors.toList());

        Map<String, String> classifications = new java.util.concurrent.ConcurrentHashMap<>();

        try {
            String systemMessage = "You are an intelligent email bucket classifier. Your task is to categorize a single email into one of the following buckets based on its text snippet.\n" +
                    "Buckets available:\n" + objectMapper.writeValueAsString(bucketData) + "\n\n" +
                    "Respond exactly with a JSON object containing a single string property 'bucketId' with the exact ID of the chosen bucket.";

            String bucketsHash = String.valueOf(bucketData.hashCode());

            threads.parallelStream().forEach(t -> {
                String cacheKey = "classification:" + t.getId() + ":" + bucketsHash;
                try {
                    String cachedBucket = redisTemplate.opsForValue().get(cacheKey);
                    if (cachedBucket != null) {
                        classifications.put(t.getId(), cachedBucket);
                        System.out.println(">>> [Cache Hit] Thread " + t.getId() + " mapped instantly to: " + cachedBucket);
                        return; 
                    }
                } catch (Exception e) {
                    // Fail cleanly and default to api call if Redis is unreachable
                }

                try {
                    String subject = "No Subject";
                    String sender = "Unknown Sender";
                    if (t.getMessages() != null && !t.getMessages().isEmpty()) {
                        var firstMsg = t.getMessages().get(0);
                        if (firstMsg.getPayload() != null && firstMsg.getPayload().getHeaders() != null) {
                            for (var h : firstMsg.getPayload().getHeaders()) {
                                if ("Subject".equalsIgnoreCase(h.getName())) subject = h.getValue();
                                if ("From".equalsIgnoreCase(h.getName())) sender = h.getValue();
                            }
                        }
                    }

                    String userMessage = "Sender: " + sender + "\n" +
                                         "Subject: " + subject + "\n" +
                                         "Email snippet:\n" + (t.getSnippet() != null ? t.getSnippet() : "");

                    Map<String, Object> requestBody = new HashMap<>();
                    requestBody.put("model", "gpt-4o-mini");
                    requestBody.put("temperature", 0.0);
                    requestBody.put("response_format", Map.of("type", "json_object"));
                    requestBody.put("messages", List.of(
                            Map.of("role", "system", "content", systemMessage),
                            Map.of("role", "user", "content", userMessage)
                    ));

                    System.out.println("\n>>> [API Request] Thread " + t.getId() + " | Snippet length: " + userMessage.length());

                    int maxAttempts = 3;
                    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                        try {
                            concurrencyThrottle.acquire();
                            try {
                                enforceTokenBucketRateLimit();

                                HttpHeaders headers = new HttpHeaders();
                                headers.setContentType(MediaType.APPLICATION_JSON);
                                headers.setBearerAuth(openAiApiKey);

                                HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
                                ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

                                String rawResponseBody = response.getBody();
                                System.out.println("<<< [API Response] Thread " + t.getId() + "\n" + rawResponseBody + "\n");

                                JsonNode root = objectMapper.readTree(rawResponseBody);
                                String resultContent = root.path("choices").get(0).path("message").path("content").asText();

                                JsonNode resultRoot = objectMapper.readTree(resultContent);
                                String bucketId = resultRoot.path("bucketId").asText();
                                
                                if (bucketId != null && !bucketId.trim().isEmpty() && !bucketId.equals("null")) {
                                    classifications.put(t.getId(), bucketId);
                                    try {
                                        redisTemplate.opsForValue().set(cacheKey, bucketId, Duration.ofDays(7));
                                    } catch (Exception e) {}
                                } else {
                                    classifications.put(t.getId(), buckets.get(0).getId());
                                }
                                break; 
                            } finally {
                                concurrencyThrottle.release();
                            }

                        } catch (Exception e) {
                            if (attempt == maxAttempts) {
                                System.err.println("Failed to classify thread " + t.getId() + " after " + maxAttempts + " attempts: " + e.getMessage());
                                classifications.put(t.getId(), buckets.get(0).getId());
                            } else {
                                long sleepMillis = attempt * 1500L + (long)(Math.random() * 1000);
                                System.err.println("Rate limit or error for thread " + t.getId() + " on attempt " + attempt + ". Retrying in " + sleepMillis + "ms... (" + e.getMessage() + ")");
                                try { java.lang.Thread.sleep(sleepMillis); } catch (InterruptedException ie) { java.lang.Thread.currentThread().interrupt(); }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Fatal error for thread " + t.getId() + ": " + e.getMessage());
                    classifications.put(t.getId(), buckets.get(0).getId());
                }
            });
            return classifications;

        } catch (Exception e) {
            System.err.println("Fatal OpenAI Setup failure: " + e.getMessage());
            Map<String, String> fallback = new HashMap<>();
            int i = 0;
            for (Thread t : threads) {
                fallback.put(t.getId(), buckets.get(i % buckets.size()).getId());
                i++;
            }
            return fallback;
        }
    }
}
