package biocode.fims.tissues;

import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.records.Record;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author rjewing
 */
public class Plate {
    private static final Pattern WELL_PATTERN = Pattern.compile("^([a-z])([0-9]{1,2})$", Pattern.CASE_INSENSITIVE);

    private Map<PlateRow, Record[]> rows;

    public Plate() {
        this.rows = new LinkedHashMap<>();

        for (PlateRow row : PlateRow.values()) {
            rows.put(row, new Record[12]);
        }
    }

    @JsonAnyGetter
    public Map<PlateRow, Record[]> getRows() {
        return rows;
    }

    public void addRecord(String well, Record record) {
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
