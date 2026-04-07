package ru.copperside.sal.example.ping;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.copperside.sal.api.dto.service.PingResponse;
import ru.copperside.sal.starter.SalProperties;

import java.time.OffsetDateTime;

@RestController
public class PingController {

    private final SalProperties properties;

    public PingController(SalProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/ping")
    public PingResponse ping() {
        PingResponse response = new PingResponse();
        response.setOnline(true);
        response.setServerTime(OffsetDateTime.now());
        response.setRecipientServiceName(properties.getAdapter().getName());
        response.setSalVersion(properties.getService().getSalVersion());
        return response;
    }
}
