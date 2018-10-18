#!/usr/bin/python

import argparse, csv, json


def transform(file):
    data = []
    with open(file, 'r+') as export_file:
        reader = csv.DictReader(export_file)
        for r in reader:
            data.append(update(r))

        export_file.seek(0)
        writer = csv.DictWriter(export_file, fieldnames=data[0].keys())
        writer.writeheader()
        for d in data:
            writer.writerow(d)


def update(data):
    data['occurrenceID'] = "https://n2t.net/{}".format(data['Sample_bcid'])
    data['catalogNumber'] = data['materialSampleID']
    data['materialSampleID'] = "https://n2t.net/{}".format(data['Sample_bcid'])
    data['dateCollected'] = create_date(data['yearCollected'], data['monthCollected'], data['dayCollected'])
    data['dateIdentified'] = create_date(data['yearIdentified'], data['monthIdentified'], data['dayIdentified'])
    if data['decimalLatitude']:
        data['decimalLatitude'] = "%.6f" % float(data['decimalLatitude'])
    if data['decimalLongitude']:
        data['decimalLongitude'] = "%.6f" % float(data['decimalLongitude'])
    add_dynamic_properties(data)

    return data


# construct the dynamic properties elements, which are fields of interest but not directly mapped to darwin core terms
def add_dynamic_properties(data):
    props = {}

    def add_prop(key):
        if data[key] is not None and data[key].strip() != '':
            props[key] = data[key].strip()

    add_prop('weight')
    add_prop('length')
    add_prop('fundingSource')
    add_prop('tissueType')
    add_prop('microHabitat')
    add_prop('permitInformation')
    add_prop('tissueCatalogNumber')
    add_prop('principalInvestigator')
    add_prop('substratum')
    add_prop('tissueBarcode')
    add_prop('wormsID')
    props['bcid'] = data['Sample_bcid'].strip()

    data['dynamicProperties'] = '' if len(props) == 0 else json.dumps(props)


# Create darwin core compliant date fields
def create_date(year, month, day):
    if year is not None:
        year = year.strip()
    if not year:
        return ''

    if month is not None:
        month = month.strip()
    if not month:
        return year

    if day is not None:
        day = day.strip()
    if not day:
        return "{}-{}".format(year, month)

    return "{}-{}-{}".format(year, month, day)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description='Transform a csv export into the appropriate gbif data')
    parser.add_argument("export_file", help="The csv export from GeOMe")
    args = parser.parse_args()

    transform(args.export_file)
