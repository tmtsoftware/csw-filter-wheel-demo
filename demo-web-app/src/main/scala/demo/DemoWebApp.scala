package demo
import com.github.ahnfelt.react4s.Component
import react4s.facade.NpmReactBridge

object DemoWebApp {
  def main(args: Array[String]): Unit = {
    NpmReactBridge.renderToDomById(Component(MainComponent), "main")
  }
}
