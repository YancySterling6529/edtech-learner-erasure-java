package example.edtech;

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class ErasureApplication {
    private final LearnerErasureService service;

    public ErasureApplication(LearnerErasureService service) { this.service = service; }

    public static void main(String[] args) { SpringApplication.run(ErasureApplication.class, args); }

    @PostMapping("/learner-erasures")
    public LearnerErasureService.Result erase(@RequestBody LearnerErasureService.Erasure input) {
        return service.erase(input);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalid(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(InfraiClient.InfraiRejected.class)
    public ResponseEntity<Map<String, String>> rejected(InfraiClient.InfraiRejected e) {
        HttpStatus status = e.status >= 400 && e.status < 500 ? HttpStatus.valueOf(e.status) : HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(status).body(Map.of("error", e.code, "message", e.getMessage()));
    }
}
