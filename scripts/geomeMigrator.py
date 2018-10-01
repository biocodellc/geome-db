#!/usr/bin/python
"""
backup your db before running this script
"""

import psycopg2
from mysql import connector
import sys, argparse, requests, json, tempfile, os, csv, io, re
from elasticsearch import Elasticsearch

# ENDPOINT = 'https://api.develop.geome-db.org/'
# BCID_URL = 'https://develop.bcid.biocode-fims.org'
# ES_ENDPOINT = 'http://esr.biocodellc.com:80/'
ENDPOINT = 'http://localhost:8080/'
BCID_URL = 'http://localhost:8080/bcid'
ES_ENDPOINT = 'https://localhost:9200'

ENTITY_MAPPING = {
    'Resource': 'Sample'
}

COLUMN_MAPPING = {
    'Samples': {},
    'fastaSequence': {},
    'fastqMetadata': {},
}

VALUE_MAPPINGS = {
    'Samples': {
        'country': {
            'United States of America': 'USA',
            'Cocos (Keeling) Islands': 'Cocos Islands',
            "Timor L'este": 'East Timor',
            'Brunei Darussalam': 'Brunei'
        },
        'yearCollected': {
            '': 'Unknown',
            '-': 'Unknown',
            '?': 'Unknown'
        }
    }
}

EXCLUDE_EXPEDITIONS = []


MONTH_MAP = {
    'january': 1,
    'february': 2,
    'march': 3,
    'april': 4,
    'may': 5,
    'june': 6,
    'july': 7,
    'august': 8,
    'september': 9,
    'october': 10,
    'november': 11,
    'december': 12
}

EXPEDITION_RESOURCE_TYPE = "http://purl.org/dc/dcmitype/Collection"
WORKING_DIR = "output"

headers = {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
}

expedition_data = {}
config = {}

def migrate_project(psql, mysql, old_project_id, access_token, entities, client_id, client_secret,
                    config_file=None, accept_warnings=False):
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
        elif config_file and os.path.exists(config_file):
            update_project_user(psql, project['id'], tmp_user_id)
            update_config(project['id'], access_token, config_file)
            project['config'] = get_project_config(psql, project)

        set_user_projects(psql, project)
        create_project_expeditions(psql, project, client_id, client_secret)

        uri_mapping = get_uri_mapping(project)

        data = {}
        for expedition in project['expeditions']:
            data[expedition['expedition_code']] = fetch_expedition_data(old_project_id, expedition['expedition_code'], uri_mapping)

        for expedition in project['expeditions']:
            if expedition['expedition_code'] in EXCLUDE_EXPEDITIONS:
                print('SKIPPING EXPEDITION:', expedition['expedition_code'])
            else:
                migrate(psql, project['id'], expedition, access_token, data[expedition['expedition_code']], project['config'], accept_warnings)

        update_project_and_expedition_users(psql, project)
    except BaseException as e:
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
        "SELECT u.userId AS userId, username, password, email, institution, firstName, lastName FROM userProjects p JOIN users u ON p.userId = u.userId WHERE projectId = %s \
        UNION \
        SELECT u.userId AS userId, username, password, email, institution, firstName, lastName FROM projects p JOIN users u ON p.userId = u.userId WHERE projectId = %s")
    cursor.execute(query, (project_id, project_id,))

    users = []
    for (userId, username, password, email, institution, firstName, lastName) in cursor:
        users.append({
            'id': userId,
            'username': username,
            'password': password,
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
        if entity not in foundEntityIdentifier or not foundEntityIdentifier[entity]:
            raise Exception(
                'Failed to locate a single entity bcid for entity: "' + entity + '". Are you sure you spelled the conceptAlias correctly'
            )

    cursor.close()
    return expeditions


def get_entity_identifiers(mysql, expedition_id, entities):
    cursor = mysql.cursor()
    query = (
        "SELECT b.title, b.identifier FROM bcids b JOIN expeditionBcids eb ON b.bcidId = eb.bcidId WHERE eb.expeditionId = %s AND b.title IN ( {} )".format("'" + "', '".join(entities) + "'")
    )
    cursor.execute(query, (expedition_id,))

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
            re.sub(' spreadsheet$', '', expedition['expedition_title']),
            expedition['identifier'],
            'ANYONE' if expedition['public'] else 'EXPEDITION',
            project['tmp_user_id'],
            expedition['modified'],
            True if expedition['public'] == 1 else False
        ])

    sql = sql[:-2] + " ON CONFLICT (id) DO UPDATE SET user_id = {}".format(project['tmp_user_id'])
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
            alias = entity['conceptAlias']
            alias = ENTITY_MAPPING[alias] if alias in ENTITY_MAPPING else alias
            entity['conceptAlias'] = alias
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
    update_project_user(psql, project['id'], project['user_id'])

    cursor = psql.cursor()
    for expedition in project['expeditions']:
        sql = "UPDATE expeditions SET user_id = %s WHERE id = %s"
        cursor.execute(sql, [expedition['user_id'], expedition['id']])
    psql.commit()
    cursor.close()


