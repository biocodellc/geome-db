-- Run as the schema/table owner with psql -v ON_ERROR_STOP=1 -f scripts/updateDataActivity.sql.
BEGIN;
ALTER TABLE public.projects ADD COLUMN IF NOT EXISTS latest_data_modification_username text;
ALTER TABLE public.projects ADD COLUMN IF NOT EXISTS latest_data_modification_event_id bigint;
ALTER TABLE public.expeditions ADD COLUMN IF NOT EXISTS latest_data_modification timestamp;
ALTER TABLE public.expeditions ADD COLUMN IF NOT EXISTS latest_data_modification_username text;
ALTER TABLE public.expeditions ADD COLUMN IF NOT EXISTS latest_data_modification_event_id bigint;
\ir dataActivityFunctions.sql
DO $body$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_trigger WHERE tgrelid = 'public.expeditions'::regclass AND tgname = 'remember_deleted_expedition_project') THEN
        CREATE TRIGGER remember_deleted_expedition_project BEFORE DELETE ON public.expeditions FOR EACH ROW EXECUTE PROCEDURE public.remember_deleted_expedition_project();
    END IF;
END;
$body$;
DO $body$
DECLARE item record;
BEGIN
    FOR item IN SELECT schemaname FROM pg_catalog.pg_tables WHERE schemaname ~ '^network_[0-9]+$' AND tablename = 'audit_table' LOOP
        IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_trigger WHERE tgrelid = format('%I.audit_table', item.schemaname)::regclass AND tgname = 'set_data_activity') THEN
            EXECUTE format('CREATE TRIGGER set_data_activity AFTER INSERT ON %I.audit_table FOR EACH ROW EXECUTE PROCEDURE public.set_data_activity_from_audit()', item.schemaname);
        END IF;
    END LOOP;
    -- Audit history filters out no-op changes and internal processing fields. Stop
    -- the old trigger advancing project activity without a matching audit actor.
    FOR item IN
        SELECT n.nspname, c.relname FROM pg_catalog.pg_trigger t
        JOIN pg_catalog.pg_class c ON c.oid = t.tgrelid JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
        WHERE n.nspname ~ '^network_[0-9]+$' AND t.tgname = 'set_project_last_modified'
    LOOP
        EXECUTE format('DROP TRIGGER set_project_last_modified ON %I.%I', item.nspname, item.relname);
    END LOOP;
END;
$body$;
-- Install tracking before scanning history; concurrent activity wins over older backfill.
COMMIT;
BEGIN;
DO $body$
DECLARE schema_row record; activity record;
BEGIN
    FOR schema_row IN SELECT schemaname FROM pg_catalog.pg_tables WHERE schemaname ~ '^network_[0-9]+$' AND tablename = 'audit_table' LOOP
        FOR activity IN EXECUTE format($sql$
            SELECT DISTINCT ON (expedition_id) expedition_id, ts, user_name, event_id
            FROM (
                SELECT DISTINCT a.event_id, a.ts, a.user_name, ids.expedition_id
                FROM %I.audit_table a
                CROSS JOIN LATERAL unnest(ARRAY[
                    NULLIF(a.row_data ->> 'expedition_id', '')::integer,
                    NULLIF(a.changed_fields ->> 'expedition_id', '')::integer
                ]) ids(expedition_id)
                WHERE a.action IN ('I', 'U', 'D') AND ids.expedition_id IS NOT NULL
            ) history ORDER BY expedition_id, ts DESC, event_id DESC
        $sql$, schema_row.schemaname)
        LOOP
            PERFORM public.record_data_activity(activity.expedition_id, activity.ts, activity.user_name, activity.event_id);
        END LOOP;
    END LOOP;
END;
$body$;
COMMIT;
