package com.samuel.khong.multiplayer_extension.feature.singleplayer.controller;


import com.khong.samuel.Mastermind_Reach.core.Authentication.model.CustomUserDetails;
import com.khong.samuel.Mastermind_Reach.feature.singleplayer.model.Game;
import com.khong.samuel.Mastermind_Reach.feature.singleplayer.service.GameService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The {@code GameController} class is responsible for handling requests related to a single-player game.
 * It handles the logic for starting a new game, processing guesses, and displaying the current game state.
 */
@Controller
@RequestMapping("/singleplayer")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }


    /**
     * Displays the game selection page for the user.
     *
     * This method handles the HTTP GET request for the "/start" endpoint. It simply returns the view name
     * for the game selection page, allowing the user to choose a game difficulty before starting a new game.
     *
     * @return The name of the Thymeleaf template that renders the game selection page.
     */
    @GetMapping ("/start")
    public String startGame(@AuthenticationPrincipal CustomUserDetails customUserDetails, Model model) {


        // get all games for the current user from the GameService
        List<Game> games = gameService.getGamesByPlayerId(customUserDetails.getUserId());

        // Add all games and userID to pass to template
        model.addAttribute("games", games);
        // Add the userId to the model
        model.addAttribute("userId", customUserDetails.getUserId());



        return "singleplayer/gameSelect";
    }

    /**
     * Starts a new game based on the selected difficulty and redirects to the game page.
     *
     * This method initializes a new game by calling the service to create a game with the specified difficulty.
     * After the game is created, the game object is added to the model and the user is redirected to the game page
     * using the newly generated game ID. If any error occurs during the game creation, the user is redirected
     * to an error page.
     *
     * @param difficulty The difficulty level selected by the user (e.g., "easy", "medium", "hard").
     * @param model The {@link Model} object used to pass the game data to the view (if needed).
     * @return A redirect to the game page with the newly created game's ID or an error page if something goes wrong.
     */
    @PostMapping("/start")
    public String startNewGame(@RequestParam String difficulty, @AuthenticationPrincipal CustomUserDetails customUserDetails, Model model) {
        try {
            // get the userId
            String userId = customUserDetails.getUserId();
            System.out.println(userId);
            // Create a new game based on the selected difficulty
            Game newGame = gameService.startNewGame(difficulty, userId);

            // Add the new game to the model (if you need to use it for something else)
            model.addAttribute("game", newGame);

            // Redirect to the game page using the game ID
            return "redirect:/singleplayer/game/" + newGame.getId();  // Redirects to the /game/{gameId} URL
        } catch (Exception e) {
            e.printStackTrace();
            return "error";  // Return an error page if something goes wrong
        }
    }

    @PostMapping("/loadGame")
    public String redirectGame(@RequestParam("gameId") String gameId, Model model) {
        return "redirect:/singleplayer/game/" + gameId;

    }


    /**
     * Loads a game by its ID and prepares the model for rendering the game page.
     *
     * This method retrieves the game object based on the provided game ID. If the game is found, it adds the
     * game object to the model for rendering in the Thymeleaf template. If the game cannot be found, it returns
     * an error page. The method then returns the name of the Thymeleaf template to display the game details to the user.
     *
     * @param gameId The ID of the game to be loaded.
     * @param model The {@link Model} object used to add the game data to the view for rendering.
     * @return The name of the Thymeleaf template to display the game, or an error page if the game is not found.
     */

    @GetMapping("/game/{gameId}")
    public String loadGame(@PathVariable String gameId, Model model) {
        // Retrieve the game object by its ID
        Game game = gameService.getGameById(gameId);

        if (game == null) {
            return "error";  // Return error if no game is found
        }

        // Add the game object to the model for the Thymeleaf template
        model.addAttribute("game", game);

        // Return the game template
        return "singleplayer/game";  // This is the Thymeleaf template to display the game
    }

    /**
     * Handles the submission of a player's guess in the game.
     *
     * This method processes the player's guess, validates and extracts the guess values from the request parameters,
     * updates the game state, and redirects the player back to the game page. The maximum number of guesses allowed
     * is determined by the game's difficulty level (easy, medium, or hard). The method then calls the game service
     * to process the guess and updates the model with the game state to render the updated view.
     *
     * @param gameId The ID of the game being played.
     * @param allParams A map containing all request parameters, including the guesses.
     * @param model The {@link Model} object used to add attributes for rendering the view.
     * @return A redirect URL to the game page with the updated game state.
     */
    @PostMapping("/guess")
    public String handleGuess(@RequestParam("gameId") String gameId,
                              @RequestParam Map<String, String> allParams,
                              Model model) {

        Game game = gameService.getGameById(gameId);

        // Initialize the maxGuesses variable based on game difficulty
        int maxGuesses = 0;

        // find the max guesses per difficulty
        switch (game.getDifficulty()) {
            case EASY:
                    maxGuesses = 4;
                break;
            case MEDIUM:
                maxGuesses = 6;
                break;
            case HARD:
                maxGuesses = 8;
                break;
            default:
                maxGuesses = 4;
                break;
        }
        // list to store the guesses in order. Important since order is necessary for check
        List<Integer> guesses = new ArrayList<>();

        // Get the values from each guess
        for (int i = 1; i <= maxGuesses; i++) {
            String guessStr = allParams.get("guess" + i); // Get the value for guess1.

            if (guessStr != null && !guessStr.isEmpty()) {
                int guess = Integer.parseInt(guessStr);
                guesses.add(guess);
            }
        }

        // Send guesses to be processed
        gameService.processGuess(gameId, guesses);

        // Add the game data to the model for rendering the view
        model.addAttribute("game", game);

        return "redirect:/singleplayer/game/" + gameId;  // after each submit, redirects back to game page
    }

}

