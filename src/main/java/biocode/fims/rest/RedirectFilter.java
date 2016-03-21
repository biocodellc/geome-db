package biocode.fims.rest;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * redirect requests to the new webapp root
 */
public class RedirectFilter implements Filter {

    @Override
    public void init(FilterConfig config) throws ServletException {
        //
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws ServletException, IOException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String requestURI = request.getRequestURI();

        if (requestURI.matches("^/biocode-fims/\\w*.(html|jsp)$") || requestURI.matches("^/biocode-fims?/$")) {
            String newURI = requestURI.substring(requestURI.lastIndexOf("/"), requestURI.length());
            response.setStatus(302);
            response.sendRedirect(newURI);
        } else {
            chain.doFilter(req, res);
        }
    }

    @Override
    public void destroy() {
        //
    }
}
