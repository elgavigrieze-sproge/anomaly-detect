package com.anomalydetect.ai;

import com.anomalydetect.model.Anomaly;
import com.anomalydetect.model.AnomalyType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI-powered service that generates human-readable explanations for detected anomalies.
 * 
 * Uses Cloudflare Workers AI (Llama 3.3 70B) to analyze anomalies and suggest
 * possible causes based on the market context.
 */
@Service
@Slf4j
public class AnomalyExplainerService {

    private static final String TEXT_MODEL = "@cf/meta/llama-3.3-70b-instruct-fp8-fast";

    private final WebClient webClient;
    private final String accountId;
    private final String apiToken;
    private final boolean enabled;
    private final int timeoutSeconds;

    public AnomalyExplainerService(
            @Value("${anomaly.ai.cloudflare.account-id:}") String accountId,
            @Value("${anomaly.ai.cloudflare.api-token:}") String apiToken,
            @Value("${anomaly.ai.timeout-seconds:10}") int timeoutSeconds) {
        
        this.accountId = accountId;
        this.apiToken = apiToken;
        this.timeoutSeconds = timeoutSeconds;
        this.enabled = accountId != null && !accountId.isBlank() 
                    && apiToken != null && !apiToken.isBlank();

        this.webClient = WebClient.builder()
                .baseUrl("https://api.cloudflare.com/client/v4/accounts")
                .defaultHeader("Authorization", "Bearer " + apiToken)
                .build();

        if (enabled) {
            log.info("AI Explainer Service enabled (Cloudflare Workers AI)");
        } else {
            log.warn("AI Explainer Service disabled - set CLOUDFLARE_ACCOUNT_ID and CLOUDFLARE_API_TOKEN to enable");
        }
    }

    /**
     * Generates an AI explanation for the given anomaly.
     * 
     * @param anomaly the detected anomaly
     * @return a Mono containing the explanation, or a fallback message if AI is unavailable
     */
    public Mono<String> explain(Anomaly anomaly) {
        if (!enabled) {
            return Mono.just(generateFallbackExplanation(anomaly));
        }

        String prompt = buildPrompt(anomaly);
        
        return callCloudflareAI(prompt)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .doOnNext(explanation -> log.debug("AI explanation for {}: {}", 
                        anomaly.getTicker(), explanation))
                .onErrorResume(ex -> {
                    log.warn("AI explanation failed for {}: {}", 
                            anomaly.getTicker(), ex.getMessage());
                    return Mono.just(generateFallbackExplanation(anomaly));
                });
    }

    /**
     * Blocking version for synchronous contexts.
     */
    public String explainSync(Anomaly anomaly) {
        return explain(anomaly).block();
    }

    private String buildPrompt(Anomaly anomaly) {
        String anomalyDescription = anomaly.getAnomalyType() == AnomalyType.PRICE 
                ? "price movement" 
                : "trading volume";

        String direction = anomaly.getZScore() > 0 ? "increase" : "decrease";
        double stdDevs = Math.abs(anomaly.getZScore());

        return String.format("""
            You are a financial market analyst AI. Explain the following stock market anomaly in 2-3 concise sentences.
            
            Anomaly Details:
            - Ticker: %s
            - Type: Unusual %s
            - Z-Score: %.2f (%.1f standard deviations %s from normal)
            - Current Price: $%.2f
            - Volume: %.0f
            
            Provide a brief, professional explanation of:
            1. What this anomaly indicates
            2. Possible causes (earnings, news, institutional activity, market conditions)
            
            Keep the response under 100 words. Be specific but concise.
            """,
                anomaly.getTicker(),
                anomalyDescription,
                anomaly.getZScore(),
                stdDevs,
                direction,
                anomaly.getPrice(),
                anomaly.getVolume()
        );
    }

    private Mono<String> callCloudflareAI(String prompt) {
        String requestBody = String.format("""
            {
              "messages": [
                {"role": "system", "content": "You are a concise financial analyst. Provide brief, professional market analysis."},
                {"role": "user", "content": "%s"}
              ],
              "max_tokens": 256
            }
            """, escapeJson(prompt));

        return webClient.post()
                .uri("/{accountId}/ai/run/{model}", accountId, TEXT_MODEL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::extractResponseText);
    }

    private String extractResponseText(String json) {
        // Extract the "response" field from Cloudflare's response format
        // {"result":{"response":"..."},"success":true,...}
        Pattern p = Pattern.compile("\"response\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"", Pattern.DOTALL);
        Matcher m = p.matcher(json);
        
        if (m.find()) {
            return m.group(1)
                    .replace("\\n", " ")
                    .replace("\\\"", "\"")
                    .replace("\\/", "/")
                    .replace("\\\\", "\\")
                    .trim();
        }
        
        log.warn("Could not parse AI response: {}", json);
        return "Analysis unavailable.";
    }

    private String generateFallbackExplanation(Anomaly anomaly) {
        String type = anomaly.getAnomalyType() == AnomalyType.PRICE ? "price" : "volume";
        String direction = anomaly.getZScore() > 0 ? "spike" : "drop";
        double stdDevs = Math.abs(anomaly.getZScore());

        return String.format(
                "%s detected a significant %s %s (%.1fσ from baseline). " +
                "This level of deviation typically warrants investigation for news, " +
                "earnings announcements, or unusual market activity.",
                anomaly.getTicker(), type, direction, stdDevs
        );
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    public boolean isEnabled() {
        return enabled;
    }
}
