package com.samuel.khong.multiplayer_extension.feature.singleplayer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;


/**
 * Represents a game of Mastermind with a secret code that the player needs to guess.
 *
 * This class contains the essential elements for managing a Mastermind game, including the secret code, the game board, the current state of the game (win/loss), and player feedback.
 * The game also tracks the number of turns taken and the player's guesses.
 *
 * The game supports three difficulty levels: EASY, MEDIUM, and HARD.
 * It is designed to be used in a Spring Boot application, leveraging MongoDB for persistent storage, with the unique identifier `id` for each game.
 *
 * The game logic includes features like:
 * - Adding guesses and storing feedback for each turn
 * - Managing the board state
 * - Checking whether the game is over or won
 *
 * The class uses Lombok annotations for boilerplate code like constructors, getters, setters, and the builder pattern for object creation.
 *
 * Example usage:
 * - Start a new game by selecting a difficulty level.
 * - Make guesses and receive feedback on each guess (exact and partial matches).
 * - Continue making guesses until the game is won or the maximum number of turns is reached.
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "games")
public class Game {

    @Id
    private String id; // Unique identifier for the game
    private String playerId;
    private String secretCode; // The secret code to be guessed


    private String[][] board;

    public enum Difficulty {
        EASY, MEDIUM, HARD
    }

    @Builder.Default
    private boolean gameOver = false; // Indicates if the game is finished

    @Builder.Default
    private Difficulty difficulty = Difficulty.EASY; // Default difficulty is easy

    @Builder.Default
    private boolean won = false; // Indicates if the player has won

    // sets default to turn 1
    @Builder.Default
    private int turn = 1; // represents the current turn

    @Builder.Default
    private List<String> guesses = new ArrayList<>(); // List of guesses as strings

    @Builder.Default
    private String[] feedbacks = new String[10]; // Default array with 10 empty strings

    public void addGuess(String guess) {
        this.guesses.add(guess);
    }
    public void addFeedback(String feedback, int turn) {
        this.feedbacks[turn - 1] = feedback;
    }

}
