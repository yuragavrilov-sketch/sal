package ru.copperside.sal.example.ping;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.copperside.sal.api.dto.service.PingResponse;
import ru.copperside.sal.starter.SalProperties;
import ru.copperside.sal.starter.web.AdapterState;

import java.time.OffsetDateTime;

/**
 * Health/ping endpoint — allows other adapters to check this adapter's availability.
 * <p>
 * C# origin: {@code PingAP} (IActionProviderAsync&lt;PingRequest, PingResponse&gt;)
 */
@RestController
public class PingController {

    private final SalProperties properties;
    private final AdapterState adapterState;

    public PingController(SalProperties properties, AdapterState adapterState) {
        this.properties = properties;
        this.adapterState = adapterState;
    }

    @GetMapping("/ping")
    public PingResponse ping() {
        PingResponse response = new PingResponse();
        response.setOnline(adapterState.isOnline());
        response.setServerTime(OffsetDateTime.now());
        response.setRecipientServiceName(properties.getAdapter().getName());
        return response;
    }
}
