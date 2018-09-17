# csw-filter-wheel-demo

A demo project containing a Scala.js based web app for controlling a filter and disperser wheel using the CSW framework.

The project has the following parts:

* demo-assembly - an assembly that accepts Setup commands to select a filter and/or disperser (by name)

* demo-hcd - contains the filter and disperser HCDs, which each accept a numerical value for a filter or disperser

* demo-deploy - provides a command line application for starting the assembly and HCDs.

* demo-web-app - contains a Scala.js based web UI for setting the filter and disperser values and displaying feedback while the wheel is moving


## Prerequisites

This project depends on csw-prod and esw-prototype. The version of csw-prod must be the same as the one used by esw-prototype.
At present, this is set to Git commit identifier. Use this to also check out that version of csw-prod and install the services:

    cd ../csw-prod
    git checkout <sha>
    sbt publishLocal stage

Start the CSW services:
    
    export interfaceName=... # Set to the ethernet interface to use for networking
    csw-services.sh start
    export clusterSeeds=... # See output from above command. Use this setting for all of the following commands.

Compile the esw-prototype dependencies and install and run the `ocs-gateway`, which provides access to the CSW services for web apps:
 
    cd ../esw-prototype
    sbt publishLocal stage 
    ./target/universal/stage/bin/ocs-gateway

## Building

Building this project consists of installing the script used to start the assembly and HCDs as well as compiling the Scala.js code to JavaScript and starting a test HTTP server:

    sbt 
    > publishLocal stage
    > fastOptJS::startWebpackDevServer
    > fastOptJS::webpack

Note that the fastOptJS tasks currently output a lot of warnings and errors that can generally be ignored.
See https://github.com/webpack/webpack/issues/4518 for more information.

## Starting the assembly and HCDs:

A config file is provided for start the assembly and HCDs on the local host:

    target/universal/stage/bin/demo-container-cmd-app --local demo-deploy/src/main/resources/DemoContainer.conf

At this point you can access the web app at http://localhost:8080/.

When you select a filter or disperser, the web app sends a Submit to the assembly and waits for events, which it 
uses to display the current positions of the filter and disperser wheels.

## Implementation

When you select a filter or disperser from the menu, a Setup object is created with the name of the selected item 
and sent to the demo assembly via the ESW gateway, which runs by default on http://localhost:9090