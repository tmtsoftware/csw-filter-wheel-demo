package demo
import com.github.ahnfelt.react4s.Component
import tmt.r4s.facade.NpmReactBridge

object DemoWebApp {
  def main(args: Array[String]): Unit = {
    NpmReactBridge.renderToDomById(Component(MainComponent), "main")
  }
}
