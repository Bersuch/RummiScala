package de.htwg.se.rummi.aview.swing

import java.awt.Color

import de.htwg.se.rummi.model.{RummiColor, Tile}

import scala.swing.{Button, Swing}

class Field(val row: Int, val col: Int) extends Button {
  var tileOpt = Option.empty[Tile]
  val WIDTH = 10
  val HIGHT = 10
  preferredSize = new swing.Dimension(WIDTH, HIGHT)
  minimumSize = new swing.Dimension(WIDTH, HIGHT)
  maximumSize = new swing.Dimension(WIDTH, HIGHT)
  unsetTile()

  def setTile(tile: Tile): Unit = {

    tileOpt = Some(tile)

    background = tile.color match {
      case RummiColor.YELLOW => Color.decode("#FFFF33")
      case RummiColor.GREEN => Color.decode("#90EE90")
      case RummiColor.BLUE => Color.decode("#add8e6")
      case RummiColor.RED => Color.decode("#ff0000")
      case RummiColor.WHITE => Color.decode("#000000")
    }

    text = tile.joker match {
      case true => "J"
      case false => tile.number.toString
    }
  }

  def unsetTile(): Unit = {
    background = Color.WHITE
    text = ""
    tileOpt = None
    border = Swing.LineBorder(Color.BLACK, 1)
  }
}
