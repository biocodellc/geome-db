package biocode.fims.rest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;

/**
 * @author rjewing
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {BiscicolFimsRestTestAppConfig.class})
@Sql(scripts = "classpath:test_data.sql")
@Transactional
public class V1_1_REST_SERVICES_TEST {
    /**
     * inject the WebTarget instance by spring for test.
     * here no any dependencies on Jersey, just jax-rs api
     */
    @Inject
    public WebTarget target;

    @Before
    public void setup() {
        // logout
        target.path("v1.1/authenticationService/logout").request().get();
    }

    @Test
    public void GET_projects_projectId_expeditions_expeditionCode() {

        // Un-Authenticated should return 200 for public project
        Response res = target.path("v1.1/projects/1/expeditions/PROJ1_EXP1").request().get();
        assertEquals(200, res.getStatus());
        assertEquals("{\"expeditionId\":1,\"expeditionCode\":\"PROJ1_EXP1\",\"expeditionTitle\":\"project 1 expedition 1\",\"ts\":\"2017-04-26 15:08:14\",\"project\":{\"projectId\":1,\"projectCode\":\"PROJ1\",\"projectTitle\":\"project 1\"},\"user\":{\"userId\":1,\"username\":\"demo\",\"projectAdmin\":false},\"expeditionBcid\":{\"ezidRequest\":true,\"identifier\":\"ark:/99999/a2\",\"resourceType\":\"http://purl.org/dc/dcmitype/Collection\",\"subResourceType\":null,\"ts\":\"2017-04-26 15:08:14\"},\"entityBcids\":[{\"ezidRequest\":true,\"identifier\":\"ark:/99999/b2\",\"resourceType\":\"http://purl.org/dc/dcmitype/Event\",\"subResourceType\":null,\"ts\":\"2017-04-26 15:08:14\"}],\"public\":true}",
                res.readEntity(String.class));

        // Un-Authenticated request should return 403 for private project
        res = target.path("v1.1/projects/2/expeditions/PROJ2_EXP1").request().get();
        assertEquals(403, res.getStatus());

        // login as non-admin user
        MultivaluedMap authDetails = new MultivaluedHashMap();
        authDetails.add("username", "demo2");
        authDetails.add("password", "d");
        Response authRes = target.path("v1.1/authenticationService/login").request().post(Entity.form(authDetails));

        // successful login
        assertEquals(200, authRes.getStatus());

        // project member can view expedition in private project
        res = target.path("v1.1/projects/2/expeditions/PROJ2_EXP1").request().get();
        assertEquals(200, res.getStatus());
        assertEquals("{\"expeditionId\":3,\"expeditionCode\":\"PROJ2_EXP1\",\"expeditionTitle\":\"project 2 expedition 1\",\"ts\":\"2017-04-26 15:08:14\",\"project\":{\"projectId\":2,\"projectCode\":\"PROJ2\",\"projectTitle\":\"project 2\"},\"user\":{\"userId\":2,\"username\":\"demo2\",\"projectAdmin\":false},\"expeditionBcid\":{\"ezidRequest\":true,\"identifier\":\"ark:/99999/A2\",\"resourceType\":\"http://purl.org/dc/dcmitype/Collection\",\"subResourceType\":null,\"ts\":\"2017-04-26 15:08:14\"},\"entityBcids\":[],\"public\":true}",
                res.readEntity(String.class));

        // should return empty expedition if doesn't exist
        res = target.path("v1.1/projects/2/expeditions/invalid_expedition_code").request().get();
        assertEquals(204, res.getStatus());

    }

    @Test
    public void GET_projects_user_list() {
        // Un-Authenticated should return 401
        Response res = target.path("v1.1/projects/user/list").request().get();
        assertEquals(401, res.getStatus());

        // login
        MultivaluedMap authDetails = new MultivaluedHashMap();
        authDetails.add("username", "demo3");
        authDetails.add("password", "d");
        Response authRes = target.path("v1.1/authenticationService/login").request().post(Entity.form(authDetails));

        // successful login
        assertEquals(200, authRes.getStatus());

        // should return only member projects
        res = target.path("v1.1/projects/user/list").request().get();
        assertEquals(200, res.getStatus());
        assertEquals("[{\"projectId\":2,\"projectCode\":\"PROJ2\",\"projectTitle\":\"project 2\",\"projectAbstract\":\"\",\"ts\":\"2017-04-26 15:08:14\",\"validationXml\":\"\",\"user\":{\"userId\":2,\"username\":\"demo2\",\"projectAdmin\":false},\"public\":false},{\"projectId\":3,\"projectCode\":\"PROJ3\",\"projectTitle\":\"project 3\",\"projectAbstract\":\"\",\"ts\":\"2017-04-26 15:08:14\",\"validationXml\":\"\",\"user\":{\"userId\":3,\"username\":\"demo3\",\"projectAdmin\":false},\"public\":true},{\"projectId\":4,\"projectCode\":\"PROJ4\",\"projectTitle\":\"project 4\",\"projectAbstract\":\"\",\"ts\":\"2017-04-26 15:08:14\",\"validationXml\":\"\",\"user\":{\"userId\":1,\"username\":\"demo\",\"projectAdmin\":false},\"public\":false}]",
                res.readEntity(String.class));
    }

    @Test
    public void GET_projects_projectId_expeditions() {
        // Un-Authenticated and private project should return 403
        Response res = target.path("v1.1/projects/2/expeditions").request().get();
        assertEquals(403, res.getStatus());

        // Un-Authenticated and public project should return expeditions
        res = target.path("v1.1/projects/1/expeditions").request().get();
        assertEquals(200, res.getStatus());
        assertEquals("[{\"expeditionId\":1,\"expeditionCode\":\"PROJ1_EXP1\",\"expeditionTitle\":\"project 1 expedition 1\",\"ts\":\"2017-04-26 15:08:14\",\"project\":{\"projectId\":1,\"projectCode\":\"PROJ1\",\"projectTitle\":\"project 1\"},\"user\":{\"userId\":1,\"username\":\"demo\",\"projectAdmin\":false},\"expeditionBcid\":null,\"entityBcids\":null,\"public\":true},{\"expeditionId\":2,\"expeditionCode\":\"PROJ1_EXP2\",\"expeditionTitle\":\"project 1 expedition 2\",\"ts\":\"2017-04-26 15:08:14\",\"project\":{\"projectId\":1,\"projectCode\":\"PROJ1\",\"projectTitle\":\"project 1\"},\"user\":{\"userId\":2,\"username\":\"demo2\",\"projectAdmin\":false},\"expeditionBcid\":null,\"entityBcids\":null,\"public\":true}]",
                res.readEntity(String.class));

        // Un-Authenticated and admin query param should return 403
        res = target.path("v1.1/projects/2/expeditions")
                .queryParam("admin", "true")
                .request().get();
        assertEquals(403, res.getStatus());

        // Un-Authenticated and user query param should return no expeditions
        res = target.path("v1.1/projects/1/expeditions")
                .queryParam("user", "true")
                .request().get();
        assertEquals(200, res.getStatus());
        assertEquals("[]", res.readEntity(String.class));

        // login
        MultivaluedMap authDetails = new MultivaluedHashMap();
        authDetails.add("username", "demo4");
        authDetails.add("password", "d");
        Response authRes = target.path("v1.1/authenticationService/login").request().post(Entity.form(authDetails));

        // successful login
        assertEquals(200, authRes.getStatus());

        // Authenticated and non-member of private project should return 403
        res = target.path("v1.1/projects/2/expeditions").request().get();
        assertEquals(403, res.getStatus());

        // login
        authDetails = new MultivaluedHashMap();
        authDetails.add("username", "demo");
        authDetails.add("password", "d");
        authRes = target.path("v1.1/authenticationService/login").request().post(Entity.form(authDetails));

        // successful login
        assertEquals(200, authRes.getStatus());

        // Authenticated and member of private project should return public expeditions
        res = target.path("v1.1/projects/4/expeditions").request().get();
        assertEquals(200, res.getStatus());
        assertEquals("[{\"expeditionId\":4,\"expeditionCode\":\"PROJ4_EXP1\",\"expeditionTitle\":\"project 4 expedition 1\",\"ts\":\"2017-04-26 15:08:14\",\"project\":{\"projectId\":4,\"projectCode\":\"PROJ4\",\"projectTitle\":\"project 4\"},\"user\":{\"userId\":3,\"username\":\"demo3\",\"projectAdmin\":false},\"expeditionBcid\":null,\"entityBcids\":null,\"public\":true},{\"expeditionId\":7,\"expeditionCode\":\"PROJ4_EXP4\",\"expeditionTitle\":\"project 4 expedition 4\",\"ts\":\"2017-04-26 15:08:14\",\"project\":{\"projectId\":4,\"projectCode\":\"PROJ4\",\"projectTitle\":\"project 4\"},\"user\":{\"userId\":1,\"username\":\"demo\",\"projectAdmin\":false},\"expeditionBcid\":null,\"entityBcids\":null,\"public\":true}]",
                res.readEntity(String.class));

        // Authenticated and admin query param should return 403 for non project admin
        res = target.path("v1.1/projects/2/expeditions")
                .queryParam("admin", "true")
                .request().get();
        assertEquals(403, res.getStatus());

        // Authenticated and admin query param should return all expeditions for project admin
        res = target.path("v1.1/projects/4/expeditions")
                .queryParam("admin", "true")
                .request().get();
        assertEquals(200, res.getStatus());
        assertEquals("[{\"expeditionId\":4,\"expeditionCode\":\"PROJ4_EXP1\",\"expeditionTitle\":\"project 4 expedition 1\",\"ts\":\"2017-04-26 15:08:14\",\"project\":{\"projectId\":4,\"projectCode\":\"PROJ4\",\"projectTitle\":\"project 4\"},\"user\":{\"userId\":3,\"username\":\"demo3\",\"projectAdmin\":false},\"expeditionBcid\":null,\"entityBcids\":null,\"public\":true},{\"expeditionId\":5,\"expeditionCode\":\"PROJ4_EXP2\",\"expeditionTitle\":\"project 4 expedition 2\",\"ts\":\"2017-04-26 15:08:14\",\"project\":{\"projectId\":4,\"projectCode\":\"PROJ4\",\"projectTitle\":\"project 4\"},\"user\":{\"userId\":3,\"username\":\"demo3\",\"projectAdmin\":false},\"expeditionBcid\":null,\"entityBcids\":null,\"public\":false},{\"expeditionId\":6,\"expeditionCode\":\"PROJ4_EXP3\",\"expeditionTitle\":\"project 4 expedition 3\",\"ts\":\"2017-04-26 15:08:14\",\"project\":{\"projectId\":4,\"projectCode\":\"PROJ4\",\"projectTitle\":\"project 4\"},\"user\":{\"userId\":1,\"username\":\"demo\",\"projectAdmin\":false},\"expeditionBcid\":null,\"entityBcids\":null,\"public\":false},{\"expeditionId\":7,\"expeditionCode\":\"PROJ4_EXP4\",\"expeditionTitle\":\"project 4 expedition 4\",\"ts\":\"2017-04-26 15:08:14\",\"project\":{\"projectId\":4,\"projectCode\":\"PROJ4\",\"projectTitle\":\"project 4\"},\"user\":{\"userId\":1,\"username\":\"demo\",\"projectAdmin\":false},\"expeditionBcid\":null,\"entityBcids\":null,\"public\":true}]",
                res.readEntity(String.class));

        // Authenticated and user query param should return users public expeditions
        res = target.path("v1.1/projects/4/expeditions")
                .queryParam("user", "true")
                .request().get();
        assertEquals(200, res.getStatus());
        assertEquals("[{\"expeditionId\":7,\"expeditionCode\":\"PROJ4_EXP4\",\"expeditionTitle\":\"project 4 expedition 4\",\"ts\":\"2017-04-26 15:08:14\",\"project\":{\"projectId\":4,\"projectCode\":\"PROJ4\",\"projectTitle\":\"project 4\"},\"user\":{\"userId\":1,\"username\":\"demo\",\"projectAdmin\":false},\"expeditionBcid\":null,\"entityBcids\":null,\"public\":true}]",
                res.readEntity(String.class));

        // Authenticated with user and includePrivate query params should return users public and private expeditions
        res = target.path("v1.1/projects/4/expeditions")
                .queryParam("user", "true")
                .queryParam("includePrivate", "true")
                .request().get();
        assertEquals(200, res.getStatus());
        assertEquals("[{\"expeditionId\":6,\"expeditionCode\":\"PROJ4_EXP3\",\"expeditionTitle\":\"project 4 expedition 3\",\"ts\":\"2017-04-26 15:08:14\",\"project\":{\"projectId\":4,\"projectCode\":\"PROJ4\",\"projectTitle\":\"project 4\"},\"user\":{\"userId\":1,\"username\":\"demo\",\"projectAdmin\":false},\"expeditionBcid\":null,\"entityBcids\":null,\"public\":false},{\"expeditionId\":7,\"expeditionCode\":\"PROJ4_EXP4\",\"expeditionTitle\":\"project 4 expedition 4\",\"ts\":\"2017-04-26 15:08:14\",\"project\":{\"projectId\":4,\"projectCode\":\"PROJ4\",\"projectTitle\":\"project 4\"},\"user\":{\"userId\":1,\"username\":\"demo\",\"projectAdmin\":false},\"expeditionBcid\":null,\"entityBcids\":null,\"public\":true}]",
                res.readEntity(String.class));

        // Authenticated and includePrivate queryparam and member of private project should return public expeditions and user's private expeditions
        res = target.path("v1.1/projects/4/expeditions")
                .queryParam("includePrivate", "true")
                .request().get();
        assertEquals(200, res.getStatus());
        assertEquals("[{\"expeditionId\":4,\"expeditionCode\":\"PROJ4_EXP1\",\"expeditionTitle\":\"project 4 expedition 1\",\"ts\":\"2017-04-26 15:08:14\",\"project\":{\"projectId\":4,\"projectCode\":\"PROJ4\",\"projectTitle\":\"project 4\"},\"user\":{\"userId\":3,\"username\":\"demo3\",\"projectAdmin\":false},\"expeditionBcid\":null,\"entityBcids\":null,\"public\":true},{\"expeditionId\":6,\"expeditionCode\":\"PROJ4_EXP3\",\"expeditionTitle\":\"project 4 expedition 3\",\"ts\":\"2017-04-26 15:08:14\",\"project\":{\"projectId\":4,\"projectCode\":\"PROJ4\",\"projectTitle\":\"project 4\"},\"user\":{\"userId\":1,\"username\":\"demo\",\"projectAdmin\":false},\"expeditionBcid\":null,\"entityBcids\":null,\"public\":false},{\"expeditionId\":7,\"expeditionCode\":\"PROJ4_EXP4\",\"expeditionTitle\":\"project 4 expedition 4\",\"ts\":\"2017-04-26 15:08:14\",\"project\":{\"projectId\":4,\"projectCode\":\"PROJ4\",\"projectTitle\":\"project 4\"},\"user\":{\"userId\":1,\"username\":\"demo\",\"projectAdmin\":false},\"expeditionBcid\":null,\"entityBcids\":null,\"public\":true}]",
                res.readEntity(String.class));
    }
}
