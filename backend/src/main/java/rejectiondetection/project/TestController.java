package rejectiondetection.project;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
public class TestController {

    @GetMapping("/api/test")
    public String testEndpoint() {
        return "Test successful";
    }
    
    
}
