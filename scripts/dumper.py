#!/usr/bin/python
import ast
import json
import csv
import requests
import sys
import os
import getopt
from pprint import pprint

# script for setting up Darwin Core Archive for GeOMe data
# Run this script and generates CSV output file which can then
# be called using the mysql "LOAD DATA INFILE ..." function
basisOfRecord = 'PreservedSpecimen'
username = ''
password = ''
outputfile = ''
rowNum = 1
reload(sys)
sys.setdefaultencoding('utf8')
dynamicValues =[]

# run program with arguments
def main(argv):
   global username 
   global password
   global outputfile
   usage = 'dumper.py -u <username> -p <password> -o <outputfile>'
   try:
      opts, args = getopt.getopt(argv,"u:p:o:h",["username=","password=","outputfile="])
   except getopt.GetoptError:
      print usage 
      sys.exit(2)
   for opt, arg in opts:
      if opt == '-h':
      	 print usage 
         sys.exit()
      elif opt in ("-u", "--username"):
         username = arg
      elif opt in ("-p", "--password"):
         password = arg
      elif opt in ("-o", "--outputfile"):
         outputfile= arg
   if username == '' or password == '' or  outputfile == '':
   	print usage
	sys.exit(2)

# add an individual dynamic property, ensuring there is data and formatting JSON
def addDynamicProperty(key,value):
    global dynamicValues
    if value is not None:
        value = value.strip()
        if value != '':
            dynamicValues.append('\'' +key + '\':\''+value+'\'');

# construct the dynamic properties elements, which are fields of interest but not directly mapped to darwin core terms
def createDynamicProperties(weight,length,extractionID,fundingSource,geneticTissueType,microHabitat,permitInformation,previousTissueID,principalInvestigator,substratum,tissueStorageID,wormsID,bcid):
    global dynamicValues
    retValue = ''

    addDynamicProperty('weightInGrams',weight)
    addDynamicProperty('lengthInCentimeters',length)
    addDynamicProperty('extractionId',extractionID)
    addDynamicProperty('fundingSource',fundingSource)
    addDynamicProperty('geneticTissueType',geneticTissueType)
    addDynamicProperty('microHabitat',microHabitat)
    addDynamicProperty('permitInformation',permitInformation)
    addDynamicProperty('previousTissueID',previousTissueID)
    addDynamicProperty('principalInvestigator',principalInvestigator)
    addDynamicProperty('substratum',substratum)
    addDynamicProperty('tissueStorageID',tissueStorageID)
    addDynamicProperty('wormsID',wormsID)
    addDynamicProperty('bcid',bcid)

    if (len(dynamicValues) == 0):
        return ''
    else:
        retValue += '{'
        count = 0
        for s in dynamicValues:
            if (count):
                retValue += ','
            retValue += dynamicValues[count]
            count += 1
        retValue += '}'

        return retValue

# Create darwin core compliant date fields
def createDate(year,month,day):
    if year is not None:
        year = year.strip()
    else:
        year = ''
    if month is not None:
        month = month.strip()
    else:
        month = ''
    if day is not None:
        day = day.strip()
    else:
        day = '';

    retValue = ''
    if year == '':
        return retValue
    if month == '':
        return year
    if day == '':
        return year +'-'+month
    else: 
        return year+'-'+month+'-'+day


