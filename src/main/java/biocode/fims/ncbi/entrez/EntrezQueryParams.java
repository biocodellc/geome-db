package biocode.fims.ncbi.entrez;

/**
 * @author rjewing
 */
public enum EntrezQueryParams {
    RETRIEVAL_START("retstart"), RETRIEVAL_MAX("retmax"), RETRIEVAL_MODE("retmode"), DB("db"), TERM("term"), API_KEY("api_key");

    private final String name;

    EntrezQueryParams(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
