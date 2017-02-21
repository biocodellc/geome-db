Process for Submitting GeOMe data to Genbank

Current Status
#################################

 * Currently support legacy sequence data via fasta uploader.
 * Genbank submission will be via the bioproject interface.
   * This process is outlined below.
 
Step 1: FILE GENERATION
#################################
  
* This involves generating the FASTQ files and a submission.xml file.
* The submission.xml file is where we can create/modify biosamples and link the biosample to the FASTQ file.
* We have a couple of options here:
    1. We can generate the files in batchs. This would scan the db looking in the associatedSequence attribute field for data without accession numbers.
      * can be a weekly, monthly, etc automated process
      * or can be initiated via the FIMS web interface.
    2. We can generate the files on a per expedition basis via the expedition manager in FIMS
      * [Example](https://cloud.githubusercontent.com/assets/1154501/15022774/1844707a-11e2-11e6-9665-0324eec27138.png) 

Step 2: Submitting to Genbank SRA
#################################

* We will ftp the xml and FASTQ files to Genbank.
* Genbank will automatically begin ingesting the data
 
Step 3: GENBANK LINKER (FIMS INTEGRATION)
#################################

* This step runs automatically.  Daily polling of GeOMe Bioproject data from Genbank from FIMS end looking for ARK keys in the specimen_voucher qualifier field and updating the graph w/ the accession number.

Possible Tools and process to use for submitting legacy data to Genbank.
#################################
 * COI Tool: Only CO1
    http://www.ncbi.nlm.nih.gov/WebSub/?tool=barcode
    needs forward and reverse primers for EACH sample
    also needs title of Paper for EACH sample
 * 16S Tool: Only 16S
    https://submit.ncbi.nlm.nih.gov/subs/genbank/
 * BankIt (web based tool; seems to have limited utility)
 * Sequin (off-line tool; more analysis options)
    http://www.ncbi.nlm.nih.gov/Sequin/
    Allows any type of data -- only needs organism as required.
    This option seems to give us the most power for inserting structured comments and qualifiers as needed.
   	

Step 1: FILE GENERATION
#################################

1. Look for data w/out accession numbers in FIMS DB.  
This is stored in the associatedSequence attribute field.  We search for the string https://www.ncbi.nlm.nih.gov/nuccore/%  and must also have a sequence associated with it.

2.	Click a button on web-interface.  This runs a job that will generate all FASTa files and email the result (zipped/gzipped) to the email on file for the user account.


Step 2: MANUAL LOADING
#################################

1.	All submission types are focused on manual submission; e.g. web tool.
Anyone can submit data. This happens OUTSIDE of FIMS.

Step 3: GENBANK LINKER (FIMS INTEGRATION)
#################################
This step runs automatically.  Daily polling of Genbank nuccore db from FIMS end looking for ARK keys in the specimen_voucher qualifier field and updating the graph w/ the accession number.