def update_project_user(psql, project_id, user_id):
    cursor = psql.cursor()
    sql = "UPDATE projects SET user_id = %s WHERE id = %s"
    cursor.execute(sql, [user_id, project_id])
    psql.commit()
    cursor.close()


def update_config(project_id, access_token, config_file):
    print("Updating project config")
    url = "{}projects/{}/config?access_token={}".format(ENDPOINT, project_id, access_token)

    with open(config_file) as f:
        config = json.load(f)
        response = requests.put(url, json=config)
        if response.status_code != requests.codes.ok:
            print(response.text)
            response.raise_for_status()


def get_uri_mapping(project):
    uri_mapping = {}
    for entity in project['config']['entities']:
        for attribute in entity['attributes']:
            uri_mapping[attribute['uri']] = attribute['column']
    return uri_mapping


def fetch_expedition_data(project_id, expeditionCode, uri_mapping):
    def get_cached_samples():
        file = os.path.join(WORKING_DIR, project_id, expeditionCode + "_sample.csv")
        if os.path.exists(file):
            with open(file, 'r') as f:
                reader = csv.DictReader(f)
                return [row for row in reader]

    if project_id == '25' or project_id == 25:
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
        if data['sample'] != None and len(data['sample']) > 0:
            print('Using existing data in output dir for expedition: ', expeditionCode)
            return data
    else:
        data = get_cached_samples()
        if data:
            print('Using existing data in output dir for expedition: ', expeditionCode)
            return {'sample': data}

    print('Fetching data for expedition: ', expeditionCode)
    es = Elasticsearch(ES_ENDPOINT)
    query = {
        "query": {
            "term": {
                "expedition.expeditionCode.keyword": expeditionCode
            }
        }
    }

    entity_data = {'sample': []}
    res = es.search(index=project_id, doc_type="resource", body=query, size=10000)
    if res['hits']['total'] >= 10000:
        print("More then 10k records. Need to fix this script and paginate response")
    for doc in res['hits']['hits']:
        data = doc['_source']

        data['urn:materialSampleID'] = data['urn:materialSampleID'].replace('-', '_')
        if 'fastaSequence' in data:
            if 'fasta' not in entity_data:
                entity_data['fasta'] = []
            for d in data['fastaSequence']:
                entity_data['fasta'].append({
                    'marker': d['urn:marker'],
                    'sequence': d['urn:sequence'],
                    'urn:materialSampleID': data['urn:materialSampleID'],
                })
            del data['fastaSequence']
        if 'fastqMetadata' in data:
            if 'fastq' not in entity_data:
                entity_data['fastq'] = []
            data['fastqMetadata']['urn:materialSampleID'] = data['urn:materialSampleID']
            entity_data['fastq'].append(data['fastqMetadata'])
            del data['fastqMetadata']

        # TODO what about > 10k results
        del data['expedition.expeditionCode']
        del data['bcid']
        entity_data['sample'].append(data)

    def write_file(data, file):
        directory = os.path.dirname(file)
        if not os.path.exists(directory):
            os.makedirs(directory)
        with open(file, 'w') as f:
            columns = [x for row in data for x in row.keys()]
            columns = list(set(columns))

            csv_w = csv.writer(f)
            csv_w.writerow(columns)

            for i_r in data:
                csv_w.writerow(map(lambda x: i_r.get(x, ""), columns))

    if 'sample' in entity_data:
        file = os.path.join(WORKING_DIR, project_id, expeditionCode + "_sample.csv")
        entity_data['sample'] = transform_data(entity_data['sample'], uri_mapping)
        write_file(entity_data['sample'], file)
    if 'fasta' in entity_data:
        file = os.path.join(WORKING_DIR, project_id, expeditionCode + "_fasta.csv")
        d = entity_data['fasta']
        write_file(d, file)
    if 'fastq' in entity_data:
        file = os.path.join(WORKING_DIR, project_id, expeditionCode + "_fastq.csv")
        write_file(entity_data['fastq'], file)

    return entity_data


