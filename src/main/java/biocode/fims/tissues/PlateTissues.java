package biocode.fims.tissues;

import biocode.fims.application.config.GeomeProperties;
import biocode.fims.config.models.Entity;
import biocode.fims.models.Project;
import biocode.fims.records.GenericRecord;
import biocode.fims.records.Record;
import biocode.fims.records.RecordSet;
import org.apache.commons.collections.keyvalue.MultiKey;

import java.util.*;

/**
 * @author rjewing
 */
public class PlateTissues {
    private final Plate plate;
    private Project project;
    private Entity entity;
    private Entity parentEntity;
    private Set<String> parentIdentifiers;
    private Map<MultiKey, List<Record>> newTissues;
    private Map<MultiKey, List<Record>> existingTissues;

    public PlateTissues(Builder builder) {
        this.plate = builder.plate;
        this.project = builder.project;
        this.newTissues = builder.newTissues;
        this.existingTissues = builder.existingTissues;
        this.parentIdentifiers = builder.parentIdentifiers;
        this.entity = builder.entity;
        this.parentEntity = builder.parentEntity;

    }

    public Set<String> parentIdentifiers() {
        return parentIdentifiers;
    }

    public Map<MultiKey, List<Record>> newTissues() {
        return newTissues;
    }

    public Map<MultiKey, List<Record>> existingTissues() {
        return existingTissues;
    }

    public Project project() {
        return project;
    }

    public Entity entity() {
        return entity;
    }

    public Entity parentEntity() {
        return parentEntity;
    }

    public String name() {
        return plate.name();
    }

    /**
     * create RecordSets by expedition for all newTissues
     * <p>
     * generate a unique identifier for any new Tissues and group into RecordSets by expedition
     *
     * @param siblingTissues existing sibling tissues in the project, keyed with MultiKey(expeditionCode, parentIdentifier)
     */
    public Map<String, RecordSet> createRecordSets(Map<MultiKey, List<Record>> siblingTissues) {
        Map<String, RecordSet> recordSets = new HashMap<>();
        // generate a uniqueKey for each newTissue tissue in the plate
        for (Map.Entry<MultiKey, List<Record>> e : newTissues().entrySet()) {
            MultiKey k = e.getKey();

            String expeditionCode = (String) k.getKey(0);

            RecordSet recordSet = recordSets.computeIfAbsent(expeditionCode, key -> new RecordSet(entity, false));

            List<Record> siblings = siblingTissues.getOrDefault(k, new ArrayList<>());
            for (Record r : e.getValue()) {
                r = transformProperties(r);
                recordSet.add(r);
                if (!r.has(entity.getUniqueKeyURI())) {
                    r.set(entity.getUniqueKeyURI(), r.get(parentEntity.getUniqueKeyURI()) + "." + (siblings.size() + 1));
                }
                siblings.add(r);
            }
        }

        return recordSets;
    }

    /**
     * attempts to map any properties using the entity Attributes uri if possible
     *
     * @param r
     * @return new Record instance w/ mapped properties
     */
    private Record transformProperties(Record r) {
        Map<String, String> props = new HashMap<>();

        for (String key : r.properties().keySet()) {
            String uri = entity.getAttributeUri(key);
            if (uri == null) {
                props.put(key, r.get(key));
            } else {
                props.put(uri, r.get(key));
            }
        }

        return new GenericRecord(props, r.rootIdentifier(), r.projectId(), r.expeditionCode(), r.persist());
    }


    public static class Builder {
        private Set<String> parentIdentifiers;
        private Map<MultiKey, List<Record>> newTissues;
        private Map<MultiKey, List<Record>> existingTissues;
        private Entity entity;
        private Entity parentEntity;
        private GeomeProperties props;
        private Plate plate;
        private Project project;

        public Builder() {
            this.parentIdentifiers = new HashSet<>();
            this.newTissues = new HashMap<>();
            this.existingTissues = new HashMap<>();
        }

        public Builder plate(Plate plate) {
            this.plate = plate;
            return this;
        }

        public Builder project(Project project) {
            this.project = project;
            return this;
        }

        public Builder props(GeomeProperties props) {
            this.props = props;
            return this;
        }

        public Builder entity(Entity entity) {
            this.entity = entity;
            return this;
        }

        public Builder parentEntity(Entity parentEntity) {
            this.parentEntity = parentEntity;
            return this;
        }

        public PlateTissues build() {

            for (Map.Entry<PlateRow, Record[]> e : plate.getRows().entrySet()) {
                PlateRow row = e.getKey();

                int col = 1;
                for (Record r : e.getValue()) {
                    if (r != null) {
                        String parentIdentifier = r.get(parentEntity.getUniqueKey());
                        parentIdentifiers.add(parentIdentifier);

                        MultiKey k = new MultiKey(r.expeditionCode(), parentIdentifier);

                        // collect any records we need to create tissues for
                        // those that are missing the tissue entity uniqueKey
                        if (!r.has(entity.getUniqueKey())) {
                            r.set(props.tissuePlateUri(), plate.name());
                            r.set(props.tissueWellUri(), row.toString() + col);
                            newTissues.computeIfAbsent(k, key -> new ArrayList<>()).add(r);
                        } else {
                            existingTissues.computeIfAbsent(k, key -> new ArrayList<>()).add(r);
                        }
                    }
                    col++;
                }
            }
            return new PlateTissues(this);
        }
    }
}
