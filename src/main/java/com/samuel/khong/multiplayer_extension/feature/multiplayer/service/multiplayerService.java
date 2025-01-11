package com.samuel.khong.multiplayer_extension.feature.multiplayer.service;

import com.samuel.khong.multiplayer_extension.feature.multiplayer.model.Multiplayer;
import com.samuel.khong.multiplayer_extension.feature.multiplayer.repository.MultiplayerRepository;
import com.samuel.khong.multiplayer_extension.feature.singleplayer.service.GameService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service class for handling multiplayer game operations.
 * Provides functionality to manage and interact with multiplayer games, including game initialization,
 * processing guesses, updating game status, and handling player turns.
 */
@Service
public class multiplayerService {
    // single player game Service
    private GameService gameService;
    private MultiplayerRepository multiplayerRepository;

    multiplayerService(GameService gameService, MultiplayerRepository multiplayerRepository) {
        this.gameService = gameService;
        this.multiplayerRepository = multiplayerRepository;
    }

    /**
     * Initializes a new multiplayer game by creating a specified number of single-player games
     * and storing their IDs in a list. The method then creates a new multiplayer game using the list of game IDs.
     *
     * @param playerCount The number of players in the multiplayer game. This determines how many single-player games will be created.
     */
    public void intializeMultiplayerGame(int playerCount) {
        // Stores a list of GameIds strings to be passed to startService()
        List<String> gameIds = new ArrayList<>();

        // make number of new single playerGames by playerCount
        for (int i = 0; i < playerCount; i++) {
            String currentGameID =  gameService.startNewGame(); // create a new game ID

            // add newly generated gameID to playerIds
            gameIds.add(currentGameID);
        }

        // create new multiplayer Game
        Multiplayer multiplayer = new Multiplayer();
        multiplayer.setGameIDs(gameIds); // set new  games based on multiplayer

    }

    /**
     * Processes a player's guess in the multiplayer game by updating the corresponding single-player game.
     * It validates the multiplayer game, retrieves the associated single-player game, processes the guess,
     * and then updates the multiplayer game status based on the result.
     *
     * @param guess The player's guess to be processed in the single-player game.
     * @param multiPlayerID The ID of the multiplayer game in which the guess is being made.
     * @throws Exception If no multiplayer game or corresponding single-player game is found, or any other error occurs during the guess processing.
     */

    public void sendGuess(String guess, String multiPlayerID) {
        // check if multiplayer game is valid
        String singleplayerId = getCurrentGameId(multiPlayerID);

        // use game Services to process single player guess
        gameService.processGuess(singleplayerId, guess);

        // Update the multiplayer game status based on the result
        updateMultiplayerGameStatus(multiPlayerID, singleplayerId);
    }


    /**
     * Updates the status of the multiplayer game based on the current player's performance in the single-player game.
     * If the player has not won, it changes the turn to the next player.
     * If the player has won, it ends the multiplayer game.
     *
     * @param currentMultiplayerGame The ID of the current multiplayer game.
     * @param currentSingleplayerGame The ID of the corresponding single-player game.
     */
    private void updateMultiplayerGameStatus(String currentMultiplayerGame, String currentSingleplayerGame) {
        // If the player didn't win, change the turn to the next player
        if (!gameService.checkIfWon(currentSingleplayerGame)) {
            changePlayerTurn(currentMultiplayerGame);
        }
        // end game in multiplayer object
        else {
            endGame(currentMultiplayerGame);

        }
    }


    /**
     * Changes the current player to the next player in the multiplayer game.
     * The player index cycles back to the first player when the last player is reached.
     *
     * @param multiplayerId The ID of the multiplayer game whose turn is being changed.
     */
    void changePlayerTurn(String multiplayerId) {
        // change the current player to next player
        Multiplayer multiplayer = multiplayerRepository.findById(multiplayerId);
        int playerAmount = multiplayer.getPlayerCount();
        int currentPlayer = multiplayer.getCurrentPlayer();

        // increments player count to next player or cycles back if at last player
        multiplayer.setCurrentPlayer((currentPlayer + 1) % playerAmount);
        multiplayerRepository.save(multiplayer); // save the current player

    }


    /**
     * Retrieves the current game ID for the player in the multiplayer game based on the player's index.
     * It checks if the current player index is valid and returns the corresponding game ID from the list of game IDs.
     *
     * @param multiplayerID The ID of the multiplayer game to retrieve the current game ID from.
     * @return The game ID for the current player.
     * @throws Exception If the current player index is invalid or out of bounds.
     */
    public String getCurrentGameId(String multiplayerID) {

        // get the current single player game from multiplayerID
        Multiplayer multiplayer = multiplayerRepository.findById(multiplayerID);

        //  get current player index
        int currentPlayerIndex = multiplayer.getCurrentPlayer();

        // get List of game ids
        List<String> gameIds = multiplayer.getGameIDs();

        //  Check if playerIndex is valid
        if (currentPlayerIndex >= 0 && currentPlayerIndex < gameIds.size()) {
            // Return the game ID at the current player's index
            return gameIds.get(currentPlayerIndex);
        } else {
            throw new Exception("Invalid current player index");
        }
    }

    /**
     * Ends the multiplayer game by setting the game status to "over".
     *
     * @param gameId The ID of the multiplayer game to be ended.
     */
    public void endGame(String gameId) {
        Multiplayer multiplayer = multiplayerRepository.findById(gameId);
        multiplayer.setGameOver(true);

        multiplayerRepository.save(multiplayer);

    }



}
