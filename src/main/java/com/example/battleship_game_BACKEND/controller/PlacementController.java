package com.example.battleship_game_BACKEND.controller;

import com.example.battleship_game_BACKEND.dto.*;
import com.example.battleship_game_BACKEND.service.PlacementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/placement")
@RequiredArgsConstructor
public class PlacementController {

    private final PlacementService placementService;

    @PostMapping("/generate")
    public ResponseEntity<PlacementResponse> generatePlacement(
            @RequestBody PlacementRequest request) {
        try {
            PlacementResponse response = placementService.generatePlacement(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/save")
    public ResponseEntity<Void> savePlacement(
            @RequestBody SavePlacementRequest request) {
        try {
            placementService.saveUserPlacement(request);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/user-placements")
    public ResponseEntity<List<UserPlacementResponse>> getUserPlacements(
            @RequestParam Long playerId) { // Измените String на Long
        try {
            List<UserPlacementResponse> placements = placementService.getUserPlacements(playerId);
            return ResponseEntity.ok(placements);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}