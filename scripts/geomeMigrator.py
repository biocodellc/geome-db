#!/usr/bin/python
"""
backup your db before running this script
"""

import psycopg2
from mysql import connector
import sys, argparse, requests, json, tempfile, os, csv
from os.path import isfile, join
from elasticsearch import Elasticsearch

# ENDPOINT = 'https://api.develop.geome-db.org/'
# BCID_URL = 'https://develop.bcid.biocode-fims.org'
ENDPOINT = 'http://localhost:8080/'
BCID_URL = 'http://localhost:8080/bcid'

EXPEDITION_RESOURCE_TYPE = "http://purl.org/dc/dcmitype/Collection"
WORKING_DIR = "output"

headers = {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
}

expedition_data = {}
config = {}


def migrate_project(psql, mysql, old_project_id, access_token, entities, client_id, client_secret,
                    config_file=None):
    project = get_project_data(mysql, old_project_id, entities)

    tmp_user_id = get_tmp_user_id(psql, access_token)
    project['tmp_user_id'] = tmp_user_id

    project['id'] = get_project_id(psql, project)
    project_existed = project['id'] is not None

    disable_triggers(psql)
    try:
        create_or_update_project(psql, project)

        project['config'] = get_project_config(psql, project)
        if not project_existed or project['config'] == {}:
            if not config_file or not os.path.exists(config_file):
                raise Exception(
                    'Project config does not exist and no config_file was provided, or the file does not exists')

            update_config(project['id'], access_token, config_file)
            project['config'] = get_project_config(psql, project)

        set_user_projects(psql, project)
        create_project_expeditions(psql, project, client_id, client_secret)

        # TODO need to create missing entity identifiers

        for expedition in project['expeditions']:
            data = fetch_expedition_data(old_project_id, expedition['expedition_code'])
            migrate(project['id'], expedition['expeditionCode'], access_token, data)

        update_project_and_expedition_users(psql, project)
    except Exception as e:
        enable_triggers(psql)
        raise e


def disable_triggers(psql):
    cursor = psql.cursor()
    query = """
    ALTER TABLE projects DISABLE TRIGGER set_projects_createdtime;
    ALTER TABLE projects DISABLE TRIGGER config_history;
    ALTER TABLE expeditions DISABLE TRIGGER set_expeditions_createdtime;
    """
    cursor.execute(query)
    psql.commit()
    cursor.close()


def enable_triggers(psql):
    cursor = psql.cursor()
    query = """
    ALTER TABLE projects ENABLE TRIGGER set_projects_createdtime;
    ALTER TABLE projects ENABLE TRIGGER config_history;
    ALTER TABLE expeditions ENABLE TRIGGER set_expeditions_createdtime;
    """
    cursor.execute(query)
    psql.commit()
    cursor.close()


def get_tmp_user_id(psql, access_token):
    cursor = psql.cursor()
    query = "SELECT user_id FROM oauth_tokens WHERE token = %s"
    cursor.execute(query, [access_token])
    result = cursor.fetchone()

    if not result:
        raise Exception("Invalid access_token")

    cursor.close()
    return result[0]


def get_project_data(mysql, project_id, entities):
    cursor = mysql.cursor()
    query = (
        "SELECT projectCode, projectTitle, validationXml, userId, public FROM projects WHERE projectId = %s"
    )
    cursor.execute(query, (project_id,))

    project = None

    for (projectCode, projectTitle, validationXml, userId, public) in cursor:
        project = {
            'project_code': projectCode,
            'project_title': projectTitle,
            'validationXml': validationXml,
            'user_id': userId,
            'public': public
        }

    cursor.close()
    if project is None:
        raise Exception('Failed to find project with id:' + project_id)

    project['users'] = get_project_users(mysql, project_id)
    project['expeditions'] = get_project_expeditions(mysql, project_id, entities)
    return project


def get_project_users(mysql, project_id):
    cursor = mysql.cursor()
    query = (
        "SELECT u.userId AS userId, username, password, hasSetPassword, email, institution, firstName, lastName FROM userProjects p JOIN users u ON p.userId = u.userId WHERE projectId = %s \
        UNION \
        SELECT u.userId AS userId, username, password, hasSetPassword, email, institution, firstName, lastName FROM projects p JOIN users u ON p.userId = u.userId WHERE projectId = %s")
    cursor.execute(query, (project_id, project_id,))

    users = []
    for (userId, username, password, hasSetPassword, email, institution, firstName, lastName) in cursor:
        users.append({
            'id': userId,
            'username': username,
            'password': password,
            'has_set_password': hasSetPassword,
            'email': email,
            'institution': institution,
            'first_name': firstName,
            'last_name': lastName
        })

    cursor.close()
    return users


