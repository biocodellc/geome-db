## Create a network

ensure the postgres function is loaded:

      CREATE OR REPLACE FUNCTION create_network(network_id integer, network_title text, user_id integer)
        RETURNS VOID AS
      $func$
      DECLARE
         db_owner text;
      BEGIN
        db_owner := (SELECT pg_catalog.pg_get_userbyid(d.datdba) FROM pg_catalog.pg_database d WHERE d.datname = (select current_database ()) ORDER BY 1);

        IF network_id IS NULL THEN
            EXECUTE format(E'insert into networks(title, config, user_id) VALUES (\'%s\', \'{}\', %s)', network_title, user_id);
            network_id := (SELECT id from networks order by id desc limit 1);
        ELSE
            EXECUTE format(E'insert into networks(id, title, config, user_id) VALUES (%s, \'%s\', \'{}\', %s)', network_id, network_title, user_id);
        END IF;


        EXECUTE format('CREATE SCHEMA network_%s', network_id);
      
        EXECUTE format(E'
        CREATE TABLE network_%s.audit_table
        (
          event_id bigserial primary key,
          table_name text not null, -- table the change was made to
          user_name text, -- user who made the change
          ts TIMESTAMP WITH TIME ZONE NOT NULL, -- timestamp the change happened
          action TEXT NOT NULL CHECK (action IN (\'I\',\'D\',\'U\', \'T\')), -- INSERT, DELETE, UPDATE, or TRUNCATE
          row_data jsonb, -- For INSERT this is the new row values. For DELETE and UPDATE it is the old row values.
          changed_fields jsonb -- Null except UPDATE events. This is the result of jsonb_diff_val(NEW data, OLD data)
        );
        ', network_id);
      
        EXECUTE format('alter schema network_%s owner to %s', network_id, db_owner);
        EXECUTE format('alter table network_%s.audit_table owner to %s', network_id, db_owner);
        
        IF EXISTS (
              SELECT                       -- SELECT list can stay empty for this
              FROM   pg_catalog.pg_roles
              WHERE  rolname = 'readaccess') THEN
        
            EXECUTE format('GRANT USAGE ON SCHEMA network_%s TO readaccess', network_id);
            EXECUTE format('GRANT SELECT ON ALL TABLES IN SCHEMA network_%s TO readaccess', network_id);
            EXECUTE format('GRANT SELECT ON ALL SEQUENCES IN SCHEMA network_%s TO readaccess', network_id);
        END IF;
      
      END
      $func$  LANGUAGE plpgsql;
      
Issue the following sql cmd against the db you wish to create the network in:

    select create_network(NULL, 'GeOMe Network', ${USER_ID});
    or
    select create_network(1, 'GeOMe Network', ${USER_ID});

