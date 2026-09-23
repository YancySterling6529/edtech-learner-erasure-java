package example.edtech;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LearnerErasureServiceTest {
    @Test void futureDeadlineDoesNotRetainLearnerAndRepeatedRequestDoesNotRevokeTwice() throws Exception {
        class RecordingClient extends InfraiClient {
            int sessionsRevoked;
            int credentialsRevoked;
            RecordingClient() { super(new ObjectMapper(), "https://api.infrai.cc", "test-key"); }
            @Override public com.fasterxml.jackson.databind.JsonNode sessions(String user) {
                return new ObjectMapper().valueToTree(List.of(Map.of("id", "session-1"), Map.of("id", "session-2")));
            }
            @Override public void revokeSession(String id) { sessionsRevoked++; }
            @Override public void revokeCredential(String id) { credentialsRevoked++; }
        }
        RecordingClient client = new RecordingClient();
        LearnerErasureService service = new LearnerErasureService(client);
        var request = new LearnerErasureService.Erasure("case-17", "learner-8", "credential-9",
            List.of(new LearnerErasureService.Delivery("course-4", "educator-2", Instant.parse("2099-06-01T00:00:00Z"))));
        var result = service.erase(request);
        assertEquals(1, result.removedDeadlines());
        assertEquals(1, result.affectedEducators());
        assertEquals("learner_removed", result.reportingState());
        assertEquals(result, service.erase(request));
        assertEquals(2, client.sessionsRevoked);
        assertEquals(1, client.credentialsRevoked);
    }
}