foundEntityIdentifier = {}


def get_project_expeditions(mysql, project_id, entities):
    cursor = mysql.cursor()
    query = (
        "SELECT e.expeditionId, e.expeditionCode, e.expeditionTitle, e.userId, e.ts, e.public, b.identifier, u.firstName, u.lastName, u.email \
        FROM expeditions e \
        JOIN users u ON u.userId = e.userId \
        LEFT JOIN expeditionBcids eb ON e.expeditionId = eb.expeditionId \
        LEFT JOIN bcids b ON eb.bcidId = b.bcidId \
        WHERE b.resourceType = %s AND e.projectId = %s")
    cursor.execute(query, (EXPEDITION_RESOURCE_TYPE, project_id,))

    expeditions = []
    for (expeditionId, expeditionCode, expeditionTitle, userId, ts, public, identifier, first_name, last_name,
         email) in cursor:
        expeditions.append({
            'id': expeditionId,
            'expedition_code': expeditionCode,
            'expedition_title': expeditionTitle,
            'user_id': userId,
            'modified': ts,
            'public': public,
            'user': {
                'first_name': first_name,
                'last_name': last_name,
                'email': email
            },
            'identifier': identifier,
            'entity_identifiers': get_entity_identifiers(mysql, expeditionId, entities)
        })

    for entity in entities:
        if not foundEntityIdentifier[entity]:
            raise Exception(
                'Failed to locate a single entity bcid for entity: "' + entity + "'. Are you sure you spelled the conceptAlias correctly'"
            )

    cursor.close()
    return expeditions


def get_entity_identifiers(mysql, expedition_id, entities):
    cursor = mysql.cursor()
    query = (
        "SELECT b.title AS conceptAlias, b.identifier AS identifier FROM bcids b JOIN expeditionBcids eb ON b.bcidId = eb.bcidId WHERE eb.expeditionId = %s AND b.title IN ( %s )"
    )
    cursor.execute(query, (expedition_id, ",".join(entities),))

    entity_identifiers = []
    for (conceptAlias, identifier) in cursor:
        foundEntityIdentifier[conceptAlias] = True
        entity_identifiers.append({
            'concept_alias': conceptAlias,
            'identifier': identifier
        })

    cursor.close()
    return entity_identifiers


def get_project_id(psql, project):
    cursor = psql.cursor()
    query = 'SELECT id FROM projects WHERE project_code = %s'
    cursor.execute(query, [project['project_code']])
    res = cursor.fetchone()
    cursor.close()
    return res[0] if res is not None else None


def get_project_config(psql, project):
    cursor = psql.cursor()
    query = 'SELECT config FROM projects WHERE id = %s'
    cursor.execute(query, [project['id']])
    res = cursor.fetchone()
    cursor.close()
    print(type(res[0]))
    return res[0]


def create_or_update_project(psql, project):
    create_project_users(psql, project)
    cursor = psql.cursor()
    if not project['id']:
        sql = "INSERT INTO projects (project_code, project_title, config, user_id, public) VALUES (%s, %s, '{}', %s, %s);"
        cursor.execute(sql,
                       [project['project_code'], project['project_title'], project['tmp_user_id'],
                        True if project['public'] == 1 else False])
        psql.commit()
        project['id'] = get_project_id(psql, project)
        sql = """
          CREATE SCHEMA project_{};

          CREATE TABLE project_{}.audit_table
          (
            event_id bigserial primary key,
            table_name text not null, -- table the change was made to
            user_name text, -- user who made the change
            ts TIMESTAMP WITH TIME ZONE NOT NULL, -- timestamp the change happened
            action TEXT NOT NULL CHECK (action IN ('I','D','U', 'T')), -- INSERT, DELETE, UPDATE, or TRUNCATE
            row_data jsonb, -- For INSERT this is the new row values. For DELETE and UPDATE it is the old row values.
            changed_fields jsonb -- Null except UPDATE events. This is the result of jsonb_diff_val(NEW data, OLD data)
          );
        """.format(project['id'], project['id'])
        cursor.execute(sql)
        psql.commit()


