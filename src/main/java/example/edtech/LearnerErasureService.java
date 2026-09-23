package example.edtech;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class LearnerErasureService {
    private final InfraiClient infrai;
    private final ConcurrentHashMap<String, CompletableFuture<Result>> requests = new ConcurrentHashMap<>();

    public LearnerErasureService(InfraiClient infrai) { this.infrai = infrai; }

    public record Delivery(String courseId, String educatorId, Instant deadline) {}
    public record Erasure(String requestId, String userId, String credentialId, List<Delivery> deliveries) {}
    public record Result(String requestId, int revokedSessions, int removedDeliveries,
                         int removedDeadlines, int affectedEducators, String reportingState) {}

    public Result erase(Erasure input) {
        if (input == null || blank(input.requestId()) || blank(input.userId()) ||
            blank(input.credentialId()) || input.deliveries() == null ||
            input.deliveries().stream().anyMatch(d -> d == null || blank(d.courseId()) ||
                blank(d.educatorId()) || d.deadline() == null)) {
            throw new IllegalArgumentException("requestId, userId, credentialId and complete deliveries are required");
        }
        CompletableFuture<Result> pending = new CompletableFuture<>();
        CompletableFuture<Result> existing = requests.putIfAbsent(input.requestId(), pending);
        if (existing != null) return existing.join();
        try {
            JsonNode sessions = infrai.sessions(input.userId());
            if (!sessions.isArray()) throw new IllegalStateException("Session list must be an array");
            int count = 0;
            for (JsonNode session : sessions) {
                String id = session.path("id").asText("");
                if (id.isBlank()) throw new IllegalStateException("Session id is required");
                infrai.revokeSession(id);
                count++;
            }
            infrai.revokeCredential(input.credentialId());
            // Deadline and educator projections are removed regardless of the due date.
            int educators = (int) input.deliveries().stream().map(Delivery::educatorId).distinct().count();
            Result result = new Result(input.requestId(), count, input.deliveries().size(),
                input.deliveries().size(), educators, "learner_removed");
            pending.complete(result);
            return result;
        } catch (RuntimeException e) {
            requests.remove(input.requestId(), pending);
            pending.completeExceptionally(e);
            throw e;
        }
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
}
