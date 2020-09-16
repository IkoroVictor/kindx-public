package io.kindx.gateway.handler.error;

import io.kindx.exception.NotFoundException;
import io.kindx.gateway.dto.ErrorDto;
import io.kindx.gateway.exception.ConflictException;
import io.kindx.gateway.exception.InvalidRequestException;
import io.kindx.gateway.exception.MethodNotAllowedException;
import io.kindx.util.IDUtil;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ResponseExceptionHandler {
    private static final Logger logger = LogManager.getLogger(ResponseExceptionHandler.class);

    public ErrorDto handleException(Exception ex) {
        ErrorDto.ErrorDtoBuilder builder = ErrorDto.builder();
        if (ex instanceof MethodNotAllowedException) {
            builder.code(HttpStatus.SC_METHOD_NOT_ALLOWED)
                    .message(ex.getMessage());
        } else if (ex instanceof NotFoundException) {
            builder.code(HttpStatus.SC_NOT_FOUND)
                    .message(ex.getMessage());
        } else if (ex instanceof ConflictException) {
            builder.code(HttpStatus.SC_CONFLICT)
                    .message(ex.getMessage());
        } else if (ex instanceof InvalidRequestException) {
            builder.code(HttpStatus.SC_BAD_REQUEST)
                    .message(ex.getMessage());
        } else {
            String logId = IDUtil.generateLogId();
            logger.error("LogId: [{}], {}", logId,  ex.getMessage(), ex);
            builder.code(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                    .message("An internal error occurred. LogID: " + logId);
        }
        return builder.build();
    }
}
