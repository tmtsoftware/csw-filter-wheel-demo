package org.tmt.test.demodeploy

import csw.framework.deploy.containercmd.ContainerCmd

object DemoContainerCmdApp extends App {

  ContainerCmd.start("csw-filter-wheel-demo-container-cmd-app", args)

}
