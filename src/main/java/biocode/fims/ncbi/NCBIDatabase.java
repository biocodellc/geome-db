package biocode.fims.ncbi;

/**
 * enum representing the NCBI database names
 *
 * @author rjewing
 */
public enum NCBIDatabase {
    BIO_SAMPLE("biosample"), SRA("sra");

    private final String name;

    NCBIDatabase(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
