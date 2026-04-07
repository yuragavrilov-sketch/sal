package ru.copperside.sal.example.ping;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.copperside.sal.starter.SalProperties;
import ru.copperside.sal.starter.web.AdapterState;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PingController.class,
        excludeAutoConfiguration = ru.copperside.sal.starter.web.WebAutoConfiguration.class)
class PingControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    SalProperties salProperties;

    @MockitoBean
    AdapterState adapterState;

    @Test
    void pingReturnsAdapterNameAndOnlineState() throws Exception {
        SalProperties.Adapter adapter = new SalProperties.Adapter();
        adapter.setName("test-adapter");
        when(salProperties.getAdapter()).thenReturn(adapter);
        when(adapterState.isOnline()).thenReturn(true);

        mockMvc.perform(get("/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipientServiceName").value("test-adapter"))
                .andExpect(jsonPath("$.online").value(true))
                .andExpect(jsonPath("$.serverTime").exists());
    }
}
