//package biocode.fims.photos;
//
//import biocode.fims.config.project.models.Validation;
//import biocode.fims.config.project.models.Mapping;
//import biocode.fims.photos.FimsPhotosFileManager;
//import biocode.fims.renderers.SheetMessages;
//import biocode.fims.renderers.SimpleMessage;
//import biocode.fims.run.ProcessController;
//import biocode.fims.settings.SettingsManager;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.node.ArrayNode;
//import com.fasterxml.jackson.databind.node.ObjectNode;
//import org.json.simple.JSONObject;
//import org.junit.Assert;
//import org.junit.Before;
//import org.junit.Test;
//import validation.SheetMessagesUtils;
//
//import java.io.File;
//import java.io.IOException;
//
//import static org.junit.Assert.*;
//
///**
// * @author rjewing
// */
//public class FimsPhotoFileManagerValidationTest {
//
//    private Mapping mapping;
//    private FimsPhotosFileManager fm;
//    private ProcessController pc;
//    private ClassLoader classLoader;
//
//    @Before
//    public void setUp() throws Exception {
//        classLoader = getClass().getClassLoader();
//    }
//
//    @Test
//    public void test_invalid_photos_dataset() {
//        File datasetFile = new File(classLoader.getResource("invalidPhotoDataset.csv").getFile());
//        init(datasetFile);
//
//        ArrayNode fimsDataset = new ObjectMapper().createArrayNode();
//        ObjectNode resource1 = fimsDataset.addObject();
//        resource1.put("eventId", "1");
//        resource1.put("principalInvestigator", "jack smith");
//        ObjectNode resource2 = fimsDataset.addObject();
//        resource2.put("eventId", "2");
//        resource2.put("principalInvestigator", "jack smith");
//        ObjectNode resource3 = fimsDataset.addObject();
//        resource3.put("eventId", "3");
//        resource3.put("principalInvestigator", "jack smith");
//
//        fm.validate(fimsDataset);
//
//        SheetMessages expected = getInvalidPhotosDatasetExpectedMessages();
//        JSONObject worksheetMessages = getValidationMessages();
//
//        Assert.assertEquals(SheetMessagesUtils.sheetMessagesToJSONObject(expected), worksheetMessages);
//    }
//
//    private SheetMessages getInvalidPhotosDatasetExpectedMessages() {
//        SheetMessages sheetMessages = new SheetMessages(mapping.getChildEntities().getFirst().getWorksheet());
//
//        // uniqueValue rule
//        sheetMessages.addErrorMessage("Unique value constraint did not pass",
//                new SimpleMessage("\"originalPhotoUrl\" column is defined as unique but some values used more than once: http://ftp.myserver.com/photos/expedition/1"));
//
//        // compositeUniqueValue rule
//        sheetMessages.addErrorMessage("Unique value constraint did not pass",
//                new SimpleMessage("(\"eventId\", \"photoId\") is defined as a composite unique key, but some value combinations were used more than once: (\"eventId\": \"2\", \"photoId\": \"2.1\")"));
//
//        // InvalidURL error rule
//        sheetMessages.addErrorMessage("Invalid URL",
//                new SimpleMessage("notAUrl is not a valid URL for \"originalPhotoUrl\""));
//
//        // RequiredColumns error rule
//        sheetMessages.addErrorMessage("Missing column(s)",
//                new SimpleMessage("\"eventId\" has a missing cell value"));
//
//        // RequiredColumns error rule
//        sheetMessages.addWarningMessage("Missing column(s)",
//                new SimpleMessage("\"photoNotes\" has a missing cell value"));
//
//        return sheetMessages;
//    }
//
//    @Test
//    public void test_valid_photos_dataset_empty_parent_dataset() {
//        File datasetFile = new File(classLoader.getResource("validPhotoDataset.csv").getFile());
//        init(datasetFile);
//
//        ArrayNode fimsDataset = new ObjectMapper().createArrayNode();
//
//        fm.validate(fimsDataset);
//
//        SheetMessages expected = new SheetMessages(mapping.getChildEntities().getFirst().getWorksheet());
//        expected.addErrorMessage("Spreadsheet check",
//                new SimpleMessage("No parent resources found."));
//
//        JSONObject worksheetMessages = getValidationMessages();
//
//        assertEquals(SheetMessagesUtils.sheetMessagesToJSONObject(expected), worksheetMessages);
//    }
//
//    @Test
//    public void test_empty_photos_dataset() {
//        File datasetFile = new File(classLoader.getResource("emptyPhotoDataset.csv").getFile());
//        init(datasetFile);
//
//        ArrayNode fimsDataset = new ObjectMapper().createArrayNode();
//
//        fm.validate(fimsDataset);
//
//        SheetMessages expected = new SheetMessages(mapping.getChildEntities().getFirst().getWorksheet());
//        expected.addErrorMessage("Initial Spreadsheet check",
//                new SimpleMessage("NO_DATA"));
//
//        JSONObject worksheetMessages = getValidationMessages();
//
//        assertEquals(SheetMessagesUtils.sheetMessagesToJSONObject(expected), worksheetMessages);
//    }
//
//    @Test
//    public void test_valid_photos_dataset_fails_when_no_parent_resources_match() {
//        File datasetFile = new File(classLoader.getResource("validPhotoDataset.csv").getFile());
//        init(datasetFile);
//
//        ArrayNode fimsDataset = new ObjectMapper().createArrayNode();
//        ObjectNode resource1 = fimsDataset.addObject();
//        resource1.put("eventId", "something");
//        resource1.put("principalInvestigator", "jack smith");
//
//        fm.validate(fimsDataset);
//
//        SheetMessages expected = new SheetMessages(mapping.getChildEntities().getFirst().getWorksheet());
//        expected.addErrorMessage("The following eventId's do not exist.",
//                new SimpleMessage("1, 2, 3, 3, 3"));
//
//        JSONObject worksheetMessages = getValidationMessages();
//
//        assertEquals(SheetMessagesUtils.sheetMessagesToJSONObject(expected), worksheetMessages);
//    }
//
//    @Test
//    public void test_valid_photos_dataset_warns_when_some_parent_resources_match() {
//        File datasetFile = new File(classLoader.getResource("validPhotoDataset.csv").getFile());
//        init(datasetFile);
//
//        ArrayNode fimsDataset = new ObjectMapper().createArrayNode();
//        ObjectNode resource1 = fimsDataset.addObject();
//        resource1.put("eventId", "3");
//        resource1.put("principalInvestigator", "jack smith");
//
//        fm.validate(fimsDataset);
//
//        SheetMessages expected = new SheetMessages(mapping.getChildEntities().getFirst().getWorksheet());
//        expected.addWarningMessage("The following eventId's do not exist.",
//                new SimpleMessage("1, 2"));
//
//        JSONObject worksheetMessages = getValidationMessages();
//
//        assertEquals(SheetMessagesUtils.sheetMessagesToJSONObject(expected), worksheetMessages);
//    }
//
//    @Test
//    public void test_valid_photos_dataset_has_no_messages_when_all_parent_resources_exist() {
//        File datasetFile = new File(classLoader.getResource("validPhotoDataset.csv").getFile());
//        init(datasetFile);
//
//        ArrayNode fimsDataset = new ObjectMapper().createArrayNode();
//        ObjectNode resource1 = fimsDataset.addObject();
//        resource1.put("eventId", "1");
//        resource1.put("principalInvestigator", "jack smith");
//        ObjectNode resource2 = fimsDataset.addObject();
//        resource2.put("eventId", "2");
//        resource2.put("principalInvestigator", "jack smith");
//        ObjectNode resource3 = fimsDataset.addObject();
//        resource3.put("eventId", "3");
//        resource3.put("principalInvestigator", "jack smith");
//
//        fm.validate(fimsDataset);
//
//        SheetMessages expected = new SheetMessages(mapping.getChildEntities().getFirst().getWorksheet());
//
//        JSONObject worksheetMessages = getValidationMessages();
//
//        assertEquals(SheetMessagesUtils.sheetMessagesToJSONObject(expected), worksheetMessages);
//    }
//
//    private JSONObject getValidationMessages() {
//        JSONObject worksheets = (JSONObject) pc.getMessages().get("worksheets");
//        return (JSONObject) worksheets.get(mapping.getChildEntities().getFirst().getWorksheet());
//    }
//
//    private void init(File datasetFile) {
//        File configFile = new File(classLoader.getResource("test.xml").getFile());
//
//        mapping = new Mapping();
//        mapping.addMappingRules(configFile);
//        Validation validation = new Validation();
//        validation.addValidationRules(configFile, mapping);
//
//        pc = new ProcessController(0, null);
//        pc.setMapping(mapping);
//        pc.setValidation(validation);
//
//        pc.setOutputFolder(System.getProperty("java.io.tmpdir"));
//
//        fm = new FimsPhotosFileManager(null, null, null, null, null);
//        try {
//            fm.setFilename(datasetFile.getCanonicalPath());
//        } catch (IOException e) {
//            e.printStackTrace();
//            fail(e.getMessage());
//        }
//        fm.setProcessController(pc);
//    }
//}
