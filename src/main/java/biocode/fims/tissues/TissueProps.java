package biocode.fims.tissues;

import biocode.fims.config.models.EntityProps;

/**
 * @author rjewing
 */
public enum TissueProps implements EntityProps {
    IDENTIFIER("tissueID", "urn:tissueID"),
    PLATE("tissuePlate", "urn:plateID"),
    WELL("tissueWell", "urn:wellID");

    private final String column;
    private final String uri;

    TissueProps(String column, String uri) {
        this.column = column;
        this.uri = uri;
    }

    @Override
    public String uri() {
        return uri;
    }

    @Override
    public String column() {
        return column;
    }

    @Override
    public String toString() {
        return uri;
    }
}
