package demo.param.models
import demo.param.generics.{Parameter, ParameterSetType}
import play.api.libs.json.{Json, OFormat}

/**
 * A configuration for setting telescope and instrument parameters
 *
 * @param paramSet a set of Parameters
 */
case class Struct private (paramSet: Set[Parameter[_]]) extends ParameterSetType[Struct] {

  /**
   * A Java helper to create Struct with empty paramSet
   */
  def this() = this(Set.empty[Parameter[_]])

  /**
   * Create a new Struct instance when a parameter is added or removed
   *
   * @param data a set of parameters
   * @return a new instance of Struct with provided data
   */
  override protected def create(data: Set[Parameter[_]]) = new Struct(data)

  /**
   * A comma separated string representation of all values this Struct holds
   */
  override def toString: String = paramSet.mkString(", ")
}

object Struct {
  //used by play-json
  private[param] implicit val format: OFormat[Struct] = Json.format[Struct]

  /**
   * A helper method to create Struct from given paramSet
   *
   * @param paramSet a set of parameters
   * @return an instance of Struct
   */
  def apply(paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]]): Struct = new Struct().madd(paramSet)
}
