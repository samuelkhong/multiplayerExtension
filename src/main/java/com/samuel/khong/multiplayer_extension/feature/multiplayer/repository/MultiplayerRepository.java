package com.samuel.khong.multiplayer_extension.feature.multiplayer.repository;

import com.samuel.khong.multiplayer_extension.feature.multiplayer.model.Multiplayer;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public class MultiplayerRepository extends MongoRepository<Multiplayer, String> {
}
