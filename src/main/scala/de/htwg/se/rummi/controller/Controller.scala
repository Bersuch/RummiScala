package de.htwg.se.rummi.controller

import java.util.NoSuchElementException

import de.htwg.se.rummi.Const
import de.htwg.se.rummi.controller.GameState.GameState
import de.htwg.se.rummi.model.{RummiSet, _}

import scala.swing.Publisher
import scala.swing.event.Event

class Controller(playerNames: List[String]) extends Publisher {


  var currentSets: List[RummiSet] = Nil
  private var gameState: GameState = GameState.WAITING
  var tilesMovedFromRackToGrid: List[Tile] = Nil

  val game = Game(playerNames)
  var isValidField = false

  def getGameState: GameState = {
    gameState
  }

  def setGameState(g: GameState): Unit ={
    gameState = g
    publish(new GameStateChanged)
  }

  def activePlayer = game.activePlayer

  def field: Grid = {
    game.field
  }

  def initGame() = {
    gameState = GameState.WAITING
    currentSets = Nil
    tilesMovedFromRackToGrid = Nil

    game.generateNewGame(game.players)
    
    publish(new GameStateChanged)
    publish(new PlayerSwitchedEvent)
  }


  // finish
  def switchPlayer(): Unit = {
    if (!(Set(GameState.DRAWN, GameState.VALID).contains(gameState))) {
      return
    }

    if (tilesMovedFromRackToGrid.size > 0){
      game.activePlayer.inFirstRound = false
    }

    game.nextPlayer()

    currentSets = extractSets(field)

    // check if playingfield is valid
    setGameState(GameState.WAITING)
    publish(new GameStateChanged)
    tilesMovedFromRackToGrid = Nil
    publish(new PlayerSwitchedEvent)
  }
  def rackOfActivePlayer: Grid = getRack(game.activePlayer)

  def getRack(player: Player): Grid = {
    game.racks.find(x => x._1 == player) match {
      case Some(t) => t._2
      case None => {
        println("No Rack of " + player.name)
        throw new NoSuchElementException
      }
    }
  }

  def setField(newGrid: Grid) = game.field = newGrid

  def setRack(newRack: Grid) = {
    game.racks = game.racks + (game.activePlayer -> newRack)
  }

  /**
    * Did player reached minimum score to get out?
    * All sets which the user builds or appends to do count.
    * @return true if player reached minimum score
    */
  def playerReachedMinLayOutPoints(): Boolean = {
    val sumOfFirstMove = extractSets(field)
      .filter(x => x.tiles.toSet
        .intersect(tilesMovedFromRackToGrid.toSet).size > 0)
      .map(x => x.getPoints()).sum

    if (sumOfFirstMove < Const.MINIMUM_POINTS_FIRST_ROUND) {
      return false
    }

    true
  }

  def extractSets(field: Grid): List[RummiSet] = {
    var sets: List[RummiSet] = Nil

    field.tiles.groupBy(x => x._1._1).map(x => x._2).foreach(map => {
      var list = map.map(x => (x._1._2, x._2)).toList.sortBy(x => x._1)

      while (!list.isEmpty) {
        var tiles: List[Tile] = List.empty
        tiles = list.head._2 :: tiles
        while (list.find(x => x._1 == list.head._1 + 1).isDefined) {
          list = list.drop(1)
          tiles = list.head._2 :: tiles
        }
        sets = new RummiSet(tiles.reverse) :: sets
        list = list.drop(1)
      }
    })
    sets
  }

  /**
    * Check if all RummiSets on the field are valid.
    *
    * @return true if all sets are valid.
    */
  private def validateField(): Boolean = {
    var valid = true
    for (s <- extractSets(field)) {
      if (s.isValidRun() == false && s.isValidGroup() == false) {
        valid = false
      }
    }
    if (isValidField != valid) {
      isValidField = valid
      publish(new ValidStateChangedEvent)
    }
    valid
  }

  /**
    * Draw: If the player can not place a stone on the field, he must take a stone from the stack of covered stones.
    */
  def draw(): Unit = {

    if (gameState == GameState.DRAWN) {
      return
    }

    val newTile = game.coveredTiles.head
    game.coveredTiles = game.coveredTiles.filter(x => x != newTile)

    // get the current rack from the player
    val oldRack = game.racks.find(x => x._1 == game.activePlayer) match {
      case Some(r) => r._2
      case None => throw new NoSuchElementException("No rack for player '" + game.activePlayer + "'.")
    }

    // create a new rack with the tiles from the old one plus the newly drawn one
    val newRack = oldRack.getFreeField() match {
      case Some(freeField) => Grid(Const.RACK_ROWS, Const.RACK_COLS, oldRack.tiles + (freeField -> newTile))
      case None => throw new NoSuchElementException("No space in rack left.")
    }

    setRack(newRack)

    setGameState(GameState.DRAWN)
    publish(new FieldChangedEvent)
  }


