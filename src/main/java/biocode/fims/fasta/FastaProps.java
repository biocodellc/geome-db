package biocode.fims.fasta;

import biocode.fims.config.models.EntityProps;

/**
 * @author rjewing
 */
public enum FastaProps implements EntityProps {
    SEQUENCE("sequence"),
    MARKER("marker"),
    IDENTIFIER("identifier");

    private final String val;

    FastaProps(String val) {
        this.val = val;
    }

    @Override
    public String uri() {
        return val;
    }

    @Override
    public String column() {
        return val;
    }

    @Override
    public String toString() {
        return val;
    }
}