def transform_data(data, uri_mapping):
    transformed = []

    for d in data:
        t = {}
        for key in d.keys():
            if key == 'urn:yearCollected' and d[key] == '2005-2007':
                t[uri_mapping[key]] = '2006'
                t['eventRemarks'] = "verbatimYear = {}".format(d[key])
            elif key in uri_mapping:
                t[uri_mapping[key]] = d[key]
            else:
                t[key] = d[key]
        transformed.append(t)

    return transformed


def migrate(psql, project_id, expedition, access_token, old_data, config, accept_warnings):
    code = expedition['expedition_code']
    print('Migrating data for expedition: ', code)

    transformed_data = data_to_worksheets(old_data, config)

    validate_url = "{}data/validate?access_token={}".format(ENDPOINT, access_token)

    data = {
        'projectId': project_id,
        'expeditionCode': code,
        'upload': True,
    }

    files = []
    metadata = []

    for sheet in transformed_data.keys():
        if sheet not in ['fastaSequence', 'fastqMetadata']:
            files.append(
                ('dataSourceFiles', ("{}.csv".format(sheet), data_to_filelike_csv(transformed_data[sheet]), 'text/plain'))
            )
            metadata.append(
                {
                    "dataType": 'TABULAR',
                    "filename": "{}.csv".format(sheet),
                    "metadata": {
                        "sheetName": sheet
                    },
                    "reload": True
                }
            )

    files.append(('dataSourceMetadata', (None, json.dumps(metadata), 'application/json')))

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

    for entityResults in response.get('messages'):
        if len(entityResults.get('errors')) > 0:
            print("\n\n{} found on worksheet: \"{}\" for entity: \"{}\"\n\n".format('Errors', entityResults.get('sheetName'),
                                                                                    entityResults.get('entity')))

            for group in entityResults.get('errors'):
                print_messages(group.get('groupMessage'), group.get('messages'))

        if len(entityResults.get('warnings')) > 0:
            print("\n\n{} found on worksheet: \"{}\" for entity: \"{}\"\n\n".format('Warnings', entityResults.get('sheetName'),
                                                                                    entityResults.get('entity')))

            for group in entityResults.get('warnings'):
                print_messages(group.get('groupMessage'), group.get('messages'))

    if response.get('hasError'):
        print("Validation error(s) attempting to upload expedition: {}".format(code))
        sys.exit()
    elif not response.get('isValid') and not accept_warnings:
        cont = input("Warnings found during validation. Would you like to continue? (y/n)   ")
        if cont.lower() != 'y':
            sys.exit()

    upload_url = response.get('uploadUrl') + '?access_token=' + access_token
    r = requests.put(upload_url, headers=headers)
    if r.status_code >= 400:
        print('\nERROR: ' + r.json().get('usrMessage'))
        print('\n')
        r.raise_for_status()

    if 'fastaSequence' in transformed_data:
        insert_fasta_data(psql, project_id, expedition['id'], transformed_data['fastaSequence'])
    if 'fastqMetadata' in transformed_data:
        insert_fastq_data(psql, project_id, expedition['id'], transformed_data['fastqMetadata'])

    print('Successfully uploaded expedition: ', code)


def insert_fastq_data(psql, project_id, expedition_id, data):
    cursor = psql.cursor()
    sql = "INSERT INTO project_{}.fastqMetadata (local_identifier, expedition_id, data, parent_identifier) VALUES ".format(project_id)

    for row in data:
        row['identifier'] = row['urn:materialSampleID']
        if 'bioSample' in row:
            if row['bioSample'] == "":
                del row['bioSample']
            else:
                row['bioSample'] = row['bioSample'].replace("'", '"')
        row['filenames'] = row['filenames'].replace("'", '"')
        sql += "('{}', {}, '{}'::jsonb, '{}'), ".format(
            row['identifier'],
            expedition_id,
            json.dumps(row),
            row['identifier']
        )

    sql = sql[:-2] + " ON CONFLICT (local_identifier, expedition_id) DO NOTHING"
    cursor.execute(sql)
    psql.commit()
    cursor.close()


def insert_fasta_data(psql, project_id, expedition_id, data):
    cursor = psql.cursor()
    sql = "INSERT INTO project_{}.fastaSequence (local_identifier, expedition_id, data, parent_identifier) VALUES ".format(project_id)

    for row in data:
        row['identifier'] = "{}_{}".format(row['urn:materialSampleID'], row['marker'])
        sql += "('{}', {}, '{}'::jsonb, '{}'), ".format(
            row['identifier'],
            expedition_id,
            json.dumps(row),
            row['urn:materialSampleID']
        )

    sql = sql[:-2] + " ON CONFLICT (local_identifier, expedition_id) DO NOTHING"
    cursor.execute(sql)
    psql.commit()
    cursor.close()


