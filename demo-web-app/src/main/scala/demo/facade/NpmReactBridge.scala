package demo.facade

import com.github.ahnfelt.react4s.ReactBridge

/**
 * Use (for now) in place of ReactBridge for react4s apps.
 *
 * See https://github.com/Ahnfelt/react4s/issues/11 and
 * https://github.com/Ahnfelt/react4s/issues/12.
 *
 */
object NpmReactBridge extends ReactBridge(React, ReactDOM)
