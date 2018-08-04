#!/usr/bin/python

import sys, argparse, requests, json
from os import listdir, path
from os.path import isfile, join

ENDPOINT = 'https://api.develop.geome-db.org/'

EVENTS_FILE = "{}_Collecting_Events.txt"
SPECIMENS_FILE = "{}_Specimens.txt"
TISSUES_FILE = "{}_Tissues.txt"

headers = {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
}


def upload_files(project_id, access_token, dir):
    #expeditions = {f.split('_')[0] for f in listdir(dir)}
    expeditions = {f.replace("_Specimens.txt","").replace("_Collecting_Events.txt","").replace("_Tissues.txt","") for f in listdir(dir)}

    for code in expeditions:
        if not isfile(join(dir, EVENTS_FILE.format(code))) or \
                not isfile(join(dir, SPECIMENS_FILE.format(code))) \
                or not isfile(join(dir, TISSUES_FILE.format(code))):
            raise Exception("Could not find all 3 required files for expedition: {}".format(code))

    for code in expeditions:
        create_expedition(project_id, code, access_token)
        upload_data(project_id, code, access_token, dir)


def create_expedition(project_id, code, access_token):
    print('\n\nAttempting to create expedition: ', code)

    url = "{}projects/{}/expeditions/{}?access_token={}".format(ENDPOINT, project_id, code, access_token)
    expedition = {
        'expeditionTitle': code,
        'expeditionCode': code,
        'visibility': 'anyone',
        'public': True,
        'metadata': {},
    }
    response = requests.post(url, json=expedition, headers=headers)

    if response.status_code == 400 and response.json().get('usrMessage').find('already exists.') > -1:
        print("Expedition \"{}\" already exists".format(code))
    elif response.status_code > 400:
        print('\nERROR: ' + response.json().get('usrMessage'))
        print('\n')
        response.raise_for_status()
    else:
        print('Successfully created expedition: ', code)


def upload_data(project_id, code, access_token, base_dir):
    print('Attempting to upload data for expedition: ', code)

    validate_url = "{}data/validate?access_token={}".format(ENDPOINT, access_token)

    data = {
        'projectId': project_id,
        'expeditionCode': code,
        'upload': True,
        'reloadWorkbooks': True,
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
                "reload": True,
                "metadata": {
                    "sheetName": "Events"
                }
            },
            {
                "dataType": 'TABULAR',
                "filename": "Specimens.txt",
                "reload": True,
                "metadata": {
                    "sheetName": "Samples"
                }

            },
            {
                "dataType": 'TABULAR',
                "filename": 'Tissues.txt',
                "reload": True,
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

    if 'messages' in response:
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
    elif not response.get('isValid'):
        cont = raw_input("Warnings found during validation. Would you like to continue? (y/n)   ")
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
        description='Upload a directory of files exported from the old Biocode db. '
                    'We will create an expedition if necessary. We expect 3 files for each expedition. '
                    'One for Events, Samples, and Tissues')
    parser.add_argument("project_id", help="The id of the project we're uploading")
    parser.add_argument("access_token", help="Access Token of the user to upload under")
    parser.add_argument("dir",
                        help="Directory containing the files to upload. The file prefix will be the expedition. "
                             "For each expedition, we expect 3 files PREFIX_Collecting_Events.txt, "
                             "PREFIX_Specimens.txt, and PREFIX_Tissues.txt")
    args = parser.parse_args()

    upload_files(args.project_id, args.access_token, args.dir)
