package biocode.fims.rest.services;

import biocode.fims.application.config.*;
import biocode.fims.config.project.ProjectConfig;
import biocode.fims.fimsExceptions.ForbiddenRequestException;
import biocode.fims.models.Project;
import biocode.fims.models.User;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.rest.UserContext;
import biocode.fims.service.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import java.util.Collections;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ProjectActivityAccessTest {
 private ProjectService projects;
 private RecordRepository records;
 private Project project;
 private User user;
 private TestController controller;
 private static class TestController extends ProjectController {
  TestController(ProjectService projects, RecordRepository records, GeomeSql sql) {
   super(mock(ExpeditionService.class),mock(FimsProperties.class),projects,sql,records,mock(NetworkService.class),mock(GeomeProperties.class));
   userContext = new UserContext();
  }
  void login(User user) { userContext.setUser(user); }
 }
 @Before public void setup() {
  projects=mock(ProjectService.class); records=mock(RecordRepository.class); project=mock(Project.class); user=mock(User.class);
  ProjectConfig config=mock(ProjectConfig.class); GeomeSql sql=mock(GeomeSql.class);
  when(projects.getProject(1)).thenReturn(project); when(project.isPublic()).thenReturn(true);
  when(project.getProjectConfig()).thenReturn(config); when(config.entities()).thenReturn(new java.util.LinkedList<>());
  when(sql.expeditionStats()).thenReturn("SELECT ${includePrivate} WHERE project_id=:projectId");
  when(sql.singleExpeditionStats()).thenReturn("SELECT ${includePrivate} WHERE project_id=:projectId AND code=:expeditionCode");
  controller=new TestController(projects,records,sql);
 }
 private String requestedSql() {
  ArgumentCaptor<String> captured=ArgumentCaptor.forClass(String.class);
  verify(records).query(captured.capture(),any(SqlParameterSource.class),any(RowMapper.class));
  return captured.getValue();
 }
 @Test public void anonymousPublicViewCannotIncludeMemberActivity() {
  controller.expeditionStats(1,null); assertTrue(requestedSql().contains("SELECT false"));
 }
 @Test public void unrelatedLoggedInUserCannotIncludeMemberActivity() {
  controller.login(user); controller.expeditionStats(1,null); assertTrue(requestedSql().contains("SELECT false"));
 }
 @Test public void memberCanSeeMemberExpeditionActivity() {
  controller.login(user); when(projects.isUserMemberOfProject(user,1)).thenReturn(true);
  controller.expeditionStats(1,null); assertTrue(requestedSql().contains("SELECT true"));
 }
 @Test public void projectOwnerCanSeeMemberExpeditionActivity() {
  controller.login(user); when(projects.isProjectAdmin(user,1)).thenReturn(true);
  controller.expeditionStats(1,"PRIVATE"); assertTrue(requestedSql().contains("SELECT true"));
 }
 @Test public void privateProjectRejectsNonmembersBeforeQuerying() {
  when(project.isPublic()).thenReturn(false);
  try { controller.expeditionStats(1,null); fail("Expected forbidden"); } catch (ForbiddenRequestException expected) {}
  verifyZeroInteractions(records); verify(project,never()).getProjectConfig();
 }
}
