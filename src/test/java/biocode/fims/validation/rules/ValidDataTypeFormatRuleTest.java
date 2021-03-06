package biocode.fims.validation.rules;

import biocode.fims.config.models.Attribute;
import biocode.fims.config.models.DataType;
import biocode.fims.config.models.Entity;
import biocode.fims.records.GenericRecord;
import biocode.fims.records.Record;
import biocode.fims.records.RecordSet;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.validation.messages.Message;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class ValidDataTypeFormatRuleTest extends AbstractRuleTest {

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_exception_for_null_recordSet() {
        Rule rule = new ValidDataTypeFormatRule();
        assertTrue(rule.run(null, messages));
    }

    @Test
    public void should_be_valid_for_empty_recordSet() {
        Rule rule = new ValidDataTypeFormatRule();
        assertTrue(rule.run(new RecordSet(entity(), false), messages));
    }

    @Test
    public void should_not_be_valid_for_any_invalid_integer_formats() {
        Rule rule = new ValidDataTypeFormatRule();
        RecordSet recordSet = getIntegerDataTypeRecordSet();

        assertFalse(rule.run(recordSet, messages));
        assertTrue(rule.hasError());

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addErrorMessage(
                "Invalid DataFormat",
                new Message("\"col1\" contains non-integer value \".1\"")
        );

        assertEquals(expectedMessages, messages);
    }

    @Test
    public void should_allow_unknown_integer_value() {
        Rule rule = new ValidDataTypeFormatRule();
        RecordSet recordSet = getUnknownIntegerDataTypeRecordSet();

        assertFalse(rule.run(recordSet, messages));
        assertTrue(rule.hasError());

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addErrorMessage(
                "Invalid DataFormat",
                new Message("\"col1\" contains non-integer value \".1\". Value can also be \"Unknown\"")
        );

        assertEquals(expectedMessages, messages);
    }

    @Test
    public void should_not_be_valid_for_any_invalid_float_formats() {
        Rule rule = new ValidDataTypeFormatRule();
        RecordSet recordSet = getFloatDataTypeRecordSet();

        assertFalse(rule.run(recordSet, messages));
        assertTrue(rule.hasError());

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addErrorMessage(
                "Invalid DataFormat",
                new Message("\"col2\" contains non-float value \"test\"")
        );

        assertEquals(expectedMessages, messages);
    }

    @Test
    public void should_allow_unknown_float_value() {
        Rule rule = new ValidDataTypeFormatRule();
        RecordSet recordSet = getUnknownFloatDataTypeRecordSet();

        assertFalse(rule.run(recordSet, messages));
        assertTrue(rule.hasError());

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addErrorMessage(
                "Invalid DataFormat",
                new Message("\"col2\" contains non-float value \"test\". Value can also be \"Unknown\"")
        );

        assertEquals(expectedMessages, messages);
    }

    @Test
    public void should_not_be_valid_for_any_invalid_DATETIME_formats() {
        Rule rule = new ValidDataTypeFormatRule();
        RecordSet recordSet = getDateTimeDataTypeRecordSet();

        assertFalse(rule.run(recordSet, messages));
        assertTrue(rule.hasError());

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addErrorMessage(
                "Invalid DataFormat",
                new Message("\"col3\" contains invalid date value \"May 1984\". Format must be one of " +
                        "[hh:mm MM-yyyy, MM-YYYY]. If this is an Excel workbook, the value can also be an Excel DATE cell")
        );
        expectedMessages.addErrorMessage(
                "Invalid DataFormat",
                new Message("\"col3\" contains invalid date value \"2001-12-30T15:05:12\". Format must be one of " +
                        "[hh:mm MM-yyyy, MM-YYYY]. If this is an Excel workbook, the value can also be an Excel DATE cell")
        );

        assertEquals(expectedMessages, messages);
    }

    @Test
    public void should_allow_unknown_DATETIME_value() {
        Rule rule = new ValidDataTypeFormatRule();
        RecordSet recordSet = getUnknownDateTimeDataTypeRecordSet();

        assertFalse(rule.run(recordSet, messages));
        assertTrue(rule.hasError());

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addErrorMessage(
                "Invalid DataFormat",
                new Message("\"col3\" contains invalid date value \"May 1984\". Format must be one of " +
                        "[hh:mm MM-yyyy, MM-YYYY]. If this is an Excel workbook, the value can also be an Excel DATE cell. " +
                        "Value can also be \"Unknown\"")
        );

        assertEquals(expectedMessages, messages);
    }

    @Test
    public void should_not_be_valid_for_any_invalid_DATE_formats() {
        Rule rule = new ValidDataTypeFormatRule();
        RecordSet recordSet = getDateDataTypeRecordSet();

        assertFalse(rule.run(recordSet, messages));
        assertTrue(rule.hasError());

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addErrorMessage(
                "Invalid DataFormat",
                new Message("\"col4\" contains invalid date value \"12-16-2017\". Format must be one of " +
                        "[MM-dd]. If this is an Excel workbook, the value can also be an Excel DATE cell")
        );
        expectedMessages.addErrorMessage(
                "Invalid DataFormat",
                new Message("\"col4\" contains invalid date value \"string\". Format must be one of " +
                        "[MM-dd]. If this is an Excel workbook, the value can also be an Excel DATE cell")
        );

        assertEquals(expectedMessages, messages);
    }

    @Test
    public void should_not_be_valid_for_any_invalid_TIME_formats() {
        Rule rule = new ValidDataTypeFormatRule();
        RecordSet recordSet = getTimeDataTypeRecordSet();

        assertFalse(rule.run(recordSet, messages));
        assertTrue(rule.hasError());

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addErrorMessage(
                "Invalid DataFormat",
                new Message("\"col5\" contains invalid date value \"01:00:00\". Format must be one of " +
                        "[HH:mm]. If this is an Excel workbook, the value can also be an Excel DATE cell")
        );
        expectedMessages.addErrorMessage(
                "Invalid DataFormat",
                new Message("\"col5\" contains invalid date value \"1 o'clock\". Format must be one of " +
                        "[HH:mm]. If this is an Excel workbook, the value can also be an Excel DATE cell")
        );

        assertEquals(expectedMessages, messages);
    }

    private RecordSet getIntegerDataTypeRecordSet() {
        RecordSet recordSet = new RecordSet(entity(), false);

        Record r = new GenericRecord();
        r.set("urn:col1", "1");
        recordSet.add(r);

        Record r2 = new GenericRecord();
        r2.set("urn:col1", "-1");
        recordSet.add(r2);

        Record r3 = new GenericRecord();
        r3.set("urn:col1", "+1");
        recordSet.add(r3);

        Record r4 = new GenericRecord();
        r4.set("urn:col1", ".1");
        recordSet.add(r4);
        return recordSet;
    }

    private RecordSet getUnknownIntegerDataTypeRecordSet() {
        RecordSet recordSet = new RecordSet(entity(), false);
        recordSet.entity().getAttribute("col1").setAllowUnknown(true);

        Record r = new GenericRecord();
        r.set("urn:col1", "1");
        recordSet.add(r);

        Record r2 = new GenericRecord();
        r2.set("urn:col1", ".1");
        recordSet.add(r2);

        Record r3 = new GenericRecord();
        r3.set("urn:col1", "UNknoWn");
        recordSet.add(r3);

        return recordSet;
    }

    private RecordSet getFloatDataTypeRecordSet() {
        RecordSet recordSet = new RecordSet(entity(), false);

        Record r = new GenericRecord();
        r.set("urn:col2", "test");
        recordSet.add(r);

        Record r2 = new GenericRecord();
        r2.set("urn:col2", "10.11");
        recordSet.add(r2);

        Record r3 = new GenericRecord();
        r3.set("urn:col2", "-10.00009");
        recordSet.add(r3);

        Record r4 = new GenericRecord();
        r4.set("urn:col2", "+10.0000");
        recordSet.add(r4);

        Record r5 = new GenericRecord();
        r5.set("urn:col2", "10");
        recordSet.add(r5);

        return recordSet;
    }


    private RecordSet getUnknownFloatDataTypeRecordSet() {
        RecordSet recordSet = new RecordSet(entity(), false);
        recordSet.entity().getAttribute("col2").setAllowUnknown(true);

        Record r = new GenericRecord();
        r.set("urn:col2", "test");
        recordSet.add(r);

        Record r2 = new GenericRecord();
        r2.set("urn:col2", "-11.0");
        recordSet.add(r2);

        Record r3 = new GenericRecord();
        r3.set("urn:col2", "UNknoWn");
        recordSet.add(r3);

        return recordSet;
    }

    private RecordSet getDateTimeDataTypeRecordSet() {
        RecordSet recordSet = new RecordSet(entity(), false);

        Record r = new GenericRecord();
        r.set("urn:col3", "02:12 12-1984");
        recordSet.add(r);

        Record r2 = new GenericRecord();
        r2.set("urn:col3", "May 1984");
        recordSet.add(r2);

        Record r3 = new GenericRecord();
        r3.set("urn:col3", "2001-12-30T15:05:12.000");
        recordSet.add(r3);

        Record r4 = new GenericRecord();
        r4.set("urn:col3", "2001-12-30T15:05:12");
        recordSet.add(r4);

        return recordSet;
    }


    private RecordSet getUnknownDateTimeDataTypeRecordSet() {
        RecordSet recordSet = new RecordSet(entity(), false);
        recordSet.entity().getAttribute("col3").setAllowUnknown(true);

        Record r = new GenericRecord();
        r.set("urn:col3", "02:12 12-1984");
        recordSet.add(r);

        Record r2 = new GenericRecord();
        r2.set("urn:col3", "May 1984");
        recordSet.add(r2);

        Record r3 = new GenericRecord();
        r3.set("urn:col3", "UNknoWn");
        recordSet.add(r3);

        return recordSet;
    }

    private RecordSet getDateDataTypeRecordSet() {
        RecordSet recordSet = new RecordSet(entity(), false);

        Record r = new GenericRecord();
        r.set("urn:col4", "12-16");
        recordSet.add(r);

        Record r2 = new GenericRecord();
        r2.set("urn:col4", "2017-01-17");
        recordSet.add(r2);

        Record r3 = new GenericRecord();
        r3.set("urn:col4", "12-16-2017");
        recordSet.add(r3);

        Record r4 = new GenericRecord();
        r4.set("urn:col4", "string");
        recordSet.add(r4);

        return recordSet;
    }

    private RecordSet getTimeDataTypeRecordSet() {
        RecordSet recordSet = new RecordSet(entity(), false);

        Record r1 = new GenericRecord();
        r1.set("urn:col5", "00:00");
        recordSet.add(r1);

        Record r2 = new GenericRecord();
        r2.set("urn:col5", "01:00:00");
        recordSet.add(r2);

        Record r3 = new GenericRecord();
        r3.set("urn:col5", "01:11:01.000");
        recordSet.add(r3);

        Record r4 = new GenericRecord();
        r4.set("urn:col5", "1 o'clock");
        recordSet.add(r4);

        return recordSet;
    }

    @Override
    protected Entity entity() {
        Entity e = super.entity();

        e.getAttribute("col1").setDataType(DataType.INTEGER);
        e.getAttribute("col2").setDataType(DataType.FLOAT);

        Attribute a1 = new Attribute("col3", "urn:col3");
        a1.setDataType(DataType.DATETIME);
        a1.setDataFormat("hh:mm MM-yyyy, MM-YYYY");
        e.addAttribute(a1);

        Attribute a2 = new Attribute("col4", "urn:col4");
        a2.setDataType(DataType.DATE);
        a2.setDataFormat("MM-dd");
        e.addAttribute(a2);

        Attribute a3 = new Attribute("col5", "urn:col5");
        a3.setDataType(DataType.TIME);
        a3.setDataFormat("HH:mm");
        e.addAttribute(a3);

        return e;
    }
}