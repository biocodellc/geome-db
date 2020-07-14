package biocode.fims.fastq;

import biocode.fims.config.models.EntityProps;

/**
 * @author rjewing
 */
public enum FastqProps implements EntityProps {
    LIBRARY_STRATEGY("libraryStrategy"),
    LIBRARY_SOURCE("librarySource"),
    LIBRARY_SELECTION("librarySelection"),
    LIBRARY_LAYOUT("libraryLayout"),
    PLATFORM("platform"),
    INSTRUMENT_MODEL("instrumentModel"),
    DESIGN_DESCRIPTION("designDescription"),
    FILENAMES("filenames"),
    IDENTIFIER("identifier"),
    BIOSAMPLE("bioSample"); // TODO should we mark this attribute as hidden or none editable

    private final String val;

    FastqProps(String val) {
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
