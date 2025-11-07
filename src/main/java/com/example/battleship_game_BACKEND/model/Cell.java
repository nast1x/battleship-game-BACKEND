package com.example.battleship_game_BACKEND.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "cell")
@Data
public class Cell {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cell_id")
    private Long cellId;

    @OneToOne
    @JoinColumn(name = "game_board_id", unique = true, nullable = false)
    private GameBoard gameBoard;

    @Enumerated(EnumType.STRING)
    @Column(name = "cell_type", nullable = false)
    private CellType cellType;
}