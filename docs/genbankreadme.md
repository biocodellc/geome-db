Process for Submitting DIPNet data to Genbank

Here are possible Tools
 * COI Tool: Only CO1
    http://www.ncbi.nlm.nih.gov/WebSub/?tool=barcode
    needs forward and reverse primers; title of Paper for EACH sample
 * 16S Tool: Only 16S
    https://submit.ncbi.nlm.nih.gov/subs/genbank/
 * BankIt (web based tool)
 * Sequin (off-line tool; more tools)
    seems more powerful and easier.
    Allows any type of data but only needs organism as required.
   	
Step 1: FILE GENERATION
#################################

1.	Look for data w/out accession numbers in FIMS DB.  
This is stored in the associatedSequence attribute field.  We search for the string http://www.ncbi.nlm.nih.gov/nuccore/%  and must also have a sequence associated with it.

2.	Click a button on web-interface.  This runs a job that will generate a file (e.g. Sequin) and email response to the email on file for the user account.


Step 2: MANUAL LOADING
#################################

1.	All submission types are focused on manual submission; e.g. email, web tool.
Anyone can submit data 

Step 3: FIMS INTEGRATION
#################################
Daily polling of data from Genbank looking for ARK keys in the specimen_voucher qualifier field and updating the graph w/ the accession number.
