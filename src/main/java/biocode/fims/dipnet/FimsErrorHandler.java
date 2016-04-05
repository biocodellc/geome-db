package biocode.fims.dipnet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.Request;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;

public class FimsErrorHandler extends ErrorHandler {

    private static Logger logger = LoggerFactory.getLogger(FimsErrorHandler.class);
    private boolean _fistRequest = true;

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        // On 404 page we need to show index.html and let JS router do the work, otherwise show error page
        String redirectRoute = "/index.html";
        if ((response.getStatus() == HttpServletResponse.SC_NOT_FOUND) && _fistRequest) {
            RequestDispatcher dispatcher = request.getRequestDispatcher(redirectRoute);
            if (dispatcher != null) {
                try {
                    // reset response
                    response.reset();
                    // On second 404 request we need to show original 404 page, otherwise will be redirect loop
                    _fistRequest = false;
                    dispatcher.forward(request, response);
                } catch (ServletException e) {
                    super.handle(target, baseRequest, request, response);
                }
            }
        } else if ((response.getStatus() == HttpServletResponse.SC_NOT_FOUND) && ! _fistRequest) {
            logger.error("Can not find internal redirect route " + redirectRoute + " on 404 error. Will show system 404 page");
        } else {
            super.handle(target, baseRequest, request, response);
        }
    }

    @Override
    protected void writeErrorPageBody(HttpServletRequest request, Writer writer, int code, String message, boolean showStacks) throws IOException
    {
        writeErrorPageMessage(request, writer, code, message, request.getRequestURI());
    }

    @Override
    protected void writeErrorPageMessage(HttpServletRequest request, Writer writer, int code, String message, String uri)
            throws IOException
    {
        String statusMessage = Integer.toString(code) + " " + message;
        logger.error("Problem accessing " + uri + ". " + statusMessage);
        writer.write(statusMessage);
    }
}
