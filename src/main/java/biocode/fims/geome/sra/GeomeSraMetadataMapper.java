//package biocode.fims.geome.sra;
//
//import biocode.fims.exceptions.SraCode;
//import biocode.fims.fastq.FastqMetadata;
//import biocode.fims.fastq.fileManagers.FastqFileManager;
//import biocode.fims.fimsExceptions.FimsRuntimeException;
//import biocode.fims.ncbi.sra.submission.AbstractSraMetadataMapper;
//import biocode.fims.rest.SpringObjectMapper;
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.node.ArrayNode;
//import com.fasterxml.jackson.databind.node.ObjectNode;
//import org.apache.commons.lang.StringUtils;
//
//import java.util.ArrayList;
//import java.util.Iterator;
//import java.util.List;
//
///**
// * Class that maps geome project attributes to sra metadata
// */
//public class GeomeSraMetadataMapper extends AbstractSraMetadataMapper {
//
//    private final Iterator<JsonNode> resourcesIt;
//
//    public GeomeSraMetadataMapper(ArrayNode resources) {
//
//        if (resources == null || resources.size() == 0) {
//            throw new FimsRuntimeException(SraCode.MISSING_DATASET, 400);
//        }
//
//        this.resourcesIt = resources.iterator();
//    }
//
//    @Override
//    public boolean hasNextResource() {
//        return resourcesIt.hasNext();
//    }
//
//    @Override
//    public List<String> getResourceMetadata() {
//        ObjectMapper objectMapper = new SpringObjectMapper();
//        ObjectNode sample = (ObjectNode) resourcesIt.next();
//        List<String> metadata = new ArrayList<>();
//
//        FastqMetadata fastqMetadata = null;
//        if (sample.has(FastqFileManager.CONCEPT_ALIAS)) {
//            try {
//                fastqMetadata = objectMapper.treeToValue(sample.get(FastqFileManager.CONCEPT_ALIAS), FastqMetadata.class);
//            } catch (JsonProcessingException e) {
//                throw new FimsRuntimeException(SraCode.SRA_FILES_FAILED, "failed to deserialize fastqMetadata object", 500);
//            }
//        }
//
//        if (fastqMetadata != null) {
//            String sampleId = sample.get("materialSampleID").asText();
//
//            String title;
//            String species = sample.get("species").asText();
//            String genus = sample.get("genus").asText();
//
//            if (!StringUtils.isBlank(genus)) {
//                title = fastqMetadata.getLibraryStrategy() + "_" + genus;
//                if (!StringUtils.isBlank(species)) {
//                    title += "_" + species;
//                }
//            } else {
//                title = fastqMetadata.getLibraryStrategy() + "_" + sample.get("phylum").asText();
//            }
//
//            metadata.add(sampleId);
//            metadata.add(sampleId);
//            metadata.add(title);
//            metadata.add(fastqMetadata.getLibraryStrategy());
//            metadata.add(fastqMetadata.getLibrarySource());
//            metadata.add(fastqMetadata.getLibrarySelection());
//            metadata.add(fastqMetadata.getLibraryLayout());
//            metadata.add(fastqMetadata.getPlatform());
//            metadata.add(fastqMetadata.getInstrumentModel());
//            metadata.add(fastqMetadata.getDesignDescription());
//            metadata.add("fastq");
//            metadata.add(fastqMetadata.getFilenames().get(0));
//
//            if (StringUtils.equalsIgnoreCase(fastqMetadata.getLibraryLayout(), FastqMetadata.PAIRED_LAYOUT)) {
//                metadata.add(fastqMetadata.getFilenames().get(1));
//            } else {
//                metadata.add("");
//            }
//        }
//
//        return metadata;
//    }
//}
