#!/usr/bin/python
import ast
import json
import csv
import requests
import sys
import os
import getopt
from pprint import pprint

basisOfRecord = 'PreservedSpecimen'
username = ''
password = ''
outputfile = ''
rowNum = 1
reload(sys)
sys.setdefaultencoding('utf8')


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

def createDynamicProperties(weight,length):
    values =[]
    retValue = ''

    if weight is not None:
        weight = weight.strip()
    else: 
        weight = ''
    if length is not None:
        length = length.strip()
    else:
        length = ''

    if (weight != ''):
        values.append('\'weightInGrams\':\''+weight+'\'')
    if (length!= ''):
        values.append('\'lengthInCentimeters\':\''+length+'\'')

    if (len(values) == 0):
        return ''
    else:
        retValue += '{'
        count = 0
        for s in values:
            if (count):
                retValue += ','
            count += 1
            retValue += values[0]
        retValue += '}'

        return retValue


#Friendly method for creating dates
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


# Function for writing expeditionCode output
def writeOutput(expeditionCode,csvfile,page,auth):
    global basisOfRecord
    global rowNum
    #if page == 0:
#	# the list of fields in this header must match the list of fields in the list, below
#    	header = [ 'materialSampleID','dcterms_references','expeditionCode', 'associatedMedia', 'associatedReferences', 'associatedSequences', 'associatedTaxa', 'basisOfRecord', 'basisOfIdentification', 'class', 'coordinateUncertaintyInMeters', 'country', 'dateCollected', 'dateIdentified', 'decimalLatitude', 'decimalLongitude', 'dynamicProperties', 'establishmentMeans', 'eventRemarks', 'extractionID', 'family', 'fieldNotes', 'fundingSource', 'geneticTissueType', 'genus', 'georeferenceProtocol', 'habitat', 'identifiedBy', 'island', 'islandGroup', 'lifeStage', 'locality', 'maximumDepthInMeters', 'maximumDistanceAboveSurfaceInMeters', 'microHabitat', 'minimumDepthInMeters', 'minimumDistanceAboveSurfaceInMeters',  'occurrenceID', 'occurrenceRemarks', 'order', 'permitInformation', 'phylum', 'plateID', 'preservative', 'previousIdentifications', 'previousTissueID', 'principalInvestigator', 'recordedBy', 'sampleOwnerInstitutionCode', 'samplingProtocol', 'sequence', 'sex', 'species', 'stateProvince', 'subSpecies', 'substratum', 'taxonRemarks', 'tissueStorageID', 'vernacularName',  'wellID', 'wormsID'  ]
#       csvfile.writerow(header)
    nextPage = page + 1
    url = "http://biscicol.org/dipnet/rest/v1.1/projects/query/json/?limit=10&page="+str(page)
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
	    # construct a list so we can use the csv file writer
	    try:
                list = [ rowNum, row['bcid'], expeditionCode, row['associatedMedia'], row['associatedReferences'], row['associatedSequences'], row['associatedTaxa'], row['basisOfIdentification'], basisOfRecord, row['class'], row['coordinateUncertaintyInMeters'], row['country'], createDate(row['yearCollected'],row['monthCollected'],row['dayCollected']),createDate(row['yearIdentified'],row['monthIdentified'],row['dayIdentified']), createDynamicProperties(row['weight'],row['length']),"%.6f" %float(row['decimalLatitude']), "%.6f" %float(row['decimalLongitude']), row['establishmentMeans'], row['eventRemarks'], row['extractionID'], row['family'], row['fieldNotes'], row['fundingSource'], row['geneticTissueType'], row['genus'], row['georeferenceProtocol'], row['habitat'], row['identifiedBy'], row['island'], row['islandGroup'], row['lifeStage'], row['locality'], row['maximumDepthInMeters'], row['maximumDistanceAboveSurfaceInMeters'], row['microHabitat'], row['minimumDepthInMeters'], row['minimumDistanceAboveSurfaceInMeters'], row['occurrenceID'], row['occurrenceRemarks'], row['order'], row['permitInformation'], row['phylum'], row['plateID'], row['preservative'], row['previousIdentifications'], row['previousTissueID'], row['principalInvestigator'], row['recordedBy'], row['sampleOwnerInstitutionCode'], row['samplingProtocol'], row['sex'], row['species'], row['stateProvince'], row['subSpecies'], row['substratum'], row['taxonRemarks'], row['tissueStorageID'], row['vernacularName'], row['wellID'], row['wormsID']  ]	
            	csvfile.writerow(list)
                rowNum = rowNum + 1
	    except ValueError:
		print "\tskipping bad data in line"
	# call writeOutput for the next page 
   	writeOutput(expeditionCode,csvfile,nextPage,auth)

if __name__ == "__main__":
   main(sys.argv[1:])

# Authenticate
url = 'http://biscicol.org/dipnet/rest/v1.1/authenticationService/login'
payload = {'username': username, 'password': password}
auth = requests.post(url, data=payload )

#expeditionCode = "acaach_CyB_JD"
#outputFilename = "output.csv" 

# truncate file (opening the file with w+ mode truncates the file)
f = open(outputfile, "w+")
f.close()

# open file as csv file for writing
csvWriter =  csv.writer(open(outputfile, "wb+"))

# Return Graphs
url = 'http://biscicol.org/dipnet/rest/v1.1/projects/25/graphs'
r = requests.get(url, cookies=auth.cookies)

# Loop expeditions
for expedition in r.json():
    # Get Expedition Code
    if not ("TEST") in expedition["expeditionCode"]:
    	print "processing " + expedition["expeditionCode"]
    	writeOutput(expedition["expeditionCode"], csvWriter, 0, auth)
