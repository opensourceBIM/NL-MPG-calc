# NL-MPG-CALC

This repository contains the sourcecode for a bimserver plugin that will automatically determine an MPG score for any Ifc model. It does so by applying several steps:
* Collect the objects in an Ifc model (geoemtry and material/classification information)
* Create a bill of materials using the BimMapService in [the BBLIntegration repository](https://github.com/TNOBIM/BBLintegration) 
* Determine what materials match to which NMD product card based on a string matching algorithm
* Retrieve NMD product cards
* Calculate an MPG score for the full ifc Model

Both the 3rd and last step can be returned as Json which can be used for further processing. 

### Dependencies
There are several dependencies that need to be covered
* a bimserver instance (checked out repository or a jar/war)
* access to the nmd repository. either a local (Sqlite) 2.2 version or a token and connection string to the 3.0 version. 
* access to a running and prepopulated version of the BimMapService. this can be locally or any remote service that you have a connection string for. 

How each of these items needs to be set up will be described in the next sections.

## Setup of plugin for bimserver

* To get a running version of BimServer please refer to the BimServer docs [here](https://github.com/opensourceBIM/BIMserver)
* make sure the pom.xml dependencies in this repository are refering to the version of your bimserver jar/war/repository 
* when running your bimserver instance make sure that this plugin is added using the command line arguments described in the BimServer documentation

## The NMD Service
If you are to run the NMD service locally please clone [this repository](https://github.com/TNOBIM/NMD/) and follow the readme to run it. As this is a private repository you will either need to require access rights , or use an externally hosted NMD service.

In order to connect to a locally hosted NMD service you are required to add a config file (for NMD3.0 tokens and NMD2.2 db location) in the root directory of the NL-MPG-calc repository (= the current repository) 

Currently we make use of the NMD2.2 connection as the 3.0 version is not ready for full load testing yet. However, the problem with the 2.2 version is that the user needs its own sqlite *.db file hosted locally in the config data folder. 

The disadvantage of the NMD2.2 version is that the interface is only partially implemented and therefore does not return representative values for product cards. This has been intentionally left as 'to do' as we are not planning for the 2.2 version to be used in production stages.


## The Mapping Service
The Mapping service is used for two purposes:
* Retrieve and store user selected material mappings
* Retrieve and store reference data for facilitating the plugin to make an initial estimate which NMD product cards match on which ifc objects.

The first task will be performed automatically when running the Ifc Collection and MPG calculation Dataservices. How to run this services is described [here](https://github.com/TNOBIM/BBLintegration).


The second task requires some manual preparation and should actually be done first to get more accurate material to NMD mappings.

### prepopulating the mapping service
The first task is performed for each ifc model separately. The second task only needs to be done once and should be done before running any models. to do so, make sure you can connect to your instance of the mappingservice and run the main method from the `MapRunner` class (in the `org.opensourcebim.mapping` package) you can do so via your IDE or use the command line:

```
>> java -cp . MapRunner -root=<pathToRootFolder> -words=<relativePathToWrodsCsvFile> -nlsfb=<relativePathToNlsfbAlternativesFile> <OPTIONAL>-keyWords
```

The minimal viable product will be a mapping database with at least the NLsfb alternative mappings loaded. If this is not the case you can expect a large amount of elements to miss a mapping. 

Should it not work for you please check the following common errors:
* make sure the MapRunner class is on your classpath 
* make sure a -root argument is defined. (the console will return an error in this case)
* When running the MapRunner make sure a mappingService is running and the database service (if run locally) has started.

an example could be: 
```
>> java -cp . MapRunner -root="C://myRootFolder//" -words="path//to//csv//file//words.csv"
```
In this case only the words file at location `C://myRootFolder//path//to//csv//file//words.csv` will be read and processed.

The mapRunner performs a series of post requests on the mapservice
* if the `-words` argument is present it will write the commonwords csv data to the service
* if the `-keyWords` argument is present it will write the material keywords to the service
* if the `-nlsfb` argument is present it will write the nlsfb to ifcproduct type mappings to the service

Each of these steps require a .csv file that will be read in and parsed to the desired objects. You can define the location of these csv files in the `config.xml`






