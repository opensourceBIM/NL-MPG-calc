# NL-MPG-CALC

This repository contains the sourcecode for a bimserver plugin that will automatically determine an MPG score for any Ifc model. It does so by applying several steps:
* Collect the objects in an Ifc model (geoemtry and material/classification information)
* Create a bill of materials using the BimMapService in [the BBLIntegration repository](https://github.com/TNOBIM/BBLintegration) 
* Determine what materials match to which NMD product card based on a string matching algorithm
* Retrieve NMD product cards
* Calculate an MPG score for the full ifc Model

Both the 3rd and last step can be returned as Json which can be used for further processing. 



### Dependencies
There are several dependencies that need to be covered before running the plugin
* a bimserver instance (checked out repository or a jar/war)
* Access to a local or remote Set up of the NMD Data Service. Clone the repository and go through the rest of this README to setup the necessary components
* access to a running and prepopulated version of the BimMapService. this can be locally or any remote service that you have a connection string for.  Populating the BimMapService will be covered in the MapRunner section of this document. If you want to set it up yourself: Clone the repository and go through the rest of this README to setup the necessary components
* access to a running voxel server instance to have it calculate gross floor area.
How each of these items needs to be set up will be described in the next sections.

## Setting up an development environment

### Prerequisites
The development environment has a few minimum requirements as the system requires quite a bit of memory. It is advised to have a minimum of 16GB memory and and a recent i5 processor. 

Moreover, to make life easy it would be good to get into possession several files that cannot be uploaded to github due to proprietary rights.:
* a config.xml file for the NMD connection parameters
* a NMD2 sqlite .db file 
* the reference files used for the map service.

These files are only required when you are installing the dataservices yourself. (opposed to using remote services).
All these files are available for developers from the repository admin.

### COTS software
Before we start working on setting up the code base and fixing all dependencies let's first install all the commercial software required:

* Eclipse with openjdk - used for general java development.
* MySql Workbench - Can also be a bare MySql installation, but I find this easier to manage and inspect. used for the mapping data service repository. can be downloaded [here](https://www.mysql.com/products/workbench/)

In the BimServer documentation it states how to set up the eclipse client with the maven to eclipse plugin. The openjdk that has been used for development can be downloaded [here](https://jdk.java.net/12/). Setting a custom jdk can be done in a few simple steps (From stackoverflow):

* manage the list of available compilers in the Window -> Preferences -> Java -> Installed JRE's tab.
* In the project build path configuration dialog, under the libraries tab, you can delete the entry for "JRE System Library" 
* click on "Add Library" and choose the installed JRE to compile with. 
* The jdk 12 is compatible for java 8 and 11 builds and therefore will support any current bimserver builds.

## Setup of plugin for bimserver
* To get a running version of BimServer please refer to the BimServer docs [here](https://github.com/opensourceBIM/BIMserver)
* make sure the pom.xml dependencies in this repository are refering to the version of your bimserver jar/war/repository. That means that the versions checked out should refer to each other in the pom.xml files. Best approach is to check out versions of the latest release (shown by the tags). Repositories required are:
    * BIMServer
    * BIMViews
    * BinarySerializers
    * console
    * IfcOpenShell-BIMServer-plugin
    * IfcPlugins
    * BIMserver-JavaScript-API
    * BIMsurfer
* when running your bimserver instance make sure that the bouwbesluit plugin is added using the command line arguments described in the BimServer documentation. This does require setup of the bouwbesluit plugin

## The NMD Service
If you are to run the NMD service locally please clone [this repository](https://github.com/TNOBIM/NMD/) and follow the readme to run it. As this is a private repository you will either need to require access rights , or use an externally hosted NMD service.

In order to connect to a locally hosted NMD service you are required to add a config.xml file (for NMD3.0 tokens and NMD2.2 db location) in the root directory of the NL-MPG-CALC *and* the root directory of the NMD Service  repositories. An example file with the structure of the xml has been provided in the examples folder of this repository.

Currently we make use of the NMD2.2 connection as the 3.0 version is not ready for full load testing yet. However, the problem with the 2.2 version is that the user needs its own sqlite *.db file hosted locally in the config data folder. However, the disadvantage of the NMD2.2 version is that the interface is only partially implemented and therefore does not return representative values for product cards. This has been intentionally left as 'to do' as we are currently not planning for the 2.2 version to be used in production stages.

### setting up config.xml
As the config should contain an access token to the NMD we cannot push this to the repository. In order to get a valid access token (and config file) please contact this repository administrator. The valid config file should be placed in the root directory of the NMDService repository.

### NMD2 data
The NMD2 data has been used during development for having 'some' data availlable. it is advised to switch to the NMD3 data to get more representative data, but it will be supported for now.

The NMD2 data is available through a Sqlite.db file. This .db file should be available together with the config file and you should make sure that this file is located in the relative path defined in the config.xml file.

Please note that this data is in no way usable for actual MPG calculations, but can only be used for quick and dirty front-end testing.

### NMD3 data - Caching
The NMD 3 data service relies on an external API to retrieve its data. Due to the delay this external service has, a persistent cache is created of every httpget request done on this service. The cache service creates a md5 hash of the request string and stores this in a local temp folder.

This cache can be recreated or directly imported from an earlier generated cache.

**To recreate the cache**
* run the NMD3IntegrationTests. one of the tests will basically pull the entire database (such that it gets cached).

**To import an existing cache (Windows)**
* get an earlier NMD3Cahce folder and zip it. (could already be provided)
* Unzip the folder in the `<UserFolder>/AppData/Local/Temp/` such that it now contains a `NMD3cache` folder with many files with md5 hashes as name.

For other operating systems this folder should be created in the equivalent of the users auto create Temp folder.

## The Mapping Service
The Mapping service is used for two purposes:
* Retrieve and store reference data for facilitating the plugin to make an initial estimate which NMD product cards match on which ifc objects.
* Retrieve and store user selected material mappings.

The storage and retrieval of user mappings will be performed automatically when running the Ifc Collection and MPG calculation Dataservices. How to run this service is described [here](https://github.com/TNOBIM/BBLintegration).

To store the reference data it will require some manual preparation and should be done first to get accurate material to NMD mappings.

### prepopulating the mapping service
To read in the reference data for the mapping service, make sure:
* The MySql database service is created and started.
* The application.properties file in the resources section of the repository points to the correct database. (default set to local db with default port).
* you can connect to your instance of the mappingservice
* run the main method from the `MapRunner` class (in the `org.opensourcebim.mapping` package) you can do so via your IDE or use the command line:

```
>> java -cp . MapRunner -root=<pathToRootFolder> -words=<relativePathToWordsCsvFile> -nlsfb=<relativePathToNlsfbAlternativesFile> <OPTIONAL>-keyWords
```

The minimal viable product will be a mapping database with at least the NLsfb alternative mappings loaded. If this is not the case you can expect a large amount of elements to miss a mapping. 

Should it not work for you please check the following common errors:
* make sure the MapRunner class is on your classpath 
* make sure a -root argument is defined. (the console will return an error in this case)
* When running the MapRunner make sure a mappingService is running and the MySql database server (if run locally) has started.

an example could be: 
```
>> java -cp . MapRunner -root="C://myRootFolder//" -words="path//to//csv//file//words.csv"
```
In this case only the words file at location `C://myRootFolder//path//to//csv//file//words.csv` will be read and processed.

The mapRunner performs a series of post requests on the mapservice
* if the `-words` flag is present it will write the commonwords csv data defined in the argument to the service
* if the `-keyWords` flag is present (no argument required) it will write the material keywords to the service
* if the `-nlsfb` flag is present it will write the nlsfb to ifcproduct type mappings file in the argument to the service

Take care with the keywords argument that this can be a very lengthy process. The MapRunner will try to find all .ifc files in the -root argument folder and generate  a wordcount of all the words found in IfcMaterial objects in all these files.

Each of these steps require a .csv file that will be read in and parsed to the desired objects. Examples for these csv files can be found in the examples folder in this repository.


## BouwBesluit plugin
This requires a few simple steps:

* Clone the latest version of the repository from [here](https://https://github.com/opensourceBIM/NL-MPG-calc).
* Make sure the dependencies to the BimServer repositories listed above is up to date. This can be done by checking the pom.xml file and making sure the versions of the org.opensourcebim dependencies match with the checked out versions.

* Make sure that the versions of the NMD and Mapping service dependencies match up with the checked out versions similar to the org.opensourcebim dependencies. 

