package demo

import com.github.ahnfelt.react4s._

case class MainComponent() extends Component[NoEmit] {
  val filters = List("None", "g_G0301", "r_G0303", "i_G0302", "z_G0304", "Z_G0322", "Y_G0323", "u_G0308")

  val dispersers =
    List("Mirror", "B1200_G5301", "R831_G5302", "B600_G5303", "B600_G5307", "R600_G5304", "R400_G5305", "R150_G5306")

  override def render(get: Get): Element = {

    E.div(
      A.className("container"),
      E.div(A.className("row teal lighten-2"), E.div(A.className("col s12"), Text("CSW Filter Wheel Demo"))),
      E.div(A.className("row"), E.div(Component(FormComboBox, "Filter", filters).withHandler(s => itemSelected("Filter", s)))),
      E.div(A.className("row"),
            E.div(Component(FormComboBox, "Disperser", dispersers).withHandler(s => itemSelected("Disperser", s)))),
    )
  }

  private def itemSelected(name: String, value: String): Unit = {
    println(s"XXX Selected $name: $value")
  }
}
