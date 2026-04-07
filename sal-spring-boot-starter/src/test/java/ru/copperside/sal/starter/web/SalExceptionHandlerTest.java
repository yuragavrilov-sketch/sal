package ru.copperside.sal.starter.web;

import org.junit.jupiter.api.Test;
import ru.copperside.sal.api.exception.InfrastructureExceptionDTO;
import ru.copperside.sal.api.exception.SalErrorCodes;
import ru.copperside.sal.api.exception.SalException;
import ru.copperside.sal.starter.SalProperties;

import static org.assertj.core.api.Assertions.assertThat;

class SalExceptionHandlerTest {

    private final SalProperties properties = new SalProperties();
    private final SalExceptionHandler handler = new SalExceptionHandler(properties);

    @Test
    void salExceptionReturns500WithDto() {
        SalException ex = SalException.error(SalErrorCodes.ADAPTER_IS_OFFLINE, "Adapter is offline");

        var response = handler.handleSalException(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        InfrastructureExceptionDTO body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getCode()).isEqualTo(SalErrorCodes.ADAPTER_IS_OFFLINE);
    }

    @Test
    void genericExceptionReturns500WithFatalCode() {
        var response = handler.handleGenericException(new RuntimeException("boom"));

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        InfrastructureExceptionDTO body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getCode()).isEqualTo(SalErrorCodes.FATAL_EXCEPTION);
        assertThat(body.getMessage()).isEqualTo("boom");
    }
}
