#!/usr/bin/python
from elasticsearch import Elasticsearch
import elasticsearch.helpers

es = Elasticsearch()
query = {
    "query": {
        "nested": {
            "path": "fastaSequence",
            "query": {
                "bool": {
                    "must": [
                        {"query_string": {"query": "+fastaSequence.urn\\:marker.keyword:\"CyB\""}}
                    ]
                }
            }
        }
    }
}

res = es.search(index="25", doc_type="resource", body=query, size=10000)

to_update = []

for doc in res['hits']['hits']:
    source = doc['_source']
    for s in source['fastaSequence']:
        if s['urn:marker'] == 'CyB':
            s['urn:marker'] = 'CYB'

    del doc['_score']
    doc['_op_type'] = 'update'
    doc['doc'] = {
        "fastaSequence": source['fastaSequence']
    }
    del doc['_source']
    to_update.append(doc)

update_response = elasticsearch.helpers.bulk(es, to_update)

if len(to_update) != update_response[0]:
    print("possible that not all docs successfully updated")