  private def moveTileImpl(gridFrom: Grid, gridTo: Grid, tile: Tile, newRow: Int, newCol: Int): (Grid, Grid) = {
    gridFrom.getTilePosition(tile) match {
      case Some(x) => {
        if (gridTo == gridFrom) {
          // tile is moved within the same grid
          val tiles = gridFrom.tiles - (x) + ((newRow, newCol) -> tile)
          (Grid(gridFrom.ROWS, gridFrom.COLS, tiles),
            Grid(gridTo.ROWS, gridTo.COLS, tiles))
        } else {
          (Grid(gridFrom.ROWS, gridFrom.COLS, gridFrom.tiles - (x)),
            Grid(gridTo.ROWS, gridTo.COLS, gridTo.tiles + ((newRow, newCol) -> tile)))
        }
      }
      case None => throw new NoSuchElementException("Tile not found in rack.")
    }
  }

  def moveTile(gridFrom: Grid, gridTo: Grid, tile: Tile, newRow: Int, newCol: Int) = {
    val (f, t): (Grid, Grid) = moveTileImpl(gridFrom, gridTo, tile, newRow, newCol)

    if ((gridFrom eq field) && (gridTo eq getRack(game.activePlayer))) {
      setRack(t)
      setField(f)
      tilesMovedFromRackToGrid = tilesMovedFromRackToGrid.filter(x => x != tile)
    }

    if ((gridFrom eq field) && (gridTo eq field)) {
      setField(f)
    }

    if ((gridFrom eq getRack(game.activePlayer)) && (gridTo eq field)) {
      setRack(f)
      setField(t)
      tilesMovedFromRackToGrid = tilesMovedFromRackToGrid :+ tile
    }

    if ((gridFrom eq getRack(game.activePlayer)) && (gridTo eq getRack(game.activePlayer))) {
      setRack(t)
    }

    publish(new FieldChangedEvent)
    setGameStateAfterMoveTile()
  }

  private def setGameStateAfterMoveTile() = {

    if (tilesMovedFromRackToGrid.size == 0) {
      setGameState(GameState.WAITING)
    } else if (validateField()) {
      if (game.activePlayer.inFirstRound) {
        if (playerReachedMinLayOutPoints()) {
          setGameState(GameState.VALID)
        } else {
          setGameState(GameState.TO_LESS)
        }
      } else {
        if (getRack(game.activePlayer).tiles.size == 0) {
          setGameState(GameState.WON)
        } else {
          setGameState(GameState.VALID)
        }
      }
    } else {
      setGameState(GameState.INVALID)
    }
  }

  def sortRack(): Unit = {
    val sortedRack = sortRack(getRack(game.activePlayer))
    setRack(sortedRack)
    publish(new FieldChangedEvent)
  }

  private def sortRack(rack: Grid): Grid = {
    var tilesByColor = rack.tiles.map(x => x._2)
      .groupBy(x => x.color)
    while (tilesByColor.size > Const.RACK_ROWS) {
      // combine colors if there are to many
      val keyOfFirstElement = tilesByColor.keys.toList(0)
      val keyOfSecondElement = tilesByColor.keys.toList(1)
      val elements = tilesByColor(keyOfFirstElement) ++ tilesByColor(keyOfSecondElement)
      tilesByColor = tilesByColor + (keyOfSecondElement -> elements)
      tilesByColor = tilesByColor - tilesByColor.keys.toList(0)
    }
    var newMap: Map[(Int, Int), Tile] = Map.empty
    var row = 1
    tilesByColor.map(x => x._2.toList).foreach(listOfTiles => {
      var col = 1
      listOfTiles.sortBy(t => t.number).foreach(t => {
        newMap = newMap + ((row, col) -> t)
        col += 1
      })
      row += 1
    })
    Grid(Const.RACK_ROWS, Const.RACK_COLS, newMap)
  }
}

case class PlayerSwitchedEvent() extends Event

//case class RackChangedEvent() extends Event

case class ValidStateChangedEvent() extends Event

case class FieldChangedEvent() extends Event

case class GameStateChanged() extends Event

case class WinEvent(winningPlayer: Player) extends Event