#!/usr/bin/python
"""
backup your db before running this script
"""

import requests, re, psycopg2


def escape(s):
    return re.sub("[%:\r\n]", lambda c: "%%%02X" % ord(c.group(0)), s)


def update(psql, ezid_username, ezid_pass):
    entity_identifiers = get_entity_identifiers(psql)

    for (alias, i) in entity_identifiers:
        d = {
            '_target': "https://geome-db.org/record/{}".format(i),
            'dc.title': alias,
            'dc.type': alias,
            'dc.publisher': 'GeOMe-db FIMS'
        }
        anvl = "\n".join("%s: %s" % (escape(name), escape(value)) for name,
                                                                      value in d.items()).encode("UTF-8")
        res = requests.post("https://ezid.cdlib.org/id/{}".format(i), data=anvl, auth=(ezid_username, ezid_pass),
                            headers={'Content-type': 'text/plain; charset=utf-8'})
        res.raise_for_status()


def get_entity_identifiers(psql):
    cursor = psql.cursor()
    cursor.execute("SELECT concept_alias, identifier FROM entity_identifiers")

    identifiers = cursor.fetchall()

    cursor.close()
    return identifiers


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description='Update ezid url for all geome entity_identifiers')
    parser.add_argument("--psql_connection_string",
                        help="psycopg2 connection info for the postgres db. Ex. 'host=biscicol4.acis.ufl.edu user=bisicoldev password=pass dbname=biscicoldev",
                        required=True)
    parser.add_argument("--ezid_username", required=True)
    parser.add_argument("--ezid_pass", required=True)
    args = parser.parse_args()

    psql = psycopg2.connect(args.psql_connection_string)

    update(psql, args.ezid_username, args.ezid_pass)
