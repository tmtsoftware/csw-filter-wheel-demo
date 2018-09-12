package demo

import com.github.ahnfelt.react4s._

//object FormComboBox {
//  case class IconCss(angle: Int)
//      extends CssClass(
//        S.transform(s"rotate(${angle}deg)")
//      )
//
//}

case class FormComboBox(labelStr: P[String], choices: P[List[String]], currentState: P[String]) extends Component[String] {
//  import FormComboBox._

  val targetState = State("")

  override def render(get: Get): Element = {
    val label        = get(labelStr)
    val idStr        = label.toLowerCase()
    val stateIdStr   = idStr + "State"
    val currentValue = get(currentState)
    val targetValue  = get(targetState)
    val choiceList   = get(choices)
    println(s"XXX $label current value is $currentValue, target value is $targetValue")

    val items      = choiceList.map(s => E.option(A.value(s), Text(s)))
    val selectItem = E.select(A.id(idStr), A.onChangeText(itemSelected), Tags(items))

    val i         = choiceList.indexOf(currentValue)
    val matched   = currentValue == targetValue
    val iconName  = if (matched) "done" else if (i == 0) "filter_none" else s"filter_$i"
    val iconLabel = if (matched) "" else currentValue

    val selectState =
      E.span(A.id(stateIdStr), E.i(A.className("material-icons"), Text(iconName)), Text(iconLabel))

    println(s"XXX render $label")

    E.div(
      E.label(A.className("col s1"), A.`for`(idStr), Text(label)),
      E.div(A.className("col s6 input-field"), selectItem),
      E.div(A.className("col s1"), selectState),
    )
  }

  // called when an item is selected
  private def itemSelected(value: String): Unit = {
    targetState.set(value)
    emit(value)
  }

}