def data_to_worksheets(old_data, config):
    data = {}

    if 'fasta' in old_data:
        data['fastaSequence'] = old_data['fasta']
    if 'fastq' in old_data:
        data['fastqMetadata'] = old_data['fastq']

    sheet_attributes = {}
    for entity in config['entities']:
        if entity['conceptAlias'] in ['fastaSequence', 'fastqMetadata', 'Sample_Photo', 'Event_Photo']:
            continue
        if not entity['worksheet']:
            continue
        if not entity['worksheet'] in sheet_attributes:
            sheet_attributes[entity['worksheet']] = []
        sheet_attributes[entity['worksheet']].extend(entity['attributes'])

    sample = old_data['sample']
    for s in sample:
        for sheet in sheet_attributes:
            mapping = COLUMN_MAPPING[sheet] if sheet in COLUMN_MAPPING else None
            val_mapping = VALUE_MAPPINGS[sheet] if sheet in VALUE_MAPPINGS else None
            ed = {}
            for attribute in sheet_attributes[sheet]:
                col = attribute['column']

                def val(v):
                    if col == 'coordinateUncertaintyInMeters' and v == 'NA':
                        v = ''
                    elif col == 'monthCollected' or col == 'monthIdentifier':
                        if v.lower() in MONTH_MAP:
                            v = MONTH_MAP[v.lower()]

                    if attribute['dataType'] == 'FLOAT':
                        # converts sci-notation
                        try:
                            if v and float(v) == int(float(v)):
                                v = int(float(v))
                            else:
                                v = float(v)
                        except ValueError:
                            # catch error if string can't be converted to a float
                            if v.endswith(' m'):
                                # special case in dipnet data
                                return val(v.replace(' m', ''))
                    if attribute['dataType'] == 'INTEGER':
                        # if v is FLOAT and we can convert to int w/o rounding, do it
                        # also converts sci-notation
                        try:
                            if v and float(v) == int(float(v)):
                                v = int(float(v))
                        except ValueError:
                            # catch error if string can't be converted to a float
                            if v.endswith('M'):
                                # special case in dipnet data
                                return val(v.replace('M', ''))
                            elif v.endswith(' m'):
                                # special case in dipnet data
                                return val(v.replace(' m', ''))

                    if val_mapping and col in val_mapping and v in val_mapping[col]:
                        return val_mapping[col][v]
                    return v


                if mapping and col in mapping:
                    ed[col] = val(s[mapping[col]])
                elif col in s:
                    ed[col] = val(s[col])
                elif col == 'yearCollected':
                    ed[col] = 'Unknown'
            if sheet not in data:
                data[sheet] = []

            data[sheet].append(ed)

    return data


def data_to_filelike_csv(data):
    # header
    str = ",".join(data[0].keys())
    str += "\n"

    for d in data:
        str += "{}\n".format(",".join([json.dumps(v) for v in d.values()]))

    return io.StringIO(str)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description='Migrate geome data from single entity config to a multi entity config')
    parser.add_argument("--project_id", help="The old id of the project we are migrating", required=True)
    parser.add_argument("--access_token", help="Access Token of the user to upload under", required=True)
    parser.add_argument("--psql_connection_string",
                        help="psycopg2 connection info for the postgres db. Ex. 'host=biscicol4.acis.ufl.edu user=bisicoldev password=pass dbname=biscicoldev", required=True)
    parser.add_argument("--mysql_user", help="mysql db user", required=True)
    parser.add_argument("--mysql_password", help="mysql db password", required=True)
    parser.add_argument("--mysql_host", help="mysql db host", required=True)
    parser.add_argument("--mysql_db", help="mysql db name", required=True)
    parser.add_argument("--old_entities", help="entites in the old config in order to migrate the entity identifiers",
                        nargs='+', required=True)
    parser.add_argument("--bcid_client_id", help="client_id for the bcid system")
    parser.add_argument("--bcid_client_secret", help="client_secret for the bcid system")
    parser.add_argument("--config_file", help="Project configuration JSON to set the project to before uploading data")
    parser.add_argument("--accept_warnings", help="Continue to upload any data with validation warnings", default=False)
    args = parser.parse_args()

    psql = psycopg2.connect(args.psql_connection_string)
    mysql = connector.connect(user=args.mysql_user, passwd=args.mysql_password, host=args.mysql_host, db=args.mysql_db,
                              buffered=True)

    migrate_project(psql, mysql, args.project_id, args.access_token, args.old_entities, args.bcid_client_id,
                    args.bcid_client_secret, args.config_file, args.accept_warnings)
