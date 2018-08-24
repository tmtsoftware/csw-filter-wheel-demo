package org.tmt.test.demodeploy

import csw.framework.deploy.hostconfig.HostConfig

object DemoHostConfigApp extends App {

  HostConfig.start("csw-filter-wheel-demo-host-config-app", args)

}
