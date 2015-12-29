package io.synaptix.pantilt

import spray.json._

case class PositionDescription(pan: Double, tilt: Double)

case class Error(message: String)

trait EventMarshalling extends DefaultJsonProtocol {
  implicit val positionDescriptionFormat = jsonFormat2(PositionDescription)
  implicit val errorFormat = jsonFormat1(Error)
}
