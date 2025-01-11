package com.samuel.khong.multiplayer_extension.feature.singleplayer.service;

import com.khong.samuel.Mastermind_Reach.feature.singleplayer.model.Feedback;
import com.khong.samuel.Mastermind_Reach.feature.singleplayer.model.Game;
import com.khong.samuel.Mastermind_Reach.feature.singleplayer.repository.GameRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Service class responsible for handling game-related operations.
 *
 * This service provides methods to start a new game, process guesses, retrieve game details,
 * and manage game state. It interacts with the {@link GameRepository} to persist game data
 * and execute business logic related to game flow.
 */
@Service
public class GameService {

    private final GameRepository gameRepository;


    public GameService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    /**
     * Starts a new game with the specified difficulty level and initializes the game state.
     *
     * The method performs the following actions:
     * - Sets the game difficulty based on the provided difficulty string.
     * - Initializes the game board and feedback arrays based on the difficulty level.
     * - Generates a secret code specific to the difficulty level.
     * - Creates a new {@link Game} instance with the generated secret code, difficulty, board, and initial game state.
     * - Saves the new game instance to the repository.
     *
     * @param difficulty A string representing the desired game difficulty. Valid values are "easy", "medium", and "hard".
     * @return The newly created and initialized {@link Game} object.
     * @throws IllegalArgumentException if an invalid difficulty level is provided.
     */
    @Transactional
    public Game startNewGame(String difficulty, String playerId) {
        // Set intial difficulty
        Game.Difficulty gameDifficulty = Game.Difficulty.valueOf(difficulty.toUpperCase());

        // Fill board based on difficulty
        String[][] board = intializeBoard(difficulty);

        String[] feedback = intializeFeedback();

        // Generate a new game with difficulty and persist it in DB
        Game newGame = Game.builder()
                .secretCode(generateSecretCode(difficulty)) // Generate secret code based on difficulty
                .playerId(playerId)
                .difficulty(gameDifficulty) // Set the difficulty
                .board(board) // Initialize the board
                .gameOver(false) // Game is not over initially
                .won(false) // Player has not won yet
                .turn(1) // Start from turn 1
                .feedbacks(feedback)
                .build();
        gameRepository.save(newGame);

        return newGame;
    }

    /**
     * Retrieves a game by its unique identifier.
     *
     * This method queries the game repository for a game using the provided game ID.
     * If the game is found, it returns the corresponding {@link Game} object.
     * If the game is not found, it throws an {@link IllegalArgumentException}.
     *
     * @param gameId The unique identifier of the game to retrieve.
     * @return The {@link Game} object corresponding to the provided game ID.
     * @throws IllegalArgumentException if no game with the provided ID is found.
     */
    public Game getGameById(String gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game with ID " + gameId + " not found"));
    }

    /**
     * Processes a player's guess in the game, updates the game state, and checks if the player has won.
     *
     * The method performs the following actions:
     * - Retrieves the game state using the provided game ID.
     * - Exits early if the game is already over.
     * - Validates that the guess is a valid list of integers (between 0 and 7).
     * - Converts the list of guesses into an integer array.
     * - Adds feedback based on the guess.
     * - Updates the game board with the new guess.
     * - Checks if the guess resulted in a win and updates the game state accordingly.
     * - Increments the turn counter and checks if the player has exceeded the maximum number of turns (10).
     * - Saves the updated game state to the repository.
     *
     * @param gameID The unique identifier of the game being played.
     * @param guess A list of integers representing the player's guess.
     * @return The updated {@link Game} object with the latest game state.
     */
    public Game processGuess(String gameID, List<Integer> guess) {
        Game game = getGameById(gameID);
        try {
            // Early exit if game already won or game over
            if (game.isGameOver()) {
                return game;
            }

            // Validate the guess if it is integers
            if (!isValidNumber(guess)) {
                // exit early if not valid
            }

            // Convert guess list into int[]
            int[] guessArr = guess.stream()
                    .mapToInt(Integer::intValue)  // Convert Integer to int
                    .toArray();

            // Add feedback to the game state
            addFeedback(guessArr, game);

            // Update the board with the guess
            updateBoard(guessArr, game);

            // Check if the guess resulted in a win
            if (checkWin(guessArr, game)) {
                game.setWon(true);  // Set won to true
                game.setGameOver(true);  // Set game over
            }
            // Increment the turn count
            game.setTurn(game.getTurn() + 1);

            // Check if the max turn count is exceeded
            if (game.getTurn() > 10) {
                game.setWon(false);  // Set false for player loss
                game.setGameOver(true);
                gameRepository.save(game);
                return game;
            }
            // Save the updated game state
            gameRepository.save(game);

            return game;
        } catch (Exception e) {
            e.printStackTrace();
            return game; // Or return an error
        }
    }

