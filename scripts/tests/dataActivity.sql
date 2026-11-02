\set ON_ERROR_STOP on
-- Isolated fixture database only. Run from scripts/tests using psql -f dataActivity.sql.
CREATE TABLE public.projects(id integer PRIMARY KEY, latest_data_modification timestamp, modified timestamp DEFAULT now(), title text);
CREATE TABLE public.expeditions(id integer PRIMARY KEY, project_id integer REFERENCES public.projects(id), modified timestamp DEFAULT now(), title text);
CREATE SCHEMA network_1;
CREATE TABLE network_1.audit_table(event_id bigserial PRIMARY KEY, table_name text, user_name text, ts timestamptz, action text, row_data jsonb, changed_fields jsonb);
CREATE TABLE network_1.sample(id integer PRIMARY KEY, expedition_id integer REFERENCES public.expeditions(id) ON DELETE CASCADE, data jsonb, modified timestamp DEFAULT now());
INSERT INTO public.projects VALUES (1, NULL, now(), 'One'), (2, NULL, now(), 'Two'), (3, '2040-01-01', now(), 'Preserved legacy activity');
INSERT INTO public.expeditions VALUES (10,1,now(),'A'), (11,2,now(),'B'), (12,1,now(),'Deleted data'), (13,3,now(),'Legacy');
-- Historical activity includes equal timestamps, deleted data, and a move to another project.
INSERT INTO network_1.audit_table(table_name,user_name,ts,action,row_data,changed_fields) VALUES
 ('network_1.sample','older','2020-01-01Z','I','{"expedition_id":10}',NULL),
 ('network_1.sample','last-editor','2020-01-02Z','U','{"expedition_id":10}','{"expedition_id":11}'),
 ('network_1.sample','delete-user','2020-01-03Z','D','{"expedition_id":12}',NULL),
 ('network_1.sample','legacy','2020-01-01Z','I','{"expedition_id":13}',NULL);
\ir ../updateDataActivity.sql
DO $$ BEGIN
 IF (SELECT latest_data_modification_username FROM public.expeditions WHERE id=10) <> 'last-editor' THEN RAISE EXCEPTION 'old expedition missing move activity'; END IF;
 IF (SELECT latest_data_modification_username FROM public.expeditions WHERE id=11) <> 'last-editor' THEN RAISE EXCEPTION 'destination missing move activity'; END IF;
 IF (SELECT latest_data_modification_username FROM public.expeditions WHERE id=12) <> 'delete-user' THEN RAISE EXCEPTION 'deleted data backfill missing'; END IF;
 IF (SELECT latest_data_modification_username FROM public.projects WHERE id=3) IS NOT NULL THEN RAISE EXCEPTION 'invented legacy actor'; END IF;
 IF (SELECT latest_data_modification FROM public.projects WHERE id=3) <> '2040-01-01' THEN RAISE EXCEPTION 'regressed legacy timestamp'; END IF;
END $$;
\ir ../updateDataActivity.sql
CREATE OR REPLACE FUNCTION update_modified_column()
    RETURNS TRIGGER AS
$$
BEGIN
    IF TG_OP = 'INSERT' OR ROW (NEW.*) IS DISTINCT FROM ROW (OLD.*)
    THEN
        NEW.modified = now();
        RETURN NEW;
    ELSE
        RETURN OLD;
    END IF;
END;
$$ LANGUAGE 'plpgsql';

CREATE OR REPLACE FUNCTION set_created_column()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.created = now();
    RETURN NEW;
END;
$$ LANGUAGE 'plpgsql';


CREATE OR REPLACE FUNCTION get_fims_user()
    RETURNS TEXT AS
$$
SELECT current_setting('fims.username', TRUE);
$$ LANGUAGE SQL;

COMMENT ON FUNCTION get_fims_user() IS $body$
Fetches the value of current_setting('fims.username', true).
This value can be set by calling SET LOCAL "fims.username" = 'user';
This is useful to retrieve the logged in user from the fims application in a postgresql trigger.
$body$;

CREATE OR REPLACE FUNCTION jsonb_diff_val(new JSONB, old JSONB)
    RETURNS JSONB AS
$$
DECLARE
    result        JSONB;
    object_result JSONB;
    v             RECORD;
BEGIN
    IF jsonb_typeof(new) = 'null' OR new = '{}' :: JSONB
    THEN
        RAISE INFO 'Returning old';
        RETURN old;
    END IF;

    result = new;
    FOR v IN SELECT *
             FROM jsonb_each(old)
        LOOP
            IF jsonb_typeof(new -> v.key) = 'object' AND jsonb_typeof(old -> v.key) = 'object'
            THEN
                object_result = jsonb_diff_val(new -> v.key, old -> v.key);
                IF object_result = '{}' :: JSONB
                THEN
                    result = result - v.key; --if empty remove
                ELSE
                    result = result || jsonb_build_object(v.key, object_result);
                END IF;
            ELSIF new -> v.key = old -> v.key
            THEN
                result = result - v.key;
            ELSIF result ? v.key
            THEN
                CONTINUE;
            ELSE
                result = result || jsonb_build_object(v.key, NULL);
            END IF;
        END LOOP;

    RETURN result;

END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION jsonb_diff_val(JSONB, JSONB) IS $body$
Generates a jsonb diff object where the 1st arg differs from the second. If the 1st arg does not include a
key in the 2nd arg, then the diff will contain '{key}: "null"'
$body$;

CREATE OR REPLACE FUNCTION entity_history()
    RETURNS TRIGGER AS
$body$
DECLARE
    audit_table_name   TEXT;
    audit_session_user TEXT;
    row_data           JSONB;
    changed_fields     JSONB;
BEGIN
    IF TG_WHEN <> 'AFTER'
    THEN
        RAISE EXCEPTION 'entity_history() may only run as an AFTER trigger';
    END IF;

    IF TG_LEVEL = 'STATEMENT'
    THEN
        RAISE EXCEPTION 'entity_history() does not support being a STATEMENT trigger';
    END IF;

    audit_table_name = TG_TABLE_SCHEMA :: TEXT || '.audit_table';

    audit_session_user = get_fims_user();
    IF NULLIF(audit_session_user, '') IS NULL
    THEN
        audit_session_user = 'postgres_role: ' || session_user :: TEXT;
    END IF;

    IF TG_OP = 'UPDATE'
    THEN
        row_data = to_jsonb(OLD.*) - 'tsv' - 'changed' - 'modified';
        changed_fields = jsonb_diff_val(to_jsonb(NEW.*) - 'tsv' - 'changed' - 'modified', row_data);
        IF changed_fields = '{}' :: JSONB
        THEN
            -- All changed fields are ignored. Skip this update.
            RETURN NULL;
        END IF;
    ELSIF TG_OP = 'DELETE'
    THEN
        row_data = to_jsonb(OLD.*) - 'tsv' - 'changed' - 'modified';
    ELSIF TG_OP = 'INSERT'
    THEN
        row_data = to_jsonb(NEW.*) - 'tsv' - 'changed' - 'modified';
    ELSE
        RAISE EXCEPTION '[entity_history] - Trigger func added as trigger for unhandled case: %, %', TG_OP, TG_LEVEL;
        RETURN NULL;
    END IF;

    EXECUTE 'INSERT INTO ' || audit_table_name || ' (table_name, user_name, ts, action, row_data, changed_fields) ' ||
            'VALUES (' || quote_literal(TG_TABLE_SCHEMA :: TEXT || '.' || TG_TABLE_NAME :: TEXT) || ', ' ||
            quote_literal(audit_session_user)
                || ', CURRENT_TIMESTAMP, ' || quote_literal(substring(TG_OP, 1, 1)) || ', ' ||
            quote_literal(row_data) || ', '
                || quote_nullable(changed_fields) || ')';

    RETURN NULL;
END;
$body$
    LANGUAGE plpgsql
    SECURITY DEFINER;

COMMENT ON FUNCTION entity_history() IS $body$
Track changes to a table at the row level.

This will create an entry in the SCHEMA.audit_table for each row that is updated, inserted, or deleted.
This is useful for creating a history of all changes to entities.
$body$;


CREATE TRIGGER entity_audit AFTER INSERT OR UPDATE OR DELETE ON network_1.sample FOR EACH ROW EXECUTE PROCEDURE entity_history();
BEGIN;
SELECT set_config('fims.username', 'biocode', true);
INSERT INTO network_1.sample VALUES (1,10,'{"sample":"one"}',now());
DO $$ BEGIN
 IF (SELECT latest_data_modification_username FROM public.expeditions WHERE id=10) <> 'biocode' THEN RAISE EXCEPTION 'insert actor'; END IF;
 IF (SELECT latest_data_modification_username FROM public.projects WHERE id=1) <> 'biocode' THEN RAISE EXCEPTION 'project insert actor'; END IF;
