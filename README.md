# Multi-model monitor

Implementation of a multi-model monitor for the M3 Frameowrk.

A demonstration video of the implementation is available at: https://youtu.be/sjWdOcBVsw4

## Running the application

The application is currently compiled as a single [runnable fatjar](https://github.com/antialman/model-interplay-monitoring-code/releases/tag/0.1.0) (requires [Java 11 JDK](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html) to be installed).

It is possible to use both the graphical user interface or the command line interface.


### Graphical user interface

Graphical user interface allows to select the input event log and process specifications as well as editing the violation cost of each model. The results can be visualised on a trace-by trace basis.

Command: `java -jar .\model-interplay.jar`
* _If there are other versions of Java installed on the same computer then it may be necessary to use the command: `java -Djava.library.path=. -jar .\model-interplay.jar`_

User interface elements:
1. Button for selecting the input event log
2. Button for selecting the input process specifications
3. Violation cost column (double-click to edit the value)
4. Buttons for removing process specifications
5. Button to start the monitoring
6. List of processed traces (selecting a trace displays the results for that trace)
7. Visualisation of the monitoring results (hovering on an event name displays recommendations after that event)

![image](https://user-images.githubusercontent.com/18569885/125088890-85f54900-e0d6-11eb-824a-53878923b045.png)


### Command line interface

Command line interface processes the input process specifications based on a costs file and prints the monitoring results directly to stdout.

Command: `java -jar .\model-interplay.jar --cmd -c ..\input\costModel.txt -e ..\input\eventlog.xes`
* `--cmd` - Speficies that the application should be started in the command line mode
* `-c` - Input model files and violation costs. Model and corresponding violation cost separated by comma. Each model on separate line. File extension defines the model type (decl - Declare; pnml - Data Petri Net; ltl - same format as decl file, but using raw formulas instead of Declare templates)
* `-l` - Input event log 

## Inputs
* An event log in XES or MXML format
* Declare models in .decl format (as produced by RuM: https://rulemining.org/)
* LTL formulas is custom format
  * Activities and attributes must be defined the same way as in .decl file
  * Each LTL formula on a separate line followed by `|` and optional data conditions for each activity (spearated by `|`)
  * _For example: (  <> ( A ) \\/ <>( B )  ) |A.x>5 and A.x<9 |B.y=4 or B.z<10_
* Data Petri Nets (as produced by ProM: https://www.promtools.org/doku.php?id=prom69)
  * Plugin "Create DPN (Text language based)" by F.Mannhardt 


## Limitations

* Parentheses are currently not supported in conditions.
* Only variable to constant comparisons (x>5, y<=3, z=="a" etc.) are supported.
* Implementation has been tested only on Windows 10.
* Silent transitions are not supported.
* The implementation may produce incorrect results if two transitions of a DPN, that can be enabled at the same time, have overlapping guards

## Authors

Conference paper (see the technical report [here](https://arxiv.org/abs/2111.13136)):
* Anti Alman
* Fabrizio Maria Maggi
* Marco Montali
* Fabio Patrizi
* Andrey Rivkin

Implementation: Anti Alman

