package biocode.fims.rest;

import biocode.fims.settings.SettingsManager;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.*;

/**
 * Filter which checks for the user in the session, if no user is present, the redirect to login page.
 * The login page is determined b first fetching the properties file name from the servlet-context init-param
 * (propsFilename). Then it looks in the props file for a loginPageUrl
 *
 */
public class SecureFilter extends biocode.fims.rest.filters.SecureFilter {
    private FilterConfig fc = null;

    @Override
    public void init (FilterConfig fc)
        throws ServletException {
        this.fc = fc;
        ApplicationContext ctx = WebApplicationContextUtils
                .getRequiredWebApplicationContext(fc.getServletContext());
        sm = ctx.getBean(SettingsManager.class);
        loginPageUrl = sm.retrieveValue("loginPageUrl");
    }
}
