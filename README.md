
STREAMER
=================

Check our [website](https://streamer-framework.github.io)

## Documentation

The documentation is intended to give an introduction on how to use STREAMER in the various possible ways. 
As a user you can use it to develop new algorithms and test different Machine Learning algorithms in a streaming context.

[Getting Started](https://streamer-framework.github.io/docs/GettingStartedGuideSTREAMER_v2.0.pdf)

[Extended User Guide](https://streamer-framework.github.io/docs/UserGuideSTREAMER_v2.0.pdf)

[Javadoc](https://github.com/streamer-framework/streamer/tree/main/STREAMER/javadoc)

## License

The use and distribution terms for this software are covered by the
GNU GENERAL PUBLIC LICENSE Version 3 (https://www.gnu.org/licenses/gpl-3.0.html).

## Our Community

Our community is formed by a group of AI researchers working for CEA List (LI3A) and their collaborators.

The development of STREAMER is being supported by two collaborative projects (StreamOps and SmartWater4Europe) and CEA List.

### Head of the project

Sandra GARCIA-RODRIGUEZ (sandra.garciarodriguez@cea.fr)

## Getting Started

Here, we provide a quick installation of STREAMER. For the detailed installation and use check [Documentation] above.


Before we start the installation, you need to decide how you will use the framework. STREAMER is conceived to be used in two different ways (depending on your necessities):

 - Development use (oriented to data scientists): You are interested on directly working on the code of the framework to add/develop several functionalities and test them.
 - Product use (oriented to industrial use): You want to use the framework as a product (no need to get in contact with the code but execute STREAMER). In this case, you need to have in your computer the basic services STREAMER requires and STREAMER instance already packed.

### 0) Download STREAMER
Download STREAMER source + environment setup from https://github.com/streamer-framework/streamer

### Simply clone the repository:
```bash
git clone https://github.com/streamer-framework/streamer.git
```
You can already run the example use case we provide by 


### 1) Getting ready for Deployment use

In that case, you need to run the basic services that STREAMER requires. You can install them using the docker (recommended) following the steps of section 3.1, or install them yourself following section 4.

1- Install Eclipse (https://www.eclipse.org/downloads/) or the IDE you prefer.

2- Import the maven project: File->Import->Maven->Existing Maven Projects (and follow the steps to select the folder of STREAMER project).

3- [Optional] you are now ready to run our example use case!

Run from eclipse the main class ProducerMain to launch the data ingester, or from console:
```bash
java -cp target/streamer-1.0.0-test-jar-with-dependencies.jar cea.ProducerMain [setup_folder]
```
Run from eclipse the main class LauncherMain to launch the streaming pipeline, or from console:
```bash
java -cp target/streamer-1.0.0-test-jar-with-dependencies.jar cea.LauncherMain [setup_folder]
```
4- Create your first use case in STREAMER by following the steps of section 6 of the [user guide](https://github.com/streamer-framework/streamer/streamer/GettingStartedGuideSTREAMER.pdf).

5- Run your application in STREAMER as section 5 of the [user guide](https://github.com/streamer-framework/streamer/streamer/GettingStartedGuideSTREAMER.pdf) shows.

#### Important:
for running our use case example or our proposed Python or R algorithms do not forget installing the packages they need in your computer. 

Python:
```bash
pip3 -r install services/requirementsPython.txt
```
R (services/requirementsR.txt):
```bash
install.packages(c("caret","RCurl","rredis", "kernlab", "e1071", "neuralnet","xgboost"))
```


### 2) Getting ready for Production use
Try our example use case in STREAMER by following the steps of Section 3.

### 3) Using Docker
We make simple and transparent the installation of STREAMER and its services by using Docker. We provide 2 docker files that serve to:

a) Services environment (section 3.1): it contains all the services used by the framework (Kafka & Zookeeper, Redis. InfluxDB, Kibana & ElasticSearch).

b) Production environment (section 3.2): it contains STREAMER for production purpose.


#### 3.1 Install & run all the services from Docker (recommended)
[Warning]: for Linux based systems, you may need to run all the commands in “sudo” mode as, for instance, “sudo docker-compose up --build -d “.
Follow the following steps to set all necessary services before running STREAMER:

1- Install docker on your machine. At the following link, you will find how to install the docker for all the different operating systems (Windows, Linux, Mac): https://docs.docker.com/get-docker/

2- [For Linux] also install docker-compose from https://docs.docker.com/compose/install/

3- Unzip the provided folder “streamer_environment”.

4- Open a terminal and change directory to this “streamer_environment” directory.

5- Run the following command to start the services:
```bash
docker-compose up --build -d
```
In order to check if the services are running properly, check the following command:
```bash
docker ps
```

To stop the services, use the following command:
```bash
docker-compose down
```

Note: If after following the previous steps, you face a similar error to
```bash
ERROR: [1] bootstrap checks failed [1]: max virtual memory areas vm.max_map_count [65530] is too low, increase to at least [262144]
```
you can solve it by increasing your virtual memory. Run the command:
sudo sysctl -w vm.max_map_count=262144
and then build the docker again with:
sudo docker-compose up --build –d

#### 3.2 Running STREAMER in production environment
[Warning]: for Linux based systems, you may need to run all the commands in “sudo” mode as, for instance, sudo docker-compose up --build -d.

1- Before running STREAMER for production purpose, complete all the steps of case 1 above to run all the services.

2- Open a terminal and go to directory “streamer_environment/streamer_environment” directory (sub-folder of streamer_environment folder).

3- Run the following command to start the framework:
```bash
docker-compose up --build
```
If you add (-d) to the command above, it should start in the background. In case you are interested in showing the output of the framework, feel free to leave it like it-is.

In order to check if the framework is running properly, check the following command:
```bash
docker ps
```
To stop the services, use the following command:
If you started the framework without using (-d) property, first press (ctrl + c) and then the following (with (-d) or not):
```bash
docker-compose down
```

### 4) Install services yourself

STREAMER uses the following services that you can easily install yourself:
- [Apache Kafka (with Zookeeper)](https://kafka.apache.org/quickstart) / soft / Apache License 7.3.0 / version=2.7 
- [Redis](https://redis.io/) / soft / BSD / version= 7.0.5 
- [InfluxDB](https://portal.influxdata.com/) / soft / MIT / version 1.8.10
- [Elasticsearch & Kibana](https://www.elastic.co/) / soft / Apache License 2.0 / version=:7.17.0 [Optional service, just for using the Graphical Interface]
