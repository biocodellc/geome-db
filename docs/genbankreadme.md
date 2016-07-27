Process for Submitting DIPNet data to Genbank

Current Status
#################################

 * Currently support legacy sequence data via fasta uploader.
 * Genbank submission will be via the bioproject interface.
   * This process is outlined below.
 

Step 1: FILE GENERATION
#################################

1.	Look for data w/out accession numbers in FIMS DB.  
This is stored in the associatedSequence attribute field.  We search for the string https://www.ncbi.nlm.nih.gov/sra/%  and must also have a sequence associated with it.

2.	Click a button on web-interface.  This runs a job that will generate all FASTQ files and email the result (zipped/gzipped) to the email on file for the user account.


Step 2: MANUAL LOADING
#################################

1.	All submission types are focused on manual submission; e.g. web tool.
Anyone can submit data. This happens OUTSIDE of FIMS.

Step 3: GENBANK LINKER (FIMS INTEGRATION)
#################################
This step runs automatically.  Daily polling of dipnet Bioproject data from Genbank from FIMS end looking for ARK keys in the specimen_voucher qualifier field and updating the graph w/ the accession number.

Possible Tools to use for submitting legacy data to Genbank.
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
   	

