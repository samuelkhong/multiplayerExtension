package com.samuel.khong.multiplayer_extension.feature.singleplayer.repository;

import com.khong.samuel.Mastermind_Reach.feature.singleplayer.model.Game;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for accessing and managing {@link Game} entities in a MongoDB database.
 *
 * This interface extends {@link MongoRepository}, providing built-in methods for CRUD operations
 * on {@link Game} objects. It uses the game's ID (String type) as the unique identifier.
 * No custom queries added.
 *
 * @see MongoRepository
 * @see Game
 */
@Repository
public interface GameRepository extends MongoRepository<Game, String> {

    // Method to find all games by playerId
    List<Game> findByPlayerId(String playerId);

}
