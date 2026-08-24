package br.com.itbn.sisdent.controller;

import br.com.itbn.sisdent.error.ErrorCode;
import br.com.itbn.sisdent.service.SchedulingConflictException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Locale;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RestExceptionTranslatorTest {
    private final ObjectMapper objectMapper = mock(ObjectMapper.class);
    private final RestExceptionTranslator translator = new RestExceptionTranslator(new StaticMessageSource(),
            objectMapper, new SimpleMeterRegistry());

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void mapsAuthenticationAuthorizationAndConflictsToPublicProblemCodes() {
        assertThat(translator.authenticationFailure(Locale.US).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(translator.authorization(Locale.US).getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(translator.databaseConflict(Locale.US).getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(translator.optimisticLock(Locale.US).getProperties())
                .containsEntry("code", ErrorCode.CONFLICT.value());
    }

    @Test
    void mapsLegacyResponseStatusesWithoutExposingTheirReasons() {
        assertThat(translator.responseStatus(new ResponseStatusException(HttpStatus.BAD_REQUEST, "private"), Locale.US)
                .getProperties()).containsEntry("code", ErrorCode.REQUEST_PARAMETER_INVALID.value());
        assertThat(translator.responseStatus(new ResponseStatusException(HttpStatus.NOT_FOUND, "private"), Locale.US)
                .getProperties()).containsEntry("code", ErrorCode.RESOURCE_NOT_FOUND.value());
        assertThat(translator.responseStatus(new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT, "private"), Locale.US)
                .getProperties()).containsEntry("code", ErrorCode.INTERNAL_ERROR.value());
    }

    @Test
    void recordsGraphQlTransportForKnownErrorsAndKeepsUnexpectedErrorsGeneric() {
        MDC.put("transport", "graphql");

        assertThat(translator.authorization(Locale.US).getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(translator.unexpected(new RuntimeException("private"), Locale.US).getProperties())
                .containsEntry("code", ErrorCode.INTERNAL_ERROR.value());
    }

    @Test
    void mapsMalformedAndUnmappedRequestsToTheirStableCodes() {
        assertThat(translator.malformedRequest(new RuntimeException(), Locale.US).getProperties())
                .containsEntry("code", ErrorCode.REQUEST_MALFORMED.value());
        assertThat(translator.invalidParameter(new RuntimeException(), Locale.US).getProperties())
                .containsEntry("code", ErrorCode.REQUEST_PARAMETER_INVALID.value());
        assertThat(translator.uploadTooLarge(new MaxUploadSizeExceededException(1024), Locale.US).getProperties())
                .containsEntry("code", ErrorCode.ACCOUNT_AVATAR_TOO_LARGE.value());
        assertThat(translator.malformedMultipart(new MultipartException("bad"), Locale.US).getProperties())
                .containsEntry("code", ErrorCode.REQUEST_MALFORMED.value());
        assertThat(translator.missingResource(Locale.US).getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(translator.unmappedMethod(Locale.US).getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void translatesApplicationValidationFailuresAndWritesSecurityResponses() throws Exception {
        assertThat(translator.scheduling(new SchedulingConflictException(), Locale.US).getStatus())
                .isEqualTo(HttpStatus.CONFLICT.value());

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        translator.authentication(request, response);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        verify(objectMapper).writeValue(any(java.io.Writer.class), any());
    }

    @Test
    void translatesBeanAndConstraintViolationsWithStableViolationPayloads() {
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(new Object(), "input");
        result.addError(new FieldError("input", "name", "required"));
        MethodArgumentNotValidException beanFailure = mock(MethodArgumentNotValidException.class);
        when(beanFailure.getBindingResult()).thenReturn(result);

        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("input.birthDate");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("invalid date");

        assertThat(translator.beanValidation(beanFailure, Locale.US).getProperties())
                .containsKey("violations");
        assertThat(translator.constraintValidation(new ConstraintViolationException(java.util.Set.of(violation)), Locale.US)
                .getProperties()).containsKey("violations");
    }
}
