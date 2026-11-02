\set ON_ERROR_STOP on
INSERT INTO users(username,password,email) VALUES ('owner','test','owner@example.test');
INSERT INTO networks(id,title,config,user_id) VALUES (1,'Test','{}',1);
INSERT INTO project_configurations(name,config,user_id,network_id) VALUES ('Test','{"entities":[]}',1,1);
INSERT INTO projects(project_title,config_id,user_id,network_id) VALUES ('Test',1,1,1);
INSERT INTO expeditions(project_id,expedition_code,expedition_title,identifier,visibility,user_id,public) VALUES (1,'PRIVATE','Member expedition','ark:/21547/test','project members',1,false);
CREATE SCHEMA network_1;
CREATE FUNCTION network_1.set_expedition_last_modified()
  RETURNS trigger AS $body$
DECLARE
  expedition_id int;
  affected_expeditions int[];
BEGIN
  IF TG_WHEN <> 'AFTER' THEN
    RAISE EXCEPTION 'set_expedition_last_modified() may only run as an AFTER trigger';
  END IF;

  IF TG_LEVEL = 'STATEMENT' THEN
    RAISE EXCEPTION 'set_expedition_last_modified() does not support being a STATEMENT trigger';
  END IF;

  IF TG_OP = 'DELETE' THEN
    affected_expeditions = ARRAY[OLD.expedition_id];
  ELSIF TG_OP = 'UPDATE' AND OLD.expedition_id IS DISTINCT FROM NEW.expedition_id THEN
    affected_expeditions = ARRAY[OLD.expedition_id, NEW.expedition_id];
  ELSE
    affected_expeditions = ARRAY[NEW.expedition_id];
  END IF;

  FOREACH expedition_id IN ARRAY affected_expeditions LOOP
    UPDATE public.expeditions SET modified = CURRENT_TIMESTAMP WHERE id = expedition_id;
  END LOOP;

  RETURN NULL;
END; $body$
  LANGUAGE plpgsql
  SECURITY DEFINER
  SET search_path = pg_catalog, public;

CREATE TABLE network_1.audit_table (
  event_id bigserial primary key,
  table_name text not null, -- table the change was made to
  user_name text, -- user who made the change
  ts TIMESTAMP WITH TIME ZONE NOT NULL, -- timestamp the change happened
  action TEXT NOT NULL CHECK (action IN ('I','D','U', 'T')), -- INSERT, DELETE, UPDATE, or TRUNCATE
  row_data jsonb, -- For INSERT this is the new row values. For DELETE and UPDATE it is the old row values.
  changed_fields jsonb -- Null except UPDATE events. This is the result of jsonb_diff_val(NEW data, OLD data)
);
CREATE TRIGGER set_data_activity AFTER INSERT ON network_1.audit_table FOR EACH ROW EXECUTE PROCEDURE public.set_data_activity_from_audit();

CREATE TABLE network_1.sample
  (
    id SERIAL PRIMARY KEY,
    local_identifier TEXT NOT NULL,
    expedition_id INT NOT NULL REFERENCES expeditions (id) ON DELETE CASCADE,
    data JSONB NOT NULL,
    tsv TSVECTOR,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
  );

CREATE TRIGGER tsvector_update BEFORE INSERT OR UPDATE ON network_1.sample FOR EACH ROW EXECUTE PROCEDURE entity_tsv_trigger(); CREATE TRIGGER update_modtime BEFORE UPDATE ON network_1.sample FOR EACH ROW EXECUTE PROCEDURE update_modified_column(); CREATE TRIGGER set_createdtime BEFORE INSERT ON network_1.sample FOR EACH ROW EXECUTE PROCEDURE set_created_column(); CREATE TRIGGER entity_audit AFTER INSERT OR DELETE OR UPDATE ON network_1.sample FOR EACH ROW EXECUTE PROCEDURE entity_history(); CREATE TRIGGER set_expedition_last_modified AFTER INSERT OR DELETE OR UPDATE ON network_1.sample FOR EACH ROW EXECUTE PROCEDURE network_1.set_expedition_last_modified();
CREATE INDEX idx_network_1_Sample_data ON network_1.sample USING GIN (data); CREATE INDEX idx_network_1_Sample_tsv ON network_1.sample USING GIN (tsv); ALTER TABLE network_1.sample
    ADD CONSTRAINT idx_network_1_Sample_local_identifier_expedition_id UNIQUE (local_identifier, expedition_id);

BEGIN;
SELECT set_config('fims.username','data-editor',true);
INSERT INTO network_1.sample(local_identifier,expedition_id,data) VALUES ('SAMPLE',1,'{"sample":"one"}');
COMMIT;
DO $$ BEGIN
 IF (SELECT latest_data_modification_username FROM projects WHERE id=1) <> 'data-editor' THEN RAISE EXCEPTION 'Fresh install did not track actor'; END IF;
 IF EXISTS (SELECT 1 FROM pg_trigger WHERE tgrelid='network_1.sample'::regclass AND tgname='set_project_last_modified') THEN RAISE EXCEPTION 'Old activity trigger reintroduced'; END IF;