    /**
     * Calculates and adds feedback for the player's guess in the game.
     *
     * The method compares the player's guess with the secret code to generate feedback.
     * It converts the secret code into an integer array and calculates the feedback using the
     * provided guess and secret code. The feedback is then stored in the game state,
     * associating it with the current turn.
     *
     * @param guess An array of integers representing the player's current guess.
     * @param game The {@link Game} object that holds the current game state, including the secret code and turn information.
     */
    private void addFeedback(int[] guess, Game game) {
        // Convert the secret code from the game into an int array
        int[] secretCodeArr = new int[game.getSecretCode().length()];
        for (int i = 0; i < game.getSecretCode().length(); i++) {
            secretCodeArr[i] = Character.getNumericValue(game.getSecretCode().charAt(i));
        }
        // Create a Feedback object to store the result
        Feedback feedback = new Feedback();

        // Calculate feedback based on the guess and the secret code
        getFeedback(guess, secretCodeArr, feedback);

        // Convert the feedback object into a string and store it in the game
        game.addFeedback(feedbackToString(feedback), game.getTurn());
    }

    /**
     * Calculates the feedback for the player's guess compared to the secret code.
     *
     * This method determines two types of feedback:
     * - Exact matches: Correct numbers in the correct positions.
     * - Partial matches: Correct numbers in the wrong positions.
     *
     * The feedback is stored in the provided {@link Feedback} object. The method uses
     * two sets to track the indexes of exact and partial matches to avoid double-counting.
     *
     * @param guessArr An array of integers representing the player's guess.
     * @param secretCodeArr An array of integers representing the secret code.
     * @param feedback A {@link Feedback} object to store the result of the match calculations, including exact and partial matches.
     */
        public void getFeedback(int[] guessArr, int[] secretCodeArr, Feedback feedback) {
            int correctNumLoc = 0;
            int correctNumOnly = 0;

            // keeps matching indexes
            Set<Integer> guessIndexMatch = new HashSet<>();
            Set<Integer> secretIndexMatch = new HashSet<>();

            // correct number and correct location
            for (int i = 0; i < guessArr.length; i++) {
                if (guessArr[i] == secretCodeArr[i]) {
                    correctNumLoc++;
                    guessIndexMatch.add(i);
                    secretIndexMatch.add(i);
                }
            }
            //  find partial matches (correct number but wrong location)
            for (int i = 0; i < guessArr.length; i++) {
                if (guessIndexMatch.contains(i)) {
                    continue;  // Skip if found match already avoid doub;le count
                }

                // find  first occurrence of the number in the secret code not already matched
                for (int j = 0; j < secretCodeArr.length; j++) {
                    if (guessArr[i] == secretCodeArr[j] && !secretIndexMatch.contains(j)) {
                        correctNumOnly++;
                        secretIndexMatch.add(j);
                        break;  // Move on to the next guess character
                    }
                }
            }

            // Set the feedback counts for exact and partial matches
            feedback.setExactMatches(correctNumLoc);
            feedback.setPartialMatches(correctNumOnly);
        }

