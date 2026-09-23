package example.edtech;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InfraiClient {
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json;
    private final String baseUrl;
    private final String key;

    public InfraiClient(ObjectMapper json, @Value("${infrai.base-url}") String baseUrl,
                        @Value("${infrai.api-key}") String key) {
        this.json = json;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.key = key;
    }

    public JsonNode sessions(String userId) { return call("GET", "/v1/auth/session/list_for_user/" + segment(userId)); }
    public void revokeSession(String sessionId) { call("POST", "/v1/auth/session/revoke/" + segment(sessionId)); }
    public void revokeCredential(String credentialId) { call("DELETE", "/v1/account/keys/revoke/" + segment(credentialId)); }

    private static String segment(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
    }

    private JsonNode call(String method, String path) {
        for (int attempt = 0; attempt < 4; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .header("Authorization", "Bearer " + key)
                    .timeout(Duration.ofSeconds(15))
                    .method(method, HttpRequest.BodyPublishers.noBody()).build();
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode envelope = json.readTree(response.body());
                if (response.statusCode() == 429 && attempt < 3) {
                    long delay = response.headers().firstValue("Retry-After")
                        .map(v -> { try { return Long.parseLong(v) * 1000L; } catch (NumberFormatException e) { return 0L; } })
                        .filter(v -> v > 0).orElse(500L << attempt);
                    Thread.sleep(delay);
                    continue;
                }
                if (!envelope.path("ok").asBoolean(false)) {
                    JsonNode error = envelope.path("error");
                    throw new InfraiRejected(response.statusCode(), error.path("code").asText("REJECTED"),
                        error.path("message").asText("Request rejected"));
                }
                if (response.statusCode() >= 500) throw new IllegalStateException("Upstream request failed");
                return envelope.path("data");
            } catch (InfraiRejected e) {
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Request interrupted", e);
            } catch (java.io.IOException e) {
                throw new IllegalStateException("Request could not complete", e);
            }
        }
        throw new IllegalStateException("Retry limit reached");
    }

    public static class InfraiRejected extends RuntimeException {
        public final int status;
        public final String code;
        public InfraiRejected(int status, String code, String message) {
            super(message); this.status = status; this.code = code;
        }
    }
}
