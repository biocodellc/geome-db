package biocode.fims.repositories;

import biocode.fims.rest.UserContext;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.context.request.RequestContextHolder;


/**
 * AOP Advice methods affecting postgres repositories
 *
 * @author rjewing
 */
@Aspect
public class PostgresRepositoryAuditAdvice {
    @Autowired(required = false)
    private UserContext userContext;
    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * when the @SetFimsUser annotation is present for a method,
     * this method executes immediately before the annotated method, setting the
     * psql variable fims.username to the current user
     */
    @Before("@annotation(SetFimsUser)")
    public void setFimsUser() {
        String user = DataActivityActor.current();
        if (user == null) user = "anon";
        // Explicit background attribution takes precedence; otherwise use the request identity.
        if (DataActivityActor.current() != null) {
            user = DataActivityActor.current();
        } else if (RequestContextHolder.getRequestAttributes() != null) {
            if (userContext != null && userContext.getUser() != null) {
                user = userContext.getUser().getUsername();
            }
        } else {
            user = "GeOMe-DB System";
        }

        jdbcTemplate.queryForObject("SELECT set_config('fims.username', :username, true)",
                new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("username", user), String.class);

    }
}
