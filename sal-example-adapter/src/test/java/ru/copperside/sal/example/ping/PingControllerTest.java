package ru.copperside.sal.example.ping;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.copperside.sal.starter.SalProperties;

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

    @Test
    void pingReturnsAdapterName() throws Exception {
        SalProperties.Adapter adapter = new SalProperties.Adapter();
        adapter.setName("test-adapter");
        SalProperties.Service service = new SalProperties.Service();
        when(salProperties.getAdapter()).thenReturn(adapter);
        when(salProperties.getService()).thenReturn(service);

        mockMvc.perform(get("/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipientServiceName").value("test-adapter"))
                .andExpect(jsonPath("$.online").value(true))
                .andExpect(jsonPath("$.serverTime").exists());
    }
}