    /**
     * Converts the feedback object into a string representation.
     *
     * The method creates a formatted string showing the number of exact matches
     * (correct numbers in the correct positions) and partial matches (correct numbers in the wrong positions).
     *
     * @param feedback The {@link Feedback} object containing the feedback data to be converted.
     * @return A string representing the feedback in the format: "Exact Matches: [exactCount], Partial Matches: [partialCount]".
     */
    private String feedbackToString(Feedback feedback) {
        // Convert the feedback object into a string format
        return "Exact Matches: " + feedback.getExactMatches() + ", Partial Matches: " + feedback.getPartialMatches();
    }


    /**
     * Checks if the player's guess matches the secret code, resulting in a win.
     *
     * The method converts the secret code (which is stored as a string) into an integer array and compares it
     * with the player's guess (also an integer array). If both arrays match, the game is marked as won and completed.
     *
     * @param guessArr An array of integers representing the player's guess.
     * @param game The {@link Game} object containing the secret code and game state.
     * @return A boolean indicating whether the player's guess matches the secret code (true if the game is won, false otherwise).
     */
    private boolean checkWin(int[] guessArr, Game game) {
        // Convert secretCode (which is currently a string) to an int array
        String secret = game.getSecretCode();
        int[] secretArr = new int[secret.length()];

        // Convert the secret code string to an int array for comparison
        for (int i = 0; i < secret.length(); i++) {
            secretArr[i] = Character.getNumericValue(secret.charAt(i));
        }

        // If the guessArr matches the secretArr, game is won
        if (Arrays.equals(guessArr, secretArr)) {
            // Set game state to 'won'
            game.setWon(true);
            game.setGameOver(true); // Mark game as complete
            gameRepository.save(game);
            return true;
        }
        return false;
    }

    /**
     * Updates the game board with the player's current guess.
     *
     * The method converts the given guess (an integer array) into a string array and updates
     * the corresponding row on the game board. The row is determined by the current turn.
     * The board is updated in the game object, and the updated game state is saved to the repository.
     *
     * @param guess An array of integers representing the player's guess.
     * @param game The {@link Game} object that holds the current game state, including the board and turn information.
     */
    private void updateBoard(int[] guess, Game game) {
        String[][] board = game.getBoard();
        int turn = game.getTurn();

        // Convert int[] guess into a String array for each character (or split it as needed)
        String[] guessArr = new String[guess.length];
        for (int i = 0; i < guess.length; i++) {
            guessArr[i] = String.valueOf(guess[i]);  // Convert each int to String
        }

        // Fills board
        if (turn <= 10) {  // Fixed bug that did not edit last number
            board[10 - turn] = guessArr;
        }

        // Update the game board in the game object
        game.setBoard(board);

        // Save the updated game state (assuming gameRepository.save() is present)
        gameRepository.save(game);
    }

    /**
     * Validates a list of guesses to ensure they fall within the valid range of 0 to 7.
     *
     * The method checks whether the provided list of guesses is not null or empty.
     * It then iterates through each guess to ensure that each number is between 0 and 7 (inclusive).
     * If any guess is outside this range, the method returns false.
     *
     * @param guesses A list of integers representing the guesses to validate.
     * @return {@code true} if all guesses are valid (i.e., within the range 0-7);
     *         {@code false} if any guess is invalid or the list is null/empty.
     */
    private boolean isValidNumber(List<Integer> guesses) {
        // empty null check
        if (guesses == null || guesses.isEmpty()) {
            return false;
        }

        // iterate thorugh each guess
        for (Integer guess : guesses) {
            // 0 - 8 valid
            if (guess < 0 || guess >= 8) {
                return false;
            }
        }

        return true; // All guesses are valid
    }

    /**
     * Initializes an array to hold feedback for each row of the game board.
     *
     * The method creates an array of 10 elements, where each element is an
     * empty string. This array is intended to store feedback for each guess
     * made during the game.
     *
     * @return A string array of size 10, initialized with empty strings,
     *         representing the feedback for each row.
     */
    private String[] intializeFeedback() {
        String[] feedbacks = new String[10];
        Arrays.fill(feedbacks, ""); // Fill the array with empty strings
        return feedbacks;
    }

