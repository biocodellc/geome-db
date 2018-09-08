package biocode.fims.tissues;

/**
 * @author rjewing
 */
public enum PlateRow {
    A, B, C, D, E, F, G, H;

    public static PlateRow fromString(String key) {
        return key == null
                ? null
                : PlateRow.valueOf(key.toUpperCase());
    }
}
