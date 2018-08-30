package demo

import com.github.ahnfelt.react4s._
import demo.param.formats.JsonSupport
import demo.param.generics.{Key, KeyType, Parameter}
import tmt.WebClients
import tmt.sequencer.models.ControlCommandWeb
import ujson.Js

object MainComponent {
  // XXX TODO: Get these values dynamically from the HCDs?
  val filters                = List("None", "g_G0301", "r_G0303", "i_G0302", "z_G0304", "Z_G0322", "Y_G0323", "u_G0308")
  val filterKey: Key[String] = KeyType.StringKey.make("filter")

  val dispersers =
    List("Mirror", "B1200_G5301", "R831_G5302", "B600_G5303", "B600_G5307", "R600_G5304", "R400_G5305", "R150_G5306")
  val disperserKey: Key[String] = KeyType.StringKey.make("disperser")

  val assemblyName = "DemoAssembly"
}

case class MainComponent() extends Component[NoEmit] {
  import MainComponent._

//  // XXX TODO: Provide a stripped down Parameter[T] API for Scala.js?
//  val jsonTemplate =
//    """{
//      |"type":"Setup",
//      |"runId":"b3b7f7a8-07a8-4551-af18-4c2d768c632e",
//      |"source":"test.demo",
//      |"commandName":"demo",
//      |"obsId":"2023-Q22-4-33",
//      |"paramSet":[
//      |{"keyName":"filter",
//      |  "keyType":"StringKey",
//      |  "values":["None"],
//      |  "units":"NoUnits"
//      |},{
//      |  "keyName":"disperser",
//      |  "keyType":"StringKey",
//      |  "values":["Mirror"],
//      |  "units":"NoUnits"
//      |}]}""".stripMargin

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
    val client = WebClients.assemblyCommandClient(assemblyName)
    if (name == "Filter") {
      val items: Set[Parameter[_]] = Set(filterKey.set(value))
      val jsValue                  = JsonSupport.paramSetFormat.writes(items)
      println(s"$jsValue")

//
//      val cmd = client.submit(new ControlCommandWeb(
//        kind = "Setup",
//        source = "test.demo",
//        commandName = "demo",
//        maybeObsId = None,
//        paramSet = paramSet,
//        runId = None
//      ))
    } else if (name == "Disperser") {
      val items: Set[Parameter[_]] = Set(disperserKey.set(value))
      val jsValue                  = JsonSupport.paramSetFormat.writes(items)
      println(s"$jsValue")
    }
  }
}