    /**
     * Initializes a game board based on the specified difficulty level.
     *
     * The method creates a 2D board array of size 10 rows and a number of columns
     * that varies based on the difficulty level:
     * - "easy" initializes a 4-column board.
     * - "medium" initializes a 6-column board.
     * - "hard" initializes an 8-column board.
     *
     * Each cell in the board is initialized with the placeholder value "#".
     *
     * @param gameDifficulty The difficulty level of the game. Valid values are:
     *                       "easy", "medium", "hard".
     * @return A 2D array of strings representing the initialized game board.
     */
    private String[][] intializeBoard(String gameDifficulty) {
        int colSize = 4;

        switch (gameDifficulty.toLowerCase()) {
            case "easy":
                colSize = 4;
                break;
            case "medium":
                colSize = 6;
                break;
            case "hard":
                colSize = 8;
                break;
            default:
                colSize = 4; // Default to easy
                break;
        }
        // Intialize  board size based on difficulty of game
        String[][] board = new String[10][colSize];

        // Fill with  #
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < colSize; j++) {
                board[i][j] = "#";
            }
        }
        return board;
    }

    /**
     * Generates a secret code based on the specified difficulty level.
     *
     * The secret code is generated by calling the Random.org API, which provides
     * random integers within a specified range. The length of the code depends
     * on the difficulty level:
     * - "easy" generates a 4-digit code.
     * - "medium" generates a 6-digit code.
     * - "hard" generates an 8-digit code.
     *
     * If the API call fails or encounters an error, a backup random generator
     * is used to generate the code instead.
     *
     * @param difficulty The difficulty level for the code. Valid values are:
     *                   "easy", "medium", "hard".
     * @return A string representing the generated secret code.
     */
    public String generateSecretCode(String difficulty) {
        int codeLength;
        // Set code length based on difficulty
        switch (difficulty.toLowerCase()) {
            case "easy":
                codeLength = 4;
                break;
            case "medium":
                codeLength = 6;
                break;
            case "hard":
                codeLength = 8;
                break;
            default:
                codeLength = 4; // Default to easy
                break;
        }
        String baseUrl = "https://www.random.org/integers/";
        String apiUrl = String.format(
                "%s?num=%d&min=0&max=7&col=1&base=10&format=plain&rnd=new", // set to format string so that can vary num length
                baseUrl, codeLength
        );

        RestTemplate restTemplate = new RestTemplate(); // Used to send request to API
        String response = "";

        // Calls the Random API
        try {
            ResponseEntity<String> apiResponse = restTemplate.getForEntity(apiUrl, String.class);
            response = apiResponse.getBody(); // Get response body

            // Clean string of newlines
            response = response.replace("\n", "");

        } catch (HttpClientErrorException e) {
            //  HTTP errors
            System.err.println("Using backup random gen. HTTP error occurred: " + e.getStatusCode());
            response = backupRandomGenerator(codeLength);
        } catch (ResourceAccessException e) {
            //  network or resource access issues
            System.err.println("Using backup random gen. Network error occurred: " + e.getMessage());
            response = backupRandomGenerator(codeLength);
        } catch (Exception e) {
            // Catch all errors
            System.err.println("Using backup random gen. Unexpected error occurred: " + e.getMessage());
            response = backupRandomGenerator(codeLength);
        }

        return response;
    }

    /**
     * Generates a random secret code using SecureRandom as a fallback when
     * the primary random number generator (API call) fails.
     *
     * The method generates a code of the specified length, where each digit
     * is randomly chosen from the range 0-7. SecureRandom is used to ensure
     * cryptographically strong randomness.
     *
     * @param codeLength The int length of the code to be generated.
     * @return A string representing the randomly generated secret code.
     */
    private String backupRandomGenerator(int codeLength) {
        // Using SecureRandom to generate a random code
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(codeLength);

        // gen secrect code
        for (int i = 0; i < codeLength; i++) {
            sb.append(random.nextInt(8)); // Random digit from 0-7
        }

        return sb.toString();
    }

    // Method to get all games for a specific playerId
    public List<Game> getGamesByPlayerId(String playerId) {
        return gameRepository.findByPlayerId(playerId);
    }


}

