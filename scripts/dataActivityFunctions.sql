-- Shared by the existing-database migration and fresh-install schema.
CREATE OR REPLACE FUNCTION public.record_data_activity(expedition_id integer, activity_at timestamptz, actor text, audit_event_id bigint)
RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path = pg_catalog, public AS $body$
DECLARE
    project_id integer;
    activity_utc timestamp := activity_at AT TIME ZONE 'UTC';
BEGIN
    SELECT e.project_id INTO project_id FROM public.expeditions e WHERE e.id = expedition_id;
    IF project_id IS NULL THEN
        project_id := (NULLIF(current_setting('fims.deleted_expedition_projects', true), '')::jsonb ->> expedition_id::text)::integer;
    END IF;
    IF project_id IS NULL THEN RETURN; END IF;
    -- Project first gives concurrent changes within a project a consistent lock order.
    UPDATE public.projects p
    SET latest_data_modification = activity_utc,
        latest_data_modification_username = NULLIF(actor, ''),
        latest_data_modification_event_id = audit_event_id
    WHERE p.id = project_id AND (p.latest_data_modification IS NULL OR p.latest_data_modification < activity_utc
        OR (p.latest_data_modification = activity_utc AND COALESCE(p.latest_data_modification_event_id, -1) <= audit_event_id));
    UPDATE public.expeditions e
    SET latest_data_modification = activity_utc,
        latest_data_modification_username = NULLIF(actor, ''),
        latest_data_modification_event_id = audit_event_id
    WHERE e.id = expedition_id AND (e.latest_data_modification IS NULL OR e.latest_data_modification < activity_utc
        OR (e.latest_data_modification = activity_utc AND COALESCE(e.latest_data_modification_event_id, -1) <= audit_event_id));
END;
$body$;

CREATE OR REPLACE FUNCTION public.set_data_activity_from_audit()
RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path = pg_catalog, public AS $body$
DECLARE
    old_expedition integer;
    new_expedition integer;
BEGIN
    IF NEW.action NOT IN ('I', 'U', 'D') THEN RETURN NULL; END IF;
    old_expedition := NULLIF(NEW.row_data ->> 'expedition_id', '')::integer;
    new_expedition := COALESCE(NULLIF(NEW.changed_fields ->> 'expedition_id', '')::integer, old_expedition);
    -- A move affects both the old and new expedition. Audit rows contain the old
    -- values on UPDATE; changed_fields contains the destination.
    IF old_expedition IS NOT NULL THEN
        PERFORM public.record_data_activity(old_expedition, NEW.ts, NEW.user_name, NEW.event_id);
    END IF;
    IF new_expedition IS DISTINCT FROM old_expedition AND new_expedition IS NOT NULL THEN
        PERFORM public.record_data_activity(new_expedition, NEW.ts, NEW.user_name, NEW.event_id);
    END IF;
    RETURN NULL;
END;
$body$;

-- Cascading child-row deletions run after the parent expedition has disappeared.
-- Retain its project mapping only for this transaction so audited deletions can
-- still update the project summary. Empty-expedition deletion creates no activity.
CREATE OR REPLACE FUNCTION public.remember_deleted_expedition_project()
RETURNS trigger LANGUAGE plpgsql SET search_path = pg_catalog, public AS $body$
BEGIN
    PERFORM set_config('fims.deleted_expedition_projects',
        (COALESCE(NULLIF(current_setting('fims.deleted_expedition_projects', true), '')::jsonb, '{}'::jsonb)
            || jsonb_build_object(OLD.id::text, OLD.project_id))::text, true);
    RETURN OLD;
END;
$body$;
