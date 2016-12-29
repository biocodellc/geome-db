#!/usr/bin/python
import ast
import json
import csv
import requests
import sys
import os
import getopt
from pprint import pprint

username = ''
password = ''
outputfile = ''
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

# Function for writing expeditionCode output
def writeOutput(expeditionCode,csvfile,page,auth):
    #if page == 0:
#	# the list of fields in this header must match the list of fields in the list, below
#    	header = [ 'materialSampleID','expeditionCode', 'associatedMedia', 'associatedReferences', 'associatedSequences', 'associatedTaxa', 'basisOfIdentification', 'class', 'coordinateUncertaintyInMeters', 'country', 'dayCollected', 'dayIdentified', 'decimalLatitude', 'decimalLongitude', 'establishmentMeans', 'eventRemarks', 'extractionID', 'family', 'fieldNotes', 'fundingSource', 'geneticTissueType', 'genus', 'georeferenceProtocol', 'habitat', 'identifiedBy', 'island', 'islandGroup', 'length', 'lifeStage', 'locality', 'maximumDepthInMeters', 'maximumDistanceAboveSurfaceInMeters', 'microHabitat', 'minimumDepthInMeters', 'minimumDistanceAboveSurfaceInMeters', 'monthCollected', 'monthIdentified', 'occurrenceID', 'occurrenceRemarks', 'order', 'permitInformation', 'phylum', 'plateID', 'preservative', 'previousIdentifications', 'previousTissueID', 'principalInvestigator', 'recordedBy', 'sampleOwnerInstitutionCode', 'samplingProtocol', 'sequence', 'sex', 'species', 'stateProvince', 'subSpecies', 'substratum', 'taxonRemarks', 'tissueStorageID', 'vernacularName', 'weight', 'wellID', 'wormsID', 'yearCollected', 'yearIdentified' ]
#       csvfile.writerow(header)
    nextPage = page + 1
    url = "http://biscicol.org/dipnet/rest/v1.1/projects/query/json/?limit=10&page="+str(page)
    payload = {'expeditions':expeditionCode}
    response = requests.post(url, data=payload, cookies=auth.cookies)
    response.encoding = 'utf8'
    # convert response to json
    text = response.text
    j = json.loads(text)
    totalPages = j['totalPages']
    #print str(page) + " of " + str(totalPages)

    # proceed if there are additional pages
    if (nextPage <= totalPages):
	# loop each row
    	for row in j['content']:
	    # construct a list so we can use the csv file writer
	    try:
	    	list = [ row['materialSampleID'],expeditionCode, row['associatedMedia'], row['associatedReferences'], row['associatedSequences'], row['associatedTaxa'], row['basisOfIdentification'], row['class'], row['coordinateUncertaintyInMeters'], row['country'], row['dayCollected'], row['dayIdentified'], "%.6f" %float(row['decimalLatitude']), "%.6f" %float(row['decimalLongitude']), row['establishmentMeans'], row['eventRemarks'], row['extractionID'], row['family'], row['fieldNotes'], row['fundingSource'], row['geneticTissueType'], row['genus'], row['georeferenceProtocol'], row['habitat'], row['identifiedBy'], row['island'], row['islandGroup'], row['length'], row['lifeStage'], row['locality'], row['maximumDepthInMeters'], row['maximumDistanceAboveSurfaceInMeters'], row['microHabitat'], row['minimumDepthInMeters'], row['minimumDistanceAboveSurfaceInMeters'], row['monthCollected'], row['monthIdentified'], row['occurrenceID'], row['occurrenceRemarks'], row['order'], row['permitInformation'], row['phylum'], row['plateID'], row['preservative'], row['previousIdentifications'], row['previousTissueID'], row['principalInvestigator'], row['recordedBy'], row['sampleOwnerInstitutionCode'], row['samplingProtocol'], row['sex'], row['species'], row['stateProvince'], row['subSpecies'], row['substratum'], row['taxonRemarks'], row['tissueStorageID'], row['vernacularName'], row['weight'], row['wellID'], row['wormsID'], row['yearCollected'], row['yearIdentified'] ]	
            	csvfile.writerow(list)
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
