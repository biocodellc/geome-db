package biocode.fims.rest;

import com.fasterxml.jackson.databind.node.ObjectNode;
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
import static org.junit.Assert.assertTrue;

/**
 * @author rjewing
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {BiscicolFimsRestTestAppConfig.class})
@Sql(scripts = "classpath:test_data.sql")
@Transactional
public class V1_REST_SERVICES_TEST {
    /**
     * inject the WebTarget instance by spring for test.
     * here no any dependencies on Jersey, just jax-rs api
     */
    @Inject
    public WebTarget target;

    @Before
    public void setup() {
        // logout
        target.path("authenticationService/logout").request().get();
    }

    @Test
    public void GET_projects_projectId_expeditions_expeditionCode() {

        // Un-Authenticated should return 200 for public project
        Response res = target.path("projects/1/expeditions/PROJ1_EXP1").request().get();
        assertEquals(200, res.getStatus());
        assertEquals("{\"expeditionId\":1,\"expeditionCode\":\"PROJ1_EXP1\",\"expeditionTitle\":\"project 1 expedition 1\",\"ts\":\"2017-04-26 15:08:14\",\"project\":{\"projectId\":1,\"projectCode\":\"PROJ1\",\"projectTitle\":\"project 1\"},\"user\":{\"userId\":1,\"username\":\"demo\",\"projectAdmin\":false},\"expeditionBcid\":{\"ezidRequest\":true,\"identifier\":\"ark:/99999/a2\",\"resourceType\":\"http://purl.org/dc/dcmitype/Collection\",\"subResourceType\":null,\"ts\":\"2017-04-26 15:08:14\"},\"entityBcids\":[{\"ezidRequest\":true,\"identifier\":\"ark:/99999/b2\",\"resourceType\":\"http://purl.org/dc/dcmitype/Event\",\"subResourceType\":null,\"ts\":\"2017-04-26 15:08:14\"}],\"public\":true}",
                res.readEntity(String.class));

        // Un-Authenticated request should return 403 for private project
        res = target.path("projects/2/expeditions/PROJ2_EXP1").request().get();
        assertEquals(403, res.getStatus());

        // login as non-admin user
        MultivaluedMap authDetails = new MultivaluedHashMap();
        authDetails.add("username", "demo2");
        authDetails.add("password", "d");
        Response authRes = target.path("/authenticationService/login").request().post(Entity.form(authDetails));

        // successful login
        assertEquals(200, authRes.getStatus());

        // project member can view expedition in private project
        res = target.path("projects/2/expeditions/PROJ2_EXP1").request().get();
        assertEquals(200, res.getStatus());
        assertEquals("{\"expeditionId\":3,\"expeditionCode\":\"PROJ2_EXP1\",\"expeditionTitle\":\"project 2 expedition 1\",\"ts\":\"2017-04-26 15:08:14\",\"project\":{\"projectId\":2,\"projectCode\":\"PROJ2\",\"projectTitle\":\"project 2\"},\"user\":{\"userId\":2,\"username\":\"demo2\",\"projectAdmin\":false},\"expeditionBcid\":{\"ezidRequest\":true,\"identifier\":\"ark:/99999/A2\",\"resourceType\":\"http://purl.org/dc/dcmitype/Collection\",\"subResourceType\":null,\"ts\":\"2017-04-26 15:08:14\"},\"entityBcids\":[],\"public\":true}",
                res.readEntity(String.class));

        // should return empty expedition if doesn't exist
        res = target.path("projects/2/expeditions/invalid_expedition_code").request().get();
        assertEquals(204, res.getStatus());

    }

    @Test
    public void GET_projects_list() {
        // Un-Authenticated should return no projects
        Response res = target.path("projects/list").request().get();
        assertEquals(200, res.getStatus());
        assertEquals("[]",
                res.readEntity(String.class));

        // Un-Authenticated with includePublic should return public projects
        res = target.path("projects/list")
                .queryParam("includePublic", "true")
                .request().get();
        assertEquals(200, res.getStatus());
        assertEquals("[{\"projectId\":\"1\",\"projectCode\":\"PROJ1\",\"projectTitle\":\"project 1\",\"validationXml\":\"\"},{\"projectId\":\"3\",\"projectCode\":\"PROJ3\",\"projectTitle\":\"project 3\",\"validationXml\":\"\"}]",
                res.readEntity(String.class));

        // login
        MultivaluedMap authDetails = new MultivaluedHashMap();
        authDetails.add("username", "demo3");
        authDetails.add("password", "d");
        Response authRes = target.path("/authenticationService/login").request().post(Entity.form(authDetails));

        // successful login
        assertEquals(200, authRes.getStatus());

        // should return only member projects
        res = target.path("projects/list").request().get();
        assertEquals(200, res.getStatus());
        assertEquals("[{\"projectId\":\"2\",\"projectCode\":\"PROJ2\",\"projectTitle\":\"project 2\",\"validationXml\":\"\"},{\"projectId\":\"3\",\"projectCode\":\"PROJ3\",\"projectTitle\":\"project 3\",\"validationXml\":\"\"},{\"projectId\":\"4\",\"projectCode\":\"PROJ4\",\"projectTitle\":\"project 4\",\"validationXml\":\"\"}]",
                res.readEntity(String.class));

        // should return member projects and public projects
        res = target.path("projects/list")
                .queryParam("includePublic", "true").request().get();
        assertEquals(200, res.getStatus());
        assertEquals("[{\"projectId\":\"1\",\"projectCode\":\"PROJ1\",\"projectTitle\":\"project 1\",\"validationXml\":\"\"},{\"projectId\":\"2\",\"projectCode\":\"PROJ2\",\"projectTitle\":\"project 2\",\"validationXml\":\"\"},{\"projectId\":\"3\",\"projectCode\":\"PROJ3\",\"projectTitle\":\"project 3\",\"validationXml\":\"\"},{\"projectId\":\"4\",\"projectCode\":\"PROJ4\",\"projectTitle\":\"project 4\",\"validationXml\":\"\"}]",
                res.readEntity(String.class));

    }

    @Test
    public void GET_projects_user_list() {
        // Un-Authenticated should return 401
        Response res = target.path("projects/user/list").request().get();
        assertEquals(401, res.getStatus());

        // login
        MultivaluedMap authDetails = new MultivaluedHashMap();
        authDetails.add("username", "demo3");
        authDetails.add("password", "d");
        Response authRes = target.path("/authenticationService/login").request().post(Entity.form(authDetails));

        // successful login
        assertEquals(200, authRes.getStatus());

        // should return only member projects
        res = target.path("projects/user/list").request().get();
        assertEquals(200, res.getStatus());
        assertEquals("[{\"projectId\":\"2\",\"projectCode\":\"PROJ2\",\"projectTitle\":\"project 2\",\"validationXml\":\"\"},{\"projectId\":\"3\",\"projectCode\":\"PROJ3\",\"projectTitle\":\"project 3\",\"validationXml\":\"\"},{\"projectId\":\"4\",\"projectCode\":\"PROJ4\",\"projectTitle\":\"project 4\",\"validationXml\":\"\"}]",
                res.readEntity(String.class));
    }

    @Test
    public void POST_bcids() {
        MultivaluedMap bcidForm = new MultivaluedHashMap();
        bcidForm.add("doi", "doi:test");
        bcidForm.add("webAddress", "http://example.com/resolver/?abc123");
        bcidForm.add("graph", "graphId");
        bcidForm.add("title", "a test bcid");
        bcidForm.add("resourceType", "http://purl.org/dc/dcmitype/Collection");

        Response res = target.path("bcids").request().post(Entity.form(bcidForm));
        assertEquals(401, res.getStatus());

        // login
        MultivaluedMap authDetails = new MultivaluedHashMap();
        authDetails.add("username", "demo");
        authDetails.add("password", "d");
        Response authRes = target.path("/authenticationService/login").request().post(Entity.form(authDetails));

        // successful login
        assertEquals(200, authRes.getStatus());

        res = target.path("bcids").request().post(Entity.form(bcidForm));
        assertEquals(200, res.getStatus());
        ObjectNode responseEntity = res.readEntity(ObjectNode.class);
        assertTrue(responseEntity.has("identifier"));
    }
}
