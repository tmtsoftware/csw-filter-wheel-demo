# csw-filter-wheel-demo

A demo project containing a Scala.js based web app for controlling a filter and disperser wheel (via a Galil motion controller) using the CSW framework and a Galil HCD..

The project has the following parts:

* demo-assembly - an assembly that accepts Setup commands to select a filter and/or disperser (by name) and passes commands on to the Galil HCD.

* demo-deploy - provides a command line application for starting the assembly.

* demo-web-app - contains a Scala.js based web UI for setting the filter and disperser values and displaying feedback while the wheel is moving


## Prerequisites

This project depends on the [csw](https://github.com/tmtsoftware/csw), [esw-prototype](https://github.com/tmtsoftware) and [galil-prototype](https://github.com/tmtsoftware/galil-prototype) projects, which are assumed below to be installed under the same parent directory as this project. 
The version of csw must be the same as the one used by esw-prototype.
At present, this is set to Git commit SHA identifier. Use this to also check out that version of csw and install the services:

    cd ../csw
    git checkout <sha>
    sbt publishLocal stage

### Start the CSW services:
    
    export interfaceName=... # Set to the ethernet interface to use for networking
    ./target/universal/stage/bin/csw-services.sh start

Note: In future versions this will be easier to do using the [tmt-deploy](https://github.com/tmtsoftware/tmt-deploy) project.

### Start the OCS Gateway (web app access to CSW services)

Compile the esw-prototype dependencies and install and run the `ocs-gateway`, which provides access to the CSW services for web apps:
 
    cd ../esw-prototype
    sbt publishLocal stage 
    ./target/universal/stage/bin/ocs-gateway

### Start the Galil HCD:

    cd ../galil-prototype
    sbt publishLocal stage

If you don't have a Galil device, you can run a simulator:

    ./target/universal/stage/bin/galil-simulator

Start the HCD to run with the galil simulator:

    ./target/universal/stage/bin/galil-hcd --local galil-hcd/src/main/resources/GalilHcd.conf

If you do have a Galil device (such as a DMC-500x0), you can configure the host and port to use for it on the command line. For example:

    ./target/universal/stage/bin/galil-hcd --local galil-hcd/src/main/resources/GalilHcd.conf -Dgalil.host=192.168.2.2 -Dgali.port=23

## Building

Building this project consists of installing the script used to start the assembly as well as compiling the Scala.js code to JavaScript and starting a test HTTP server:

    sbt 
    > stage
    > fastOptJS::startWebpackDevServer
    ...
    > fastOptJS::webpack             // If something in the UI code changes

Note that the fastOptJS and startWebpackDevServer tasks currently output a lot of warnings and errors that can generally be ignored.
See https://github.com/webpack/webpack/issues/4518 for more information.

## Starting the assembly:

A config file is provided for start the assembly on the local host:

    target/universal/stage/bin/demo-container-cmd-app --local demo-deploy/src/main/resources/GalilDemo.conf

At this point you can access the web app at http://localhost:8080/.

When you select a filter or disperser, the web app sends a Submit to the assembly (via the gateway) and waits for events, which it 
uses to display the current positions of the filter and disperser wheels. The assembly talks to the generic Galil HCD.

## Implementation

When you select a filter or disperser from the menu, a Setup object is created with the name of the selected item 
and sent to the demo assembly via the ESW gateway, which runs by default on http://localhost:9090
