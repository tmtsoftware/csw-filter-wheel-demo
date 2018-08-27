package demo

import com.github.ahnfelt.react4s._

case class MainComponent() extends Component[NoEmit] {
  override def render(get: Get): Element = {
    E.div(Text("Hello world!"))
  }
}
