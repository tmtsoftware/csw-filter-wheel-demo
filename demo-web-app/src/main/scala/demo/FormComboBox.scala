package demo

import com.github.ahnfelt.react4s._

case class FormComboBox(labelStr: P[String], choices: P[List[String]]) extends Component[String] {

  override def render(get: Get): Element = {
    val label      = get(labelStr)
    val idStr      = label.toLowerCase()
    val stateIdStr = idStr + "State"

    val items      = get(choices).map(s => E.option(A.value(s), Text(s)))
    val selectItem = E.select(A.id(idStr), A.onChangeText(itemSelected), Tags(items))

    val selectState = E.span(A.id(stateIdStr), E.i(A.className("material-icons"), Text("done")))

    E.div(
      E.label(A.className("col s1"), A.`for`(idStr), Text(label)),
      E.div(A.className("col s10 input-field"), selectItem),
      E.div(A.className("col s1"), selectState),
    )
  }

  // called when an item is selected
  private def itemSelected(value: String): Unit = {
    emit(value)

//    stateItem.removeClass(s"$savedIcon $errorIcon text-danger text-success")
//    stateItem.addClass(editedIcon)
//    stateItem.removeClass("hidden")
//    listener(getSelectedItem)
  }

}