END $$;
DO $test$ DECLARE ct integer; BEGIN
 SELECT count(*) INTO ct FROM (SELECT e.id AS "expeditionId", timezone('UTC', e.created) AS "created", timezone('UTC', e.latest_data_modification) AS "latestDataModification", e.latest_data_modification_username AS "latestDataModificationUsername", e.expedition_code AS "expeditionCode", e.expedition_title AS "expeditionTitle", e.identifier as "expeditionIdentifier", u.username AS "ownerUsername", u.id AS "ownerUserId", u.first_name as "ownerFirstName", u.last_name as "ownerLastName", u.email as "ownerEmail" , COALESCE(Sample.ct,0) as "SampleCount" FROM expeditions AS e LEFT JOIN users AS u on e.user_id = u.id LEFT JOIN (
  SELECT expedition_id, count(*) as ct FROM network_1.sample GROUP BY expedition_id
) AS Sample on Sample.expedition_id = e.id
 WHERE e.project_id = 1 AND (e.public = true OR e.public <> true) ORDER BY lower(e.expedition_code)
) stats;
 IF ct <> 1 THEN RAISE EXCEPTION 'Unexpected visibility count for expeditionStats true'; END IF;
 END $test$;
DO $test$ DECLARE actor text; BEGIN
 SELECT "latestDataModificationUsername" INTO actor FROM (SELECT e.id AS "expeditionId", timezone('UTC', e.created) AS "created", timezone('UTC', e.latest_data_modification) AS "latestDataModification", e.latest_data_modification_username AS "latestDataModificationUsername", e.expedition_code AS "expeditionCode", e.expedition_title AS "expeditionTitle", e.identifier as "expeditionIdentifier", u.username AS "ownerUsername", u.id AS "ownerUserId", u.first_name as "ownerFirstName", u.last_name as "ownerLastName", u.email as "ownerEmail" , COALESCE(Sample.ct,0) as "SampleCount" FROM expeditions AS e LEFT JOIN users AS u on e.user_id = u.id LEFT JOIN (
  SELECT expedition_id, count(*) as ct FROM network_1.sample GROUP BY expedition_id
) AS Sample on Sample.expedition_id = e.id
 WHERE e.project_id = 1 AND (e.public = true OR e.public <> true) ORDER BY lower(e.expedition_code)
) stats;
 IF actor <> 'data-editor' THEN RAISE EXCEPTION 'Statistics lost actor'; END IF;
 END $test$;
DO $test$ DECLARE ct integer; BEGIN
 SELECT count(*) INTO ct FROM (SELECT e.id AS "expeditionId", timezone('UTC', e.created) AS "created", timezone('UTC', e.latest_data_modification) AS "latestDataModification", e.latest_data_modification_username AS "latestDataModificationUsername", e.expedition_code AS "expeditionCode", e.expedition_title AS "expeditionTitle", e.identifier as "expeditionIdentifier", u.username AS "ownerUsername", u.id AS "ownerUserId" , COALESCE(Sample.ct,0) as "SampleCount" FROM expeditions AS e LEFT JOIN users AS u on e.user_id = u.id LEFT JOIN (
  SELECT expedition_id, count(*) as ct FROM network_1.sample GROUP BY expedition_id
) AS Sample on Sample.expedition_id = e.id
 WHERE e.project_id = 1 AND e.expedition_code = 'PRIVATE' AND (e.public = true OR e.public <> true) ORDER BY lower(e.expedition_code)
) stats;
 IF ct <> 1 THEN RAISE EXCEPTION 'Unexpected visibility count for singleExpeditionStats true'; END IF;
 END $test$;
DO $test$ DECLARE actor text; BEGIN
 SELECT "latestDataModificationUsername" INTO actor FROM (SELECT e.id AS "expeditionId", timezone('UTC', e.created) AS "created", timezone('UTC', e.latest_data_modification) AS "latestDataModification", e.latest_data_modification_username AS "latestDataModificationUsername", e.expedition_code AS "expeditionCode", e.expedition_title AS "expeditionTitle", e.identifier as "expeditionIdentifier", u.username AS "ownerUsername", u.id AS "ownerUserId" , COALESCE(Sample.ct,0) as "SampleCount" FROM expeditions AS e LEFT JOIN users AS u on e.user_id = u.id LEFT JOIN (
  SELECT expedition_id, count(*) as ct FROM network_1.sample GROUP BY expedition_id
) AS Sample on Sample.expedition_id = e.id
 WHERE e.project_id = 1 AND e.expedition_code = 'PRIVATE' AND (e.public = true OR e.public <> true) ORDER BY lower(e.expedition_code)
) stats;
 IF actor <> 'data-editor' THEN RAISE EXCEPTION 'Statistics lost actor'; END IF;
 END $test$;
