## Create a project

*Note:* This is temporary, and will be added to the ui at a future date.

Issue the following sql cmd against the db you wish to create the project in:

    insert into projects(project_code, project_title, project_url, config, user_id, public) 
        VALUES ('MBIO', 'Moorea Biocode Project', 'http://www.biscicol.org/', '{}', ${USER_ID}, true);
        
Then add user_projects table entry:

    insert into user_projects(project_id, user_id) VALUES (${PROJECT_ID}, ${USER_ID});
    
Then create the project schema:

      CREATE SCHEMA project_${projectId};
    
      CREATE TABLE project_${projectId}.audit_table
      (
        event_id bigserial primary key,
        table_name text not null, -- table the change was made to
        user_name text, -- user who made the change
        ts TIMESTAMP WITH TIME ZONE NOT NULL, -- timestamp the change happened
        action TEXT NOT NULL CHECK (action IN ('I','D','U', 'T')), -- INSERT, DELETE, UPDATE, or TRUNCATE
        row_data jsonb, -- For INSERT this is the new row values. For DELETE and UPDATE it is the old row values.
        changed_fields jsonb -- Null except UPDATE events. This is the result of jsonb_diff_val(NEW data, OLD data)
      );
      
      alter schema project_${projectId} owner to biscicoldev;
      alter table project_${projectId}.audit_table owner to biscicoldev;