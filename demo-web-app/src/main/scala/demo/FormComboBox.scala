package demo

import com.github.ahnfelt.react4s._
import csw.params.commands.CommandResponse
import csw.params.commands.CommandResponse.{Accepted, Completed, Error, Invalid}

object FormComboBox {
  // Colors for the icons based on commandResponse
  val normalColor = ""
  val errorColor  = "red-text"
}

/**
 * Displays a label with a menu of choices and updates the display with the given current state variable.
 *
 * @param labelStr the label to display
 * @param choices the choices for the menu
 * @param currentState a state variable that can be updated from the outside with the current value
 */
case class FormComboBox(labelStr: P[String],
                        choices: P[List[String]],
                        currentState: P[String],
                        commandResponse: P[CommandResponse])
    extends Component[String] {

  import FormComboBox._

  val targetState = State("")

  override def render(get: Get): Element = {
    val label        = get(labelStr)
    val currentValue = get(currentState)
    val choiceList   = get(choices)

    val labelItem  = Text(label)
    val labelDiv   = E.div(A.className("col s1"), labelItem)
    val items      = choiceList.map(s => E.option(A.value(s), Text(s)))
    val selectItem = E.select(A.className("input-field"), A.onChangeText(itemSelected), Tags(items))
    val selectDiv  = E.div(A.className("col s3"), selectItem)

    val i = choiceList.indexOf(currentValue)
    val (iconName, iconLabel, iconColor) = get(commandResponse) match {
      case Accepted(_) =>
        (if (i == 0) "filter_none" else s"filter_$i", currentValue, normalColor)
      case Completed(_)      => ("done", "", normalColor)
      case Error(_, msg)     => ("error_outline", msg, errorColor)
      case Invalid(_, issue) => ("error_outline", issue.reason, errorColor)
      case x                 => ("error_outline", s"unexpected command response: $x", errorColor)
    }

    val selectStateIcon  = E.i(A.className(s"material-icons $iconColor"), Text(iconName))
    val selectStateLabel = Text(s" $iconLabel")
    val selectStateDiv   = E.div(A.className("col s8"), selectStateIcon, selectStateLabel)

    E.div(A.className("row valign-wrapper"), labelDiv, selectDiv, selectStateDiv)
  }

  // called when an item is selected
  private def itemSelected(value: String): Unit = {
    targetState.set(value)

    // This allows the main component to be notified when an item is selected
    emit(value)
  }

}
