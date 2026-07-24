package biocode.fims.models;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UserTest {

    @Test
    public void users_with_same_username_different_case_should_be_equal() {
        User upper = new User.UserBuilder("Demo", "password")
                .email("demo@example.com")
                .institution("biocode")
                .name("Demo", "User")
                .build();
        User lower = new User.UserBuilder("demo", "password")
                .email("demo@example.com")
                .institution("biocode")
                .name("Demo", "User")
                .build();

        assertEquals(upper, lower);
        assertEquals(upper.hashCode(), lower.hashCode());
    }
}