DO $test$ DECLARE ct integer; BEGIN
 SELECT count(*) INTO ct FROM (SELECT e.id AS "expeditionId", timezone('UTC', e.created) AS "created", timezone('UTC', e.latest_data_modification) AS "latestDataModification", e.latest_data_modification_username AS "latestDataModificationUsername", e.expedition_code AS "expeditionCode", e.expedition_title AS "expeditionTitle", e.identifier as "expeditionIdentifier", u.username AS "ownerUsername", u.id AS "ownerUserId", u.first_name as "ownerFirstName", u.last_name as "ownerLastName", u.email as "ownerEmail" , COALESCE(Sample.ct,0) as "SampleCount" FROM expeditions AS e LEFT JOIN users AS u on e.user_id = u.id LEFT JOIN (
  SELECT expedition_id, count(*) as ct FROM network_1.sample GROUP BY expedition_id
) AS Sample on Sample.expedition_id = e.id
 WHERE e.project_id = 1 AND (e.public = true OR e.public <> false) ORDER BY lower(e.expedition_code)
) stats;
 IF ct <> 0 THEN RAISE EXCEPTION 'Unexpected visibility count for expeditionStats false'; END IF;
 END $test$;
DO $test$ DECLARE ct integer; BEGIN
 SELECT count(*) INTO ct FROM (SELECT e.id AS "expeditionId", timezone('UTC', e.created) AS "created", timezone('UTC', e.latest_data_modification) AS "latestDataModification", e.latest_data_modification_username AS "latestDataModificationUsername", e.expedition_code AS "expeditionCode", e.expedition_title AS "expeditionTitle", e.identifier as "expeditionIdentifier", u.username AS "ownerUsername", u.id AS "ownerUserId" , COALESCE(Sample.ct,0) as "SampleCount" FROM expeditions AS e LEFT JOIN users AS u on e.user_id = u.id LEFT JOIN (
  SELECT expedition_id, count(*) as ct FROM network_1.sample GROUP BY expedition_id
) AS Sample on Sample.expedition_id = e.id
 WHERE e.project_id = 1 AND e.expedition_code = 'PRIVATE' AND (e.public = true OR e.public <> false) ORDER BY lower(e.expedition_code)
) stats;
 IF ct <> 0 THEN RAISE EXCEPTION 'Unexpected visibility count for singleExpeditionStats false'; END IF;
 END $test$;
DO $test$ DECLARE actor text; ct integer; BEGIN
 SELECT "latestDataModificationUsername", "SampleCount" INTO actor,ct FROM (SELECT
  p.id AS "projectId", timezone('UTC', p.created) AS "created", p.latest_data_modification_username AS "latestDataModificationUsername", p.project_title AS "projectTitle", timezone('UTC', p.latest_data_modification) as "latestDataModification", p.description AS "description", p.public AS "public",
  p.discoverable AS "discoverable",
  p.principal_investigator AS "principalInvestigator",
  p.principal_investigator_affiliation AS "principalInvestigatorAffiliation",
  p.project_contact AS "projectContact",
  p.project_contact_email AS "projectContactEmail",
  p.publication_guid AS "publicationGuid",
  p.project_data_guid AS "projectDataGuid",
  p.recommended_citation AS "recommendedCitation",
  p.license AS "license",
  p.localcontexts_id AS "localcontextsId",
  p.permit_guid AS "permitGuid",
  pc.id AS "configId", pc.name AS "configName", pc.description AS "configDescription", pc.network_approved AS "configNetworkApproved",
  u.id AS "userId", u.username AS "username", u.email AS "email", u.first_name AS "firstName", u.last_name AS "lastName"
  , COALESCE(Sample.ct,0) as "SampleCount"
FROM projects AS p LEFT JOIN users AS u on p.user_id = u.id LEFT JOIN project_configurations AS pc on p.config_id = pc.id LEFT JOIN (
  SELECT project_id, count(*) as ct FROM network_1.sample LEFT JOIN expeditions e ON e.id = network_1.sample.expedition_id GROUP BY e.project_id
) AS Sample on Sample.project_id = p.id
 WHERE (p.discoverable = true OR p.public = true OR p.id in (SELECT project_id from user_projects AS up where up.user_id = 1)) ORDER BY lower(p.project_title)
) stats;
 IF actor <> 'data-editor' OR ct <> 1 THEN RAISE EXCEPTION 'Project statistics lost activity/count'; END IF;
 END $test$;
SELECT 'Fresh schema, network creation, activity and statistics queries passed' AS result;
