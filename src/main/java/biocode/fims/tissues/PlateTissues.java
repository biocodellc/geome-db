package biocode.fims.tissues;

import biocode.fims.config.models.Entity;
import biocode.fims.models.Project;
import biocode.fims.records.GenericRecord;
import biocode.fims.records.Record;
import biocode.fims.records.RecordSet;
import org.apache.commons.collections.keyvalue.MultiKey;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static biocode.fims.service.PlateService.TISSUE_PLATE_URI;
import static biocode.fims.service.PlateService.TISSUE_WELL_URI;

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

    private PlateTissues(Builder builder) {
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

            List<String> siblings = siblingTissues.getOrDefault(k, new ArrayList<>()).stream()
                    .map(r -> r.get(entity.getUniqueKeyURI()))
                    .collect(Collectors.toList());

            // we get the max here so we don't create duplicates if a tissue has been deleted
            // if id is of form parentIdentifier.[0-9] we parse the digit and use that as the
            // max if it is > then siblings.size();
            int max = siblings.size();

            // don't use uniqueKeyUri here b/c we haven't called transformProperties yet
            Pattern p = Pattern.compile(e.getValue().get(0).get(parentEntity.getUniqueKey()) + "\\.(\\d+)");
            for (String s: siblings) {
                Matcher matcher = p.matcher(s);
                if (matcher.matches()) {
                    Integer i = Integer.parseInt(matcher.group(1));
                    if (i > max) max = i;
                }
            }

            for (Record r : e.getValue()) {
                r = transformProperties(r);
                recordSet.add(r);
                if (!r.has(entity.getUniqueKeyURI())) {
                    r.set(entity.getUniqueKeyURI(), r.get(parentEntity.getUniqueKeyURI()) + "." + ++max);
                }
                siblings.add(r.get(entity.getUniqueKeyURI()));
                if (siblings.size() > max) max += 1;
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
        Map<String, Object> props = new HashMap<>();

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
                            r.set(TISSUE_PLATE_URI, plate.name());
                            if (col < 10) {
                                r.set(TISSUE_WELL_URI, row.toString() + ("0" + col));
                            } else {
                                r.set(TISSUE_WELL_URI, row.toString() + col);
                            }
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
