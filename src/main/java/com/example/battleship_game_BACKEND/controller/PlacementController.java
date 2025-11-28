package com.example.battleship_game_BACKEND.controller;

import com.example.battleship_game_BACKEND.dto.*;
import com.example.battleship_game_BACKEND.service.PlacementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/placement")
public class PlacementController {

    private final PlacementService placementService;

    public PlacementController(PlacementService placementService) {
        this.placementService = placementService;
    }

    @PostMapping("/generate")
    public ResponseEntity<PlacementResponse> generatePlacement(
            @RequestBody PlacementRequest request) {
        try {
            PlacementResponse response = placementService.generatePlacement(request);
            return ResponseEntity.ok(response);
        } catch (Exception _) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/save")
    public ResponseEntity<Void> savePlacement(
            @RequestBody SavePlacementRequest request) {
        placementService.saveUserPlacement(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/user-placements")
    public ResponseEntity<List<UserPlacementResponse>> getUserPlacements(
            @RequestParam String userId) {
        List<UserPlacementResponse> placements = placementService.getUserPlacements(userId);
        return ResponseEntity.ok(placements);
    }
}