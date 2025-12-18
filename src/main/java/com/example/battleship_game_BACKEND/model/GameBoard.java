package com.example.battleship_game_BACKEND.model;


import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "game_board")
@Data

public class GameBoard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "game_board_id")
    private Long gameBoardId;

    @Column(name = "placement_matrix", nullable = false, columnDefinition = "TEXT")
    private String placementMatrix;

 }