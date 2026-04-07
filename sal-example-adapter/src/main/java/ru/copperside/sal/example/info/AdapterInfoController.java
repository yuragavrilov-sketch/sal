package ru.copperside.sal.example.info;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.copperside.sal.starter.SalProperties;

import java.util.Map;

/**
 * Returns basic adapter information.
 * <p>
 * C# origin: {@code AdapterInfoMiddleware}
 */
@RestController
public class AdapterInfoController {

    private final SalProperties properties;

    public AdapterInfoController(SalProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/info")
    public Map<String, Object> info() {
        return Map.of(
                "name", properties.getAdapter().getName(),
                "type", properties.getAdapter().getType()
        );
    }
}
