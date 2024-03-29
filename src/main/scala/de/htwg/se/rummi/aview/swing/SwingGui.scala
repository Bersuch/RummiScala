package de.htwg.se.rummi.aview.swing

import java.awt.Color

import de.htwg.se.rummi.controller._
import de.htwg.se.rummi.model.{Ending, Player, RummiSet, Tile}

import scala.swing._
import scala.swing.event.ButtonClicked


/**
  *
  * @param co
  */
class SwingGui(co: Controller) extends MainFrame {

  listenTo(co)

  preferredSize = new Dimension(1100, 800)
  title = "Rummikub in Scala"

  val finishButton = new Button("Finish")
  listenTo(finishButton)
  val getTileButton = new Button("Get Tile")
  listenTo(getTileButton)
  val checkButton = new Button("Check")
  listenTo(checkButton)

  val statusLabel = new Label(GameState.message(co.getGameState))
  val playerLabel = new Label("Current Player: " + co.activePlayer.name)

  val field = new SwingGrid(8, 13)
  val rack = new SwingGrid(4, 13)

  val newGameMenuItem = new MenuItem("New Game")
  listenTo(newGameMenuItem)
  val quitMenuItem = new MenuItem("Quit")
  listenTo(quitMenuItem)

  menuBar = new MenuBar() {
    contents += new Menu("Menu") {
      contents += newGameMenuItem
      contents += quitMenuItem
    }
  }

  val center = new BoxPanel(Orientation.Vertical) {

    contents += field
    contents += rack
    contents += new BoxPanel(Orientation.Horizontal) {
      contents += finishButton
      contents += checkButton
      contents += getTileButton
    }
  }

  val sortButton = new Button("Sort")
  listenTo(sortButton)

  val south = new GridPanel(1, 3) {
    contents += playerLabel
    contents += statusLabel
    contents += new FlowPanel {
      contents += sortButton
    }
  }

  contents = new BorderPanel() {
    add(center, BorderPanel.Position.Center)
    add(south, BorderPanel.Position.South)
  }

  rack.fields.foreach(t => listenTo(t))
  field.fields.foreach(t => listenTo(t))

  var selectedField: Option[Field] = Option.empty

  reactions += {
    case ButtonClicked(b) => {

      if (b.isInstanceOf[Field]) {
        val clickedField: Field = b.asInstanceOf[Field]
        fieldClicked(clickedField)
      } else if (b == getTileButton) {
        co.draw()
      } else if (b == finishButton) {
        co.switchPlayer()
      } else if (b == checkButton) {

      } else if (b == quitMenuItem) {
        sys.exit(0)
      } else if (b == newGameMenuItem) {
        co.initGame()
      } else if (b == sortButton) {
        co.sortRack()
      }
    }

    case event: FieldChangedEvent => {
      field.displayGrid(co.field)
      rack.displayGrid(co.getRack(co.activePlayer))
    }

    case event: ValidStateChangedEvent => {
      if (co.isValidField) {
        finishButton.enabled = true
      } else {
        finishButton.enabled = false
      }
    }

    case event: PlayerSwitchedEvent => {
      playerLabel.text = "Current Player: " + co.activePlayer.name
      rack.displayGrid(co.getRack(co.activePlayer))
    }

    case event: WinEvent => {
      field.enabled = false
    }

    case event: GameStateChanged => {

      co.getGameState match {
        case GameState.DRAWN => {
          getTileButton.enabled = false
          finishButton.enabled = true
          field.fields.foreach(f => f.enabled = false)

        }
        case GameState.WAITING => {
          getTileButton.enabled = true
          field.fields.foreach(f => f.enabled = true)
        }
        case GameState.INVALID | GameState.TO_LESS => {
          finishButton.enabled = false
          getTileButton.enabled = false
        }case GameState.VALID => {
          finishButton.enabled = true
          getTileButton.enabled = false
        }
        case _ =>

      }

      statusLabel.text = GameState.message(co.getGameState)
    }
  }

  private def fieldClicked(clickedField: Field) = {
    // Click on a empty field and there is no field selected -> Do nothing
    if (clickedField.tileOpt.isEmpty && selectedField.isEmpty) {

    }
    // Click on a empty field an there is a field selected -> move selected to empty field, unselect
    else if (clickedField.tileOpt.isEmpty && selectedField.isDefined) {

      moveTile(clickedField)

      selectedField.get.border = Swing.LineBorder(Color.BLACK, 1)
      selectedField = None
    }
    // Click on a filled field an no field selected -> Select field
    else if (clickedField.tileOpt.isDefined && selectedField.isEmpty) {
      selectedField = Some(clickedField)
      clickedField.border = Swing.LineBorder(Color.BLACK, 4)
    }
    // Click on a filled field an there is a field selected --> Unselect if same selected and clicket is same, else
    //      deselect the currently selected field and select the clicked field
    else if (clickedField.tileOpt.isDefined && selectedField.isDefined) {
      if (clickedField == selectedField.get) {
        selectedField.get.border = Swing.LineBorder(Color.BLACK, 1)
        selectedField = None
      } else {
        selectedField.get.border = Swing.LineBorder(Color.BLACK, 1)
        selectedField = Some(clickedField)
        clickedField.border = Swing.LineBorder(Color.BLACK, 4)
      }
    }
  }

  private def moveTile(clickedField: Field) = {
    if (rack.containsField(selectedField.get) && field.containsField(clickedField)) {
      co.moveTile(co.rackOfActivePlayer, co.field, selectedField.get.tileOpt.get, clickedField.row, clickedField.col)
    }

    if (field.containsField(selectedField.get) && rack.containsField(clickedField)) {
      co.moveTile(co.field, co.rackOfActivePlayer, selectedField.get.tileOpt.get, clickedField.row, clickedField.col)
    }

    if (rack.containsField(selectedField.get) && rack.containsField(clickedField)) {
      co.moveTile(co.rackOfActivePlayer, co.rackOfActivePlayer, selectedField.get.tileOpt.get, clickedField.row, clickedField.col)
    }

    if (field.containsField(clickedField) && field.containsField(selectedField.get)) {
      co.moveTile(co.field, co.field, selectedField.get.tileOpt.get, clickedField.row, clickedField.col)
    }
  }

  def init = {
    rack.displayGrid(co.getRack(co.activePlayer))
    field.displayGrid(co.field)
  }

}