## Create a project

*Note:* This is temporary, and will be added to the ui at a future date.

Issue the following sql cmd against the db you wish to create the project in:

    insert into projects(project_code, project_title, project_url, config, user_id, public) 
        VALUES ('MBIO', 'Moorea Biocode Project', 'http://www.biscicol.org/', '{}', USER_ID, true);
        
Then add user_projects table entry:

    insert into user_projects(project_id, user_id) VALUES (PROJECT_ID, USER_ID);