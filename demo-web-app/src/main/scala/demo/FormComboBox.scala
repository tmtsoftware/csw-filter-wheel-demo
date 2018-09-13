package demo

import com.github.ahnfelt.react4s._

/**
 * Displays a label with a menu of choices and updates the display with the given current state variable.
 *
 * @param labelStr the label to display
 * @param choices the choices for the menu
 * @param currentState a state variable that can be updated from the outside with the current value
 */
case class FormComboBox(labelStr: P[String], choices: P[List[String]], currentState: P[String]) extends Component[String] {
  val targetState = State("")

  override def render(get: Get): Element = {
    val label        = get(labelStr)
    val currentValue = get(currentState)
    val choiceList   = get(choices)
    val targetValue  = if (get(targetState).isEmpty) choiceList.head else get(targetState)

    val labelItem  = Text(label)
    val labelDiv   = E.div(A.className("col s1"), labelItem)
    val items      = choiceList.map(s => E.option(A.value(s), Text(s)))
    val selectItem = E.select(A.className("input-field"), A.onChangeText(itemSelected), Tags(items))
    val selectDiv  = E.div(A.className("col s3"), selectItem)

    val i         = choiceList.indexOf(currentValue)
    val matched   = currentValue == targetValue
    val iconName  = if (matched) "done" else if (i == 0) "filter_none" else s"filter_$i"
    val iconLabel = if (matched) "" else currentValue

    val selectStateIcon  = E.i(A.className("material-icons"), Text(iconName))
    val selectStateLabel = Text(s" $iconLabel")
    val selectStateDiv   = E.div(A.className("col s8"), selectStateIcon, selectStateLabel)

    E.div(A.className("row valign-wrapper"), labelDiv, selectDiv, selectStateDiv)
  }

  // called when an item is selected
  private def itemSelected(value: String): Unit = {
    targetState.set(value)
    emit(value)
  }

}