# write output for each expedition
def writeOutput(expeditionCode,csvfile,page,auth):
    global basisOfRecord
    global rowNum
    global dynamicValues
    nextPage = page + 1
    url = "http://geome-db.org/rest/v1.1/projects/query/json/?limit=10&page="+str(page)
    payload = {'expeditions':expeditionCode}
    response = requests.post(url, data=payload, cookies=auth.cookies)
    response.encoding = 'utf8'
    # convert response to json
    text = response.text
    j = json.loads(text)
    #import pdb;pdb.set_trace()
    totalPages = j['totalPages']
    #print str(page) + " of " + str(totalPages)

    # proceed if there are additional pages
    if (nextPage <= totalPages):
	# loop each row
    	for row in j['content']:
            # initialize the dynamicValues array
            dynamicValues = []
	    # construct a list so we can use the csv file writer
	    try:
                list = [
                        'http://n2t.net/'+row['bcid'], # occurrenceID 
                        'http://n2t.net/'+row['bcid'], # materialSampleID 
                        expeditionCode, # collectionCode
                        row['associatedMedia'], 
                        row['associatedReferences'], 
                        row['associatedSequences'], 
                        row['associatedTaxa'], 
                        row['basisOfIdentification'], # identificationRemarks
                        basisOfRecord, # basisOfRecord
                        row['materialSampleID'],  # catalogNumber 
                        row['class'], 
                        row['coordinateUncertaintyInMeters'], 
                        row['country'], 
                        createDate(row['yearCollected'], row['monthCollected'], row['dayCollected']), # dateCollected
                        row['yearCollected'], # year
                        row['monthCollected'], # month
                        row['dayCollected'], # day
                        createDate(row['yearIdentified'], row['monthIdentified'], row['dayIdentified']), #dateIdentified
                        createDynamicProperties(
                            row['weight'],
                            row['length'],
                            row['extractionID'], 
                            row['fundingSource'], 
                            row['geneticTissueType'], 
                            row['microHabitat'], 
                            row['permitInformation'], 
                            row['previousTissueID'], 
                            row['principalInvestigator'], 
                            row['substratum'], 
                            row['tissueStorageID'], 
                            row['wormsID'],
                            row['bcid']) ,  #dynamicProperties
                        "%.6f" %float(row['decimalLatitude']), 
                        "%.6f" %float(row['decimalLongitude']), 
                        row['establishmentMeans'], 
                        row['eventRemarks'], 
                        row['family'], 
                        row['fieldNotes'], 
                        row['genus'], 
                        row['georeferenceProtocol'], 
                        row['habitat'], 
                        row['identifiedBy'], 
                        row['sampleOwnerInstitutionCode'], # institutionCode
                        row['island'],
                        row['islandGroup'], 
                        row['lifeStage'], 
                        row['locality'], 
                        row['maximumDepthInMeters'], 
                        row['maximumDistanceAboveSurfaceInMeters'], 
                        row['minimumDepthInMeters'], 
                        row['minimumDistanceAboveSurfaceInMeters'], 
                        row['occurrenceRemarks'], 
                        row['order'], 
                        row['phylum'], 
                        row['preservative'], 
                        row['previousIdentifications'], 
                        row['recordedBy'], 
                        row['samplingProtocol'], 
                        row['sex'], 
                        row['species'], 
                        row['stateProvince'], 
                        row['subSpecies'], 
                        row['taxonRemarks'], 
                        row['vernacularName']
                        ]	
            	csvfile.writerow(list)
                rowNum = rowNum + 1
	    except ValueError:
		print "\tskipping bad data in line"
	# call writeOutput for the next page 
   	writeOutput(expeditionCode,csvfile,nextPage,auth)

if __name__ == "__main__":
   main(sys.argv[1:])

# set URL login endpoint
url = 'http://geome-db.org/rest/v1.1/authenticationService/login'
payload = {'username': username, 'password': password}
# authenticate
auth = requests.post(url, data=payload )

# truncate file (opening the file with w+ mode truncates the file)
f = open(outputfile, "w+")
f.close()

# open file as csv file for writing
csvWriter =  csv.writer(open(outputfile, "wb+"))

# get a list of expeditions for this project
url = 'http://geome-db.org/rest/v1.1/projects/25/graphs'
r = requests.get(url, cookies=auth.cookies)

# loop expeditions data and write to specified output file
for expedition in r.json():
    # get expedition code, excluding anything the word test
    if not ("TEST") in expedition["expeditionCode"]:
    	print "processing " + expedition["expeditionCode"]
    	writeOutput(expedition["expeditionCode"], csvWriter, 0, auth)
