package demo
import com.github.ahnfelt.react4s.{Component, ReactBridge}
import demo.facade.NpmReactBridge

object DemoWebApp {
  def main(args: Array[String]): Unit = {
    val component = Component(MainComponent)
    NpmReactBridge.renderToDomById(Component(MainComponent), "main")
  }
}
