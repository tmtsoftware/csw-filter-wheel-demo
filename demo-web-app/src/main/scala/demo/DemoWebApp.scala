package demo
import com.github.ahnfelt.react4s.Component
import demo.util.NpmReactBridge

object DemoWebApp {
  def main(args: Array[String]): Unit = {
    NpmReactBridge.renderToDomById(Component(MainComponent), "main")
  }
}
