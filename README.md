# Model interplay monitor

Code for the conference paper "Monitoring Hybrid Process Specifications with Conflict Management"

## Running the application

The application is currently compiled as a single fatjar. Java 11 JDK must be installed in order to run it: https://www.oracle.com/java/technologies/javase-jdk11-downloads.html.

It is possible to use both the graphical user interface or the commandline interface.

### Graphical user interface

Graphical user interface allows to select the input event log and process specifications as well as editing the violation cost of each model. The results ccan be vsualised on a trace-by trace basis.

Command: `java -jar .\model-interplay.jar`




### Command line interface

Command line interface processes the input process specifications based on a costs file and prints the monitoring results directly to stdout.

Command: `java -jar .\model-interplay.jar --cmd -c ..\input\costModel.txt -e ..\input\eventlog.xes`
* --cmd - Speficies that the application should be started in the command line mode
* -c - Input model files and violation costs. Model and correcponding violation cost separated by comma. Each model on separate line. File extension defines the model type (decl - Declare; pnml - Data Petri Net; ltl - same format as decl file, but using raw formulas instead of Declare templates)
* -l - Input event log




## Authors

Conference paper:
* Anti Alman
* Fabrizio Maria Maggi
* Marco Montali
* Fabio Patrizi
* Andrey Rivkin

Implementation: Anti Alman

