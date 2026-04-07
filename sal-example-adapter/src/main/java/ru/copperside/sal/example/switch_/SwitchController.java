package ru.copperside.sal.example.switch_;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.copperside.sal.starter.watchdog.WatchDogService;

import java.util.Map;

/**
 * REST endpoint for runtime adapter switches (online/offline toggle, log settings).
 * <p>
 * C# origin: {@code SwitchConfig} / {@code SwitchMiddleware}.
 */
@RestController
@RequestMapping("/switch")
public class SwitchController {

    private final WatchDogService watchDogService;

    public SwitchController(WatchDogService watchDogService) {
        this.watchDogService = watchDogService;
    }

    @PostMapping("/{name}")
    public Map<String, Object> toggle(@PathVariable String name) {
        return switch (name.toLowerCase()) {
            case "online" -> {
                boolean newState = watchDogService.toggleOnline();
                yield Map.of("switch", "online", "value", newState);
            }
            default -> Map.of("error", "Unknown switch: " + name);
        };
    }
}
