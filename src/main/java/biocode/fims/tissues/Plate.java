package biocode.fims.tissues;

import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.records.GenericRecord;
import biocode.fims.records.Record;
import com.fasterxml.jackson.annotation.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author rjewing
 */
@JsonIgnoreProperties(value = {"name"}, ignoreUnknown = true)
public class Plate {
    private static final Pattern WELL_PATTERN = Pattern.compile("^([a-z])([0-9]{1,2})$", Pattern.CASE_INSENSITIVE);

    private Map<PlateRow, Record[]> rows;
    private String name;

    Plate() {
        this.rows = new LinkedHashMap<>();

        for (PlateRow row : PlateRow.values()) {
            rows.put(row, new Record[12]);
        }
    }

    public String name() {
        return name;
    }

    public void name(String name) {
        this.name = name;
    }

    @JsonAnyGetter
    public Map<PlateRow, Record[]> getRows() {
        return rows;
    }

    @SuppressWarnings({"unchecked"})
    @JsonAnySetter
    public void anySetter(String name, Object value) {
        PlateRow plateRow = PlateRow.fromString(name);

        Record[] row = new Record[12];
        int i = 0;
        for (Object props : (List) value) {
            if (props != null) {
                Map<String, Object> properties = new HashMap<>();
                for (Map.Entry<String, Object> e : ((Map<String, Object>) props).entrySet()) {
                    properties.put(e.getKey(), e.getValue());

                }
                row[i] = new GenericRecord(properties);
            }
            i++;
        }

        this.rows.put(plateRow, row);
    }

    private void addRecord(String well, Record record) {
        Matcher matcher = WELL_PATTERN.matcher(well);

        if (!matcher.matches()) {
            throw new FimsRuntimeException(PlateCode.INVALID_WELL, 400, well);
        }

        PlateRow row = PlateRow.fromString(matcher.group(1));
        int col = Integer.parseInt(matcher.group(2)) - 1;

        if (row == null || col > 11 || col < 0) {
            throw new FimsRuntimeException(PlateCode.INVALID_PLATE, 400, well);
        }

        if (rows.get(row)[col] != null) {
            throw new FimsRuntimeException(PlateCode.INVALID_PLATE, 400, well);
        }

        rows.get(row)[col] = record;
    }

    public static Plate fromRecords(String wellColumn, List<Record> tissues) {
        Plate plate = new Plate();

        tissues.forEach(t -> plate.addRecord(t.get(wellColumn), t));

        return plate;
    }
}
