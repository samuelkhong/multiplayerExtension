package com.samuel.khong.multiplayer_extension.feature.multiplayer.controller;

import com.samuel.khong.multiplayer_extension.feature.multiplayer.service.multiplayerService;
import com.samuel.khong.multiplayer_extension.feature.singleplayer.service.GameService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

@Controller
public class multiplayerController {
    multiplayerService multiplayerService;
    GameService gameService;

    multiplayerController(multiplayerService multiplayerService, GameService gameService) {
        this.multiplayerService = multiplayerService;
        this.gameService = gameService;
    }

    @RequestMapping("multiplayer")

    // endpoint for starting a new game takes in query parameters for how many players you want
    // takes url parameters players=""
    @GetMapping("/start")
    public void startMultiplayerGame(@RequestParam("players") int playerCount ) {

        // error check if playerCount is Valid
        if (playerCount < 1) {
            throw new IllegalArgumentException("Player count must be at least 1.");
        }

        // call startMultiplayer to intalize new multiplayer game
        multiplayerService.intializeMultiplayerGame(playerCount);
    }

    // takes get reuqest for multiplayer guess.  Example URL: /guess?guess=1234&multiplayerId=abcd1234
    @GetMapping("/guess")
    public void sendGuess(@RequestParam("guess") String guess, @RequestParam("multiplayerId") String multiplayerID) {
        // sends currently url guess and processes to correct singleplayer game
        multiplayerService.sendGuess(guess, multiplayerID);

    }

}
