package ru.copperside.sal.starter.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.copperside.sal.api.exception.ExceptionSourceType;
import ru.copperside.sal.api.exception.InfrastructureExceptionDTO;
import ru.copperside.sal.api.exception.SalException;
import ru.copperside.sal.api.exception.SalErrorCodes;
import ru.copperside.sal.starter.SalProperties;

import java.time.Instant;

/**
 * Translates SAL and generic exceptions into wire-format {@link InfrastructureExceptionDTO} responses.
 * <p>
 * C# origin: {@code ExceptionTransformerMiddleware}
 */
@RestControllerAdvice
public class SalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(SalExceptionHandler.class);

    private final SalProperties properties;

    public SalExceptionHandler(SalProperties properties) {
        this.properties = properties;
    }

    @ExceptionHandler(SalException.class)
    public ResponseEntity<InfrastructureExceptionDTO> handleSalException(SalException ex) {
        log.warn("SAL exception [{}]: {}", ex.getCode(), ex.getMessage());
        InfrastructureExceptionDTO dto = toDto(ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(dto);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<InfrastructureExceptionDTO> handleGenericException(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        InfrastructureExceptionDTO dto = new InfrastructureExceptionDTO();
        dto.setExceptionType("FatalException");
        dto.setCode(SalErrorCodes.FATAL_EXCEPTION);
        dto.setMessage(ex.getMessage());
        dto.setAdapterName(properties.getAdapter().getName());
        dto.setSourceType(ExceptionSourceType.UNKNOWN);
        dto.setTimeStamp(Instant.now());
        setInnerException(dto, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(dto);
    }

    private InfrastructureExceptionDTO toDto(SalException ex) {
        InfrastructureExceptionDTO dto = new InfrastructureExceptionDTO();
        dto.setExceptionType(ex.getExceptionTypeName());
        dto.setCode(ex.getCode());
        dto.setCodeDescription(ex.getCodeDescription());
        dto.setMessage(ex.getMessage());
        dto.setAdapterName(ex.getAdapterName() != null ? ex.getAdapterName() : properties.getAdapter().getName());
        dto.setSourceType(ex.getSourceType() != null ? ex.getSourceType() : ExceptionSourceType.UNKNOWN);
        dto.setSourcePath(ex.getSourcePath());
        dto.setSourceId(ex.getSourceId());
        dto.setSessionId(ex.getSessionId());
        dto.setTimeStamp(ex.getTimeStamp() != null ? ex.getTimeStamp() : Instant.now());
        dto.setProperties(ex.getProperties());
        setInnerException(dto, ex);
        return dto;
    }

    private void setInnerException(InfrastructureExceptionDTO dto, Throwable ex) {
        if (ex.getCause() != null) {
            InfrastructureExceptionDTO inner = new InfrastructureExceptionDTO();
            inner.setMessage(ex.getCause().getMessage());
            inner.setExceptionType(ex.getCause().getClass().getSimpleName());
            dto.setInnerException(inner);
        }
    }
}
