The BiSciCol FIMS (Field Information Management System) is a working instance of the Biocode FIMS software stack.  The BiSciCol FIMS utilizes an ElasticSearch data storage engine and is the first created instance of the [Biocode FIMS](https://github.com/biocodellc/biocode-fims-commons/) stack.  It is based on earlier validation software created for the Moorea Biocode Project and utilizes the following workflow: Validation -> Ingestion -> Triplification.  A working installation is at http://biscicol.org/.


#Installation
ElasticSearch installation installation procedure:
 - Download ES source code to /usr/local/src/{elasticsearch-version}
 - create symbolic link to executable: 
```ln -s /usr/local/src/elasticsearch-5.0.1/bin/elasticsearch /usr/local/elasticsearch```
 - create elasticsearch user (with nologin priveleges)
 - create /var/run/elasticsearch and assign to elasticsearch user
 - create a configuration directory (e.g. /opt/esconfig/)
 - create a data directory (e.g. /opt/esdata/)
 - move ES configuration files to /opt/esconfig
 - edit elasticsearch.yml in configuration directory to point to specified data directory
 - create a start/stop daemonizer script in /etc/init.d/ for elasticsearch, running as user elasticsearch and creating pid file in /var/run/elasticsearch
