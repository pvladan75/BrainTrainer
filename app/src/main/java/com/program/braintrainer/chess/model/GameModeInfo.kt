package com.program.braintrainer.chess.model

import androidx.compose.ui.graphics.Color

// Ova data klasa služi samo za prikaz na početnom ekranu
data class GameModeInfo(
    val type: Module, // POVEZUJEMO SA TVOJIM POSTOJEĆIM Module ENUM-om
    val title: String,
    val description: String,
    val color: Color,
    val icon: Int
)