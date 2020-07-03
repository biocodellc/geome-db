package biocode.fims.ncbi.sra.submission;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class that contains general SraMetadataMapper funcitonality
 */
public abstract class AbstractSraMetadataMapper implements SraMetadataMapper {

    private static final List<String> SRA_HEADERS = new ArrayList<String>() {{
        add("sample_name");
        add("library_ID");
        add("title");
        add("library_strategy");
        add("library_source");
        add("library_selection");
        add("library_layout");
        add("platform");
        add("instrument_model");
        add("design_description");
        add("filetype");
        add("filename");
        add("filename2");
    }};

    @Override
    public List<String> getHeaderValues() {
        return SRA_HEADERS;
    }
}