def create_project_users(psql, project):
    cursor = psql.cursor()
    sql = "INSERT INTO users (id, username, password, has_set_password, email, institution, first_name, last_name) VALUES "
    params = []

    for user in project['users']:
        sql = sql + "(%s, %s, %s, %s, %s, %s, %s, %s), "
        params.extend([user['id'], user['username'], user['password'], True if user['has_set_password'] == 1 else False,
                       user['email'],
                       user['institution'], user['first_name'], user['last_name']])

    sql = sql[:-2] + " ON CONFLICT (id) DO NOTHING"
    cursor.execute(sql, params)
    psql.commit()
    cursor.close()


def set_user_projects(psql, project):
    cursor = psql.cursor()
    sql = "INSERT INTO user_projects (user_id, project_id) VALUES "
    params = []

    for user in project['users']:
        sql = sql + "(%s, %s), "
        params.extend([user['id'], project['id']])

    sql = sql[:-2] + " ON CONFLICT (user_id, project_id) DO NOTHING"
    cursor.execute(sql, params)
    psql.commit()
    cursor.close()


def create_project_expeditions(psql, project, client_id, client_secret):
    cursor = psql.cursor()
    sql = "INSERT INTO expeditions (id, project_id, expedition_code, expedition_title, identifier, visibility, user_id, modified, public) VALUES "
    params = []

    for expedition in project['expeditions']:
        sql = sql + "(%s, %s, %s, %s, %s, %s, %s, %s, %s), "
        params.extend([
            expedition['id'],
            project['id'],
            expedition['expedition_code'],
            expedition['expedition_title'],
            expedition['identifier'],
            'ANYONE' if expedition['public'] else 'EXPEDITION',
            project['tmp_user_id'],
            expedition['modified'],
            True if expedition['public'] == 1 else False
        ])

    sql = sql[:-2] + " ON CONFLICT (id) DO NOTHING"
    cursor.execute(sql, params)
    psql.commit()
    cursor.close()

    create_entity_identifiers(psql, project, client_id, client_secret)


def create_entity_identifiers(psql, project, client_id, client_secret):
    cursor = psql.cursor()
    sql = "INSERT INTO entity_identifiers (expedition_id, concept_alias, identifier) VALUES "
    params = []

    to_create = []
    print("Missing entity identifiers that need to be created:\n\nexpedition_id\tconcept_alias")
    for expedition in project['expeditions']:
        created = []
        for identifier in expedition['entity_identifiers']:
            sql = sql + "(%s, %s, %s), "
            created.append(identifier['concept_alias'])
            params.extend([
                expedition['id'],
                identifier['concept_alias'],
                identifier['identifier']
            ])

        for entity in project['config']['entities']:
            if entity['conceptAlias'] not in created and not entity_identifier_exists(psql, expedition['id'], entity['conceptAlias']):
                print("{}\t{}".format(expedition['id'], entity['conceptAlias']))
                to_create.append({'entity': entity, 'expedition_id': expedition['id'], 'user': expedition['user']})
    print("\n\n")

    sql = sql[:-2] + " ON CONFLICT (expedition_id, concept_alias) DO NOTHING"
    cursor.execute(sql, params)
    psql.commit()

    if len(to_create) > 0:
        create = input(
            'Would you like to mint the above identifiers? You will need to mint the identifiers at a later date if not. y/n: ').lower()
        if create == 'y':
            if not client_id or not client_secret:
                raise Exception("Both bcid_client_id and bcid_client_secret are required to mint bcids")
            access_token = authenticate_bcid(client_id, client_secret)

            for i in to_create:
                bcid = mint_bcid(access_token, i['entity'], i['user'])
                sql = "INSERT INTO entity_identifiers (expedition_id, concept_alias, identifier) VALUES (%s, %s, %s)"
                cursor.execute(sql, [i['expedition_id'], i['entity']['conceptAlias'], bcid['identifier']])
                psql.commit()

    cursor.close()


def entity_identifier_exists(psql, expedition_id, conceptAlias):
    cursor = psql.cursor()
    cursor.execute("SELECT count(*) FROM entity_identifiers WHERE concept_alias = %s AND expedition_id = %s",
                   [conceptAlias, expedition_id])
    res = cursor.fetchone()
    cursor.close()
    return True if res is not None and res[0] in [True, 1] else False


def authenticate_bcid(client_id, client_secret):
    h = dict(headers)
    h['Content-Type'] = None
    r = requests.post("{}/oAuth2/token".format(BCID_URL),
                      data={'grant_type': 'client_credentials', 'client_id': client_id, 'client_secret': client_secret},
                      headers=h)
    r.raise_for_status()
    return r.json()['access_token']


