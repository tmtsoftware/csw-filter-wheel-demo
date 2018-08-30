package demo.param.models

import play.api.libs.json.{Json, OFormat}

/**
 * Holds Ra(Right Ascension) and Dec(Declination) values
 */
case class RaDec(ra: Double, dec: Double)

case object RaDec {

  //used by play-json
  private[param] implicit val raDecFormat: OFormat[RaDec] = Json.format[RaDec]

}
