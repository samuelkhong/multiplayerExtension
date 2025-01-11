package com.samuel.khong.multiplayer_extension.feature.multiplayer.model;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Document("Multiplayer")
public class Multiplayer {

    int id; // stores unique generated ID for mongodb

    int playerCount; // holds the amount of players that can join the game
    List<String> gameIDs = new ArrayList<>(); // holds the ID tying each single player game to the game session
    boolean gameOver = false; // stores game state
    int currentPlayer = 0; // represents the state of which player is currently up to play
}
