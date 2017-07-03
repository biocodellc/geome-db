package biocode.fims.run;

import biocode.fims.application.config.BiscicolAppConfig;
import biocode.fims.entities.User;
import biocode.fims.service.UserService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.UUID;

/**
 * Created by rjewing on 9/6/16.
 */
public class UserUUIDGenerator {
    public static void main(String[] args) throws Exception {
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(BiscicolAppConfig.class);
        UserService userService = applicationContext.getBean(UserService.class);

        for (User u: userService.getUsers()) {
            u.setUUID(UUID.randomUUID());
            userService.update(u);
        }
    }
}
