## Create a project

*Note:* This is temporary, and will be added to the ui at a future date.

ensure the postgres function is loaded:

      CREATE OR REPLACE FUNCTION create_project(project_id integer, project_code text, project_title text, user_id integer)
        RETURNS VOID AS
      $func$
      DECLARE
         db_owner text;
      BEGIN
        db_owner := (SELECT pg_catalog.pg_get_userbyid(d.datdba) FROM pg_catalog.pg_database d WHERE d.datname = (select current_database ()) ORDER BY 1);

        IF project_id IS NULL THEN
            EXECUTE format(E'insert into projects(project_code, project_title, config, user_id, public) VALUES (\'%s\', \'%s\', \'{}\', %s, true)', project_code, project_title, user_id);
            project_id := (SELECT id from projects order by id desc limit 1);
        ELSE
            EXECUTE format(E'insert into projects(id, project_code, project_title, config, user_id, public) VALUES (%s, \'%s\', \'%s\', \'{}\', %s, true)', project_id, project_code, project_title, user_id);
        END IF;


        EXECUTE format('insert into user_projects(project_id, user_id) VALUES (%s, %s)', project_id, user_id);

        EXECUTE format('CREATE SCHEMA project_%s', project_id);
      
        EXECUTE format(E'
        CREATE TABLE project_%s.audit_table
        (
          event_id bigserial primary key,
          table_name text not null, -- table the change was made to
          user_name text, -- user who made the change
          ts TIMESTAMP WITH TIME ZONE NOT NULL, -- timestamp the change happened
          action TEXT NOT NULL CHECK (action IN (\'I\',\'D\',\'U\', \'T\')), -- INSERT, DELETE, UPDATE, or TRUNCATE
          row_data jsonb, -- For INSERT this is the new row values. For DELETE and UPDATE it is the old row values.
          changed_fields jsonb -- Null except UPDATE events. This is the result of jsonb_diff_val(NEW data, OLD data)
        );
        ', project_id);
      
        EXECUTE format('alter schema project_%s owner to %s', project_id, db_owner);
        EXECUTE format('alter table project_%s.audit_table owner to %s', project_id, db_owner);
        
        IF EXISTS (
              SELECT                       -- SELECT list can stay empty for this
              FROM   pg_catalog.pg_roles
              WHERE  rolname = 'readaccess') THEN
        
            EXECUTE format('GRANT USAGE ON SCHEMA project_%s TO readaccess', project_id);
            EXECUTE format('GRANT SELECT ON ALL TABLES IN SCHEMA project_%s TO readaccess', project_id);
            EXECUTE format('GRANT SELECT ON ALL SEQUENCES IN SCHEMA project_%s TO readaccess', project_id);
        END IF;
      
      END
      $func$  LANGUAGE plpgsql;
      
Issue the following sql cmd against the db you wish to create the project in:

    select create_project(NULL, 'MBIO', 'Moorea Biocode Project', ${USER_ID});
    or
    select create_project(3, 'MBIO', 'Moorea Biocode Project', ${USER_ID});