END $$;
COMMIT;
CREATE TEMP TABLE before_settings AS SELECT id, latest_data_modification, latest_data_modification_username FROM public.expeditions;
UPDATE public.expeditions SET title='Renamed' WHERE id=10;
UPDATE public.projects SET title='Renamed project' WHERE id=1;
DO $$ BEGIN
 IF EXISTS (SELECT 1 FROM before_settings b JOIN public.expeditions e USING(id) WHERE b.latest_data_modification IS DISTINCT FROM e.latest_data_modification OR b.latest_data_modification_username IS DISTINCT FROM e.latest_data_modification_username) THEN RAISE EXCEPTION 'settings changed data activity'; END IF;
END $$;
BEGIN;
SELECT set_config('fims.username', 'stweedt', true);
UPDATE network_1.sample SET expedition_id=11 WHERE id=1;
DO $$ BEGIN
 IF EXISTS (SELECT 1 FROM public.expeditions WHERE id IN (10,11) AND latest_data_modification_username <> 'stweedt') THEN RAISE EXCEPTION 'move actor'; END IF;
 IF EXISTS (SELECT 1 FROM public.projects WHERE id IN (1,2) AND latest_data_modification_username <> 'stweedt') THEN RAISE EXCEPTION 'move project actor'; END IF;
END $$;
COMMIT;
BEGIN;
SELECT set_config('fims.username', 'delete-user', true);
DELETE FROM network_1.sample WHERE id=1;
DO $$ BEGIN
 IF (SELECT latest_data_modification_username FROM public.expeditions WHERE id=11) <> 'delete-user' THEN RAISE EXCEPTION 'delete actor'; END IF;
END $$;
COMMIT;
-- Ignored housekeeping changes create no audit row and must not advance activity.
BEGIN;
SELECT set_config('fims.username','uploader',true);
INSERT INTO network_1.sample VALUES (2,10,'{"sample":"two"}',now());
COMMIT;
CREATE TEMP TABLE before_noop AS SELECT latest_data_modification, latest_data_modification_username FROM public.expeditions WHERE id=10;
UPDATE network_1.sample SET modified=now() WHERE id=2;
DO $$ BEGIN
 IF EXISTS (SELECT 1 FROM before_noop b CROSS JOIN public.expeditions e WHERE e.id=10 AND (b.latest_data_modification IS DISTINCT FROM e.latest_data_modification OR b.latest_data_modification_username IS DISTINCT FROM e.latest_data_modification_username)) THEN RAISE EXCEPTION 'housekeeping changed activity'; END IF;
END $$;
-- Older work cannot overwrite a newer timestamp/actor pair; same-time events use event_id.
SELECT public.record_data_activity(10,'2000-01-01Z','stale',999);
DO $$ BEGIN
 IF (SELECT latest_data_modification_username FROM public.expeditions WHERE id=10) <> 'uploader' THEN RAISE EXCEPTION 'stale event overwrote current actor'; END IF;
END $$;
SELECT public.record_data_activity(10,'2090-01-01Z','first',1000);
SELECT public.record_data_activity(10,'2090-01-01Z','second',1001);
SELECT public.record_data_activity(10,'2090-01-01Z','out-of-order',999);
DO $$ BEGIN
 IF (SELECT latest_data_modification_username FROM public.expeditions WHERE id=10) <> 'second' THEN RAISE EXCEPTION 'event tie-break failed'; END IF;
END $$;
SELECT 'Activity migration and tracking checks passed' AS result;

INSERT INTO public.projects(id,title) VALUES (4,'Cascade');
INSERT INTO public.expeditions(id,project_id,title) VALUES (14,4,'Cascade');
BEGIN;
SELECT set_config('fims.username','photo-uploader',true);
INSERT INTO network_1.sample VALUES (3,14,'{"photo":"one"}',now());
COMMIT;
BEGIN;
SELECT set_config('fims.username','cascade-user',true);
DELETE FROM public.expeditions WHERE id=14;
DO $$ BEGIN
 IF (SELECT latest_data_modification_username FROM public.projects WHERE id=4) <> 'cascade-user' THEN RAISE EXCEPTION 'cascade deletion lost actor'; END IF;
END $$;
COMMIT;
SELECT 'Cascade deletion check passed' AS result;
