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
* access to a running version of the BimMapService. this can be locally or any remote service that you have a connection string for. 

#### Setup of plugin for bimserver

* To get a running version of BimServer please refer to the BimServer docs [here](https://github.com/opensourceBIM/BIMserver)
* make sure the pom.xml dependencies in this repository are refering to the version of your bimserver jar/war/repository 
* when running your bimserver instance make sure that this plugin is added using the command line arguments described in the BimServer documentation

#### Connecting to the NMD
At time of writing there are currently two connection options to the NMD
 * SQLite NMD2.2 connection
 * REST api NMD3.0 connection

 Currently we make use of the NMD2.2 connection as the 3.0 version is not ready for full load testing yet. The problem with the 2.2 version is howver that the user needs its own sqlite *.db file hosted locally in the config data folder. 

The disadvantage of the NMD2.2 version is that the interface is only partially implemented and therefore does not return representative values for product cards. This is ongoing work

#### prepopulating the mapping service
TheMapping service is used for two purposes:
* Retrieve and store user selected material mappings
* Retrieve and store reference data for facilitating the plugin to make an initial estimate which NMD product cards match on which ifc objects.

The first task is performed for each ifc model separately. The second task only needs to be done once and should be done before running any models. to do so, make sure you can connect to your instance of the mappingservice and run the main method from the `MapRunner` class (in the `org.opensourcebim.mapping` package) you can do so via your IDE or use the command line:

```
>> java -cp . MapRunner
```
make sure the MapRunner class is on you classpath.

The mapRunner performs a series of post requests on the mapservice
* write the commonwords to the service
* write the material keywords to the service
* write the nlsfb to ifcproduct type mappings to the service

Each of these steps require a .csv file that will be read in and parsed to the desired objects. You can define the location of these csv files in the `config.xml`






