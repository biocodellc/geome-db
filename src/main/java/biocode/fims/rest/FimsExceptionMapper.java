package biocode.fims.rest;

import biocode.fims.fimsExceptions.FimsAbstractException;
import biocode.fims.utils.ErrorInfo;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * class to catch an exception thrown from a rest service and map the necessary information to a request
 */
@Provider
public class FimsExceptionMapper implements ExceptionMapper<Exception> {
    @Context
    protected HttpServletRequest request;
    @Context
    protected ExtendedUriInfo uriInfo;
    @Context
    protected HttpHeaders httpHeaders;

    protected static Logger logger = LoggerFactory.getLogger(FimsExceptionMapper.class);

    @Override
    public Response toResponse(Exception e) {
        if (e instanceof NotFoundException) return ((NotFoundException) e).getResponse();

        logException(e);
        ErrorInfo errorInfo = getErrorInfo(e);

        return Response.status(errorInfo.getHttpStatusCode())
                .entity(errorInfo.toJSON())
                .type(MediaType.APPLICATION_JSON)
                .build();

    }

    // method to set the relevant information in ErrorInfo
    protected ErrorInfo getErrorInfo(Exception e) {
        String usrMessage;
        String developerMessage = null;
        Integer httpStatusCode = getHttpStatus(e);

        if (e instanceof FimsAbstractException) {
            usrMessage = ((FimsAbstractException) e).getUsrMessage();
            developerMessage = ((FimsAbstractException) e).getDeveloperMessage();
            if (developerMessage == null || developerMessage.trim().isEmpty()) {
                developerMessage = describeThrowable(e.getCause());
            }
        } else if (e instanceof WebApplicationException) {
            Status status = Status.fromStatusCode(httpStatusCode);
            usrMessage = status == null ? "Request Error" : status.getReasonPhrase();

            if (httpStatusCode == 415) {
                developerMessage = buildUnsupportedMediaTypeDeveloperMessage();
            }
        } else {
            usrMessage = "Server Error";
        }

        return new ErrorInfo(usrMessage, developerMessage, httpStatusCode, e);

    }

    protected Integer getHttpStatus(Exception e) {
        // if the throwable is an instance of WebApplicationException, get the status code
        if (e instanceof WebApplicationException) {
            return ((WebApplicationException) e).getResponse().getStatus();
        } else if (e instanceof FimsAbstractException) {
            return ((FimsAbstractException) e).getHttpStatusCode();
        } else {
            return Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        }
    }

    protected void logException(Exception e) {
        Integer status = e instanceof FimsAbstractException ? ((FimsAbstractException) e).getHttpStatusCode() : 0;

        // Don't log BadRequestExceptions (400), Unauthorized (401), or Forbidden (403) exceptions
        if (status == 400 || status == 403 || status == 401) return;

        // Log the exception with more details
        logger.error("Exception thrown: {}\nMessage: {}\nStack Trace:",
                     e.getClass().getName(),
                     e.getMessage(),
                     e);
    }

    private String buildUnsupportedMediaTypeDeveloperMessage() {
        String method = request != null ? request.getMethod() : "<unknown>";
        String path = request != null ? request.getRequestURI() : "<unknown>";
        String contentType = request != null ? request.getContentType() : null;
        String received = contentType == null ? "<missing>" : contentType;

        String expectedTypes = "application/json";
        if ("PUT".equalsIgnoreCase(method) && path != null && path.matches(".*/photos/[^/]+/upload/?$")) {
            expectedTypes = "application/zip, application/octet-stream, application/x-zip-compressed, application/x-zip";
        }

        return "Unsupported Content-Type \"" + received + "\" for " + method + " " + path +
                ". Expected one of: " + expectedTypes + ".";
    }

    private String describeThrowable(Throwable t) {
        if (t == null) return null;

        String msg = t.getMessage();
        if (msg == null || msg.trim().isEmpty()) {
            return t.getClass().getName();
        }

        return t.getClass().getName() + ": " + msg;
    }

}