def mint_bcid(access_token, entity, user):
    bcid = {
        'publisher': 'GeOMe-db FIMS',
        'resourceType': entity['conceptURI'],
        'title': entity['conceptAlias'],
        'webAddress': 'https://ezid.cdlib.org/id/%7Bark%7D',
        'ezidRequest': True
    }

    creator = user['first_name']
    if creator != '' and user['last_name']:
        creator += " {}".format(user['last_name'])
        if user['email']:
            creator += " <{}>".format(user['email'])
    elif user['email']:
        creator += "{}".format(user['email'])
    else:
        creator = ""
    bcid['creator'] = creator

    h = dict(headers)
    h['Authorization'] = "Bearer {}".format(access_token)
    r = requests.post("{}/".format(BCID_URL), json=bcid, headers=h)
    if r.status_code != requests.codes.ok:
        print(r.text)
        r.raise_for_status()
    return r.json()


def update_project_and_expedition_users(psql, project):
    cursor = psql.cursor()
    sql = "UPDATE projects SET user_id = %s WHERE id = %s"
    cursor.execute(sql, [project['user_id'], project['id']])
    for expedition in project['expeditions']:
        sql = "UPDATE expeditions SET user_id = %s WHERE id = %s"
        cursor.execute(sql, [expedition['user_id'], expedition['id']])
    psql.commit()
    cursor.close()


def update_config(project_id, access_token, config_file):
    url = "{}projects/{}/config?access_token={}".format(ENDPOINT, project_id, access_token)

    with open(config_file) as f:
        config = json.load(f)
        response = requests.put(url, json=config)
        if response.status_code != requests.codes.ok:
            print(response.text)
            response.raise_for_status()


def fetch_expedition_data(project_id, expeditionCode):
    def get_cached_samples():
        file = os.path.join(WORKING_DIR, project_id, expeditionCode + "_sample.csv")
        if os.path.exists(file):
            with open(file, 'r') as f:
                reader = csv.DictReader(f)
                return [row for row in reader]

    if project_id == 25:
        data = {'sample': get_cached_samples()}
        # check for fasta
        file = os.path.join(WORKING_DIR, project_id, expeditionCode + "_fasta.csv")
        if os.path.exists(file):
            with open(file, 'r') as f:
                reader = csv.DictReader(f)
                data['fasta'] = [row for row in reader]
        # check for fastq
        file = os.path.join(WORKING_DIR, project_id, expeditionCode + "_fastq.csv")
        if os.path.exists(file):
            with open(file, 'r') as f:
                reader = csv.DictReader(f)
                data['fastq'] = [row for row in reader]
        if data['sample'] and data['fasta'] and data['fastq']:
            print('Using existing data in output dir for expedition: ', expeditionCode)
            return data
    else:
        data = get_cached_samples()
        if data:
            print('Using existing data in output dir for expedition: ', expeditionCode)
            return {'sample': data}

    es = Elasticsearch()
    query = {
        "query": {
            "expedition.expeditionCode.keyword": expeditionCode
        }
    }

    entity_data = {}
    res = es.search(index=project_id, doc_type="resource", body=query, size=10000)
    for doc in res['hits']['hits']:
        data = doc['_source']

        if source['fastaSequence']:
            entity_data['fasta'] = source['fastaSequence']
            del source['fastaSequence']
        if source['fastqMetadata']:
            entity_data['fastq'] = source['fastqMetadata']
            del source['fastqMetadata']

        # TODO what about > 10k results
        # TODO any other data cleanup?
        del source['expedition.expeditionCode']
        del source['bcid']
        entity_data['sample'] = source

    def write_file(records, file):
        with open(file, 'w') as f:
            columns = [x for row in data for x in row.keys()]
            columns = list(set(columns))

            csv_w = csv.writer(f)
            csv_w.writerow(columns)

            for i_r in data:
                csv_w.writerow(map(lambda x: i_r.get(x, ""), columns))

    if entity_data['sample']:
        file = os.path.join(WORKING_DIR, project_id, expeditionCode + "_sample.csv")
        write_file(entity_data['sample'], file)
    if entity_data['fasta']:
        file = os.path.join(WORKING_DIR, project_id, expeditionCode + "_fasta.csv")
        write_file(entity_data['fasta'], file)
    if entity_data['fastq']:
        file = os.path.join(WORKING_DIR, project_id, expeditionCode + "_fastq.csv")
        write_file(entity_data['fastq'], file)

    return entity_data


