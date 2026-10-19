-- Apply once to an existing database. Existing entity-table triggers already call
-- set_project_last_modified(), so replacing the function updates every entity type.
-- Run with psql -v ON_ERROR_STOP=1 -f scripts/updateExpeditionLastModified.sql.
BEGIN;

CREATE OR REPLACE FUNCTION public.set_project_last_modified()
    RETURNS TRIGGER AS
$body$
DECLARE
    expedition_id int;
    affected_expeditions int[];
BEGIN
    IF TG_WHEN <> 'AFTER'
    THEN
        RAISE EXCEPTION 'set_project_last_modified() may only run as an AFTER trigger';
    END IF;

    IF TG_LEVEL = 'STATEMENT'
    THEN
        RAISE EXCEPTION 'set_project_last_modified() does not support being a STATEMENT trigger';
    END IF;

    IF TG_OP = 'DELETE'
    THEN
        affected_expeditions = ARRAY[OLD.expedition_id];
    ELSIF TG_OP = 'UPDATE' AND OLD.expedition_id IS DISTINCT FROM NEW.expedition_id
    THEN
        affected_expeditions = ARRAY[OLD.expedition_id, NEW.expedition_id];
    ELSE
        affected_expeditions = ARRAY[NEW.expedition_id];
    END IF;

    FOREACH expedition_id IN ARRAY affected_expeditions
    LOOP
        UPDATE public.projects
        SET latest_data_modification = CURRENT_TIMESTAMP
        WHERE id = (SELECT project_id FROM public.expeditions WHERE id = expedition_id);

        UPDATE public.expeditions
        SET modified = CURRENT_TIMESTAMP
        WHERE id = expedition_id;
    END LOOP;

    RETURN NULL;
END;
$body$
    LANGUAGE plpgsql
    SECURITY DEFINER;

COMMENT ON FUNCTION public.set_project_last_modified() IS $body$
Update the project's latest_data_modification and the expedition's modified timestamp when a record is inserted, updated, or deleted.
$body$;

-- Make the new trigger behavior visible before scanning audit history. Uploads
-- made during the scan will then update their expedition immediately.
COMMIT;
BEGIN;

-- Recover the latest audited data-change time for existing expeditions, including
-- expeditions whose latest change deleted every remaining record. The audit
-- tables retain INSERT, UPDATE, and DELETE timestamps for each entity type.
CREATE TEMP TABLE expedition_data_modifications
(
    expedition_id integer PRIMARY KEY,
    modified timestamp NOT NULL
) ON COMMIT DROP;

DO $body$
DECLARE
    audit_schema text;
BEGIN
    FOR audit_schema IN
        SELECT schemaname
        FROM pg_catalog.pg_tables
        WHERE schemaname ~ '^network_[0-9]+$' AND tablename = 'audit_table'
    LOOP
        EXECUTE format($sql$
            INSERT INTO pg_temp.expedition_data_modifications (expedition_id, modified)
            SELECT (row_data ->> 'expedition_id')::integer, max(ts)::timestamp
            FROM %I.audit_table
            WHERE action IN ('I', 'U', 'D') AND row_data ? 'expedition_id'
            GROUP BY (row_data ->> 'expedition_id')::integer
            ON CONFLICT (expedition_id) DO UPDATE
            SET modified = greatest(expedition_data_modifications.modified, EXCLUDED.modified)
        $sql$, audit_schema);
    END LOOP;
END;
$body$;

-- The normal BEFORE UPDATE trigger sets modified to now(). Disable it only for
-- this historical repair, then restore it in the same transaction.
ALTER TABLE public.expeditions DISABLE TRIGGER update_expeditions_modtime;

UPDATE public.expeditions AS e
SET modified = m.modified
FROM pg_temp.expedition_data_modifications AS m
WHERE e.id = m.expedition_id AND m.modified > e.modified;

ALTER TABLE public.expeditions ENABLE TRIGGER update_expeditions_modtime;

COMMIT;