def migrate(project_id, code, access_token, data):
    # TODO finish this part. need to transform data to new entities and upload
    print('Migrating data for expedition: ', code)
    return

    validate_url = "{}data/validate?access_token={}".format(ENDPOINT, access_token)

    data = {
        'projectId': project_id,
        'expeditionCode': code,
        'upload': True,
    }
    files = [
        ('dataSourceFiles',
         ('Collecting_Events.txt', open(join(base_dir, EVENTS_FILE.format(code)), 'rb'), 'text/plain')),
        ('dataSourceFiles', ('Specimens.txt', open(join(base_dir, SPECIMENS_FILE.format(code)), 'rb'), 'text/plain')),
        ('dataSourceFiles', ('Tissues.txt', open(join(base_dir, TISSUES_FILE.format(code)), 'rb'), 'text/plain')),
        ('dataSourceMetadata', (None, json.dumps([
            {
                "dataType": 'TABULAR',
                "filename": "Collecting_Events.txt",
                "metadata": {
                    "sheetName": "Events"
                }
            },
            {
                "dataType": 'TABULAR',
                "filename": "Specimens.txt",
                "metadata": {
                    "sheetName": "Samples"
                }

            },
            {
                "dataType": 'TABULAR',
                "filename": 'Tissues.txt',
                "metadata": {
                    "sheetName": "Tissues"
                }
            }
        ]), 'application/json')),
    ]

    h = dict(headers)
    h['Content-Type'] = None
    r = requests.post(validate_url, data=data, files=files, headers=h)
    if r.status_code >= 400:
        try:
            print('\nERROR: ' + r.json().get('usrMessage'))
        except ValueError:
            print('\nERROR: ' + r.text)
    print('\n')
    r.raise_for_status()

    response = r.json()

    def print_messages(groupMessage, messages):
        print("\t{}:\n".format(groupMessage))

        for message in messages:
            print("\t\t" + message.get('message'))

        print('\n')

    # print(json.dumps(response.get('messages'), indent=4))
    for entityResults in response.get('messages'):
        level = 'Errors' if len(entityResults.get('errors')) > 0 else 'Warnings'
        print("\n\n{} found on worksheet: \"{}\" for entity: \"{}\"\n\n".format(level, entityResults.get('sheetName'),
                                                                                entityResults.get('entity')))

        for group in entityResults.get('warnings'):
            print_messages(group.get('groupMessage'), group.get('messages'))

        for group in entityResults.get('errors'):
            print_messages(group.get('groupMessage'), group.get('messages'))

    if response.get('hasError'):
        print("Validation error(s) attempting to upload expedition: {}".format(code))
        sys.exit()
    elif not response.get('isValid'):
        cont = input("Warnings found during validation. Would you like to continue? (y/n)   ")
        if cont.lower() != 'y':
            sys.exit()

    upload_url = response.get('uploadUrl') + '?access_token=' + access_token
    r = requests.put(upload_url, headers=headers)
    if r.status_code > 400:
        print('\nERROR: ' + r.json().get('usrMessage'))
        print('\n')
        r.raise_for_status()

    print('Successfully uploaded expedition: ', code)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description='Migrate geome data from single entity config to a multi entity config')
    parser.add_argument("project_id", help="The old id of the project we are migrating")
    parser.add_argument("access_token", help="Access Token of the user to upload under")
    parser.add_argument("psql_connection_string",
                        help="psycopg2 connection info for the postgres db. Ex. 'host=biscicol4.acis.ufl.edu user=bisicoldev password=pass dbname=biscicoldev")
    parser.add_argument("mysql_user", help="mysql db user")
    parser.add_argument("mysql_password", help="mysql db password")
    parser.add_argument("mysql_host", help="mysql db host")
    parser.add_argument("mysql_db", help="mysql db name")
    parser.add_argument("--old_entities", help="entites in the old config in order to migrate the entity identifiers",
                        nargs='+', required=True)
    parser.add_argument("--bcid_client_id", help="client_id for the bcid system")
    parser.add_argument("--bcid_client_secret", help="client_secret for the bcid system")
    parser.add_argument("config_file", help="Project configuration JSON to set the project to before uploading data")
    args = parser.parse_args()

    # if not args.psql_connection_string():
    #     raise
    psql = psycopg2.connect(args.psql_connection_string)
    mysql = connector.connect(user=args.mysql_user, passwd=args.mysql_password, host=args.mysql_host, db=args.mysql_db,
                              buffered=True)

    migrate_project(psql, mysql, args.project_id, args.access_token, args.old_entities, args.bcid_client_id,
                    args.bcid_client_secret, args.config_file)
