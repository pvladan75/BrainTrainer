package com.program.braintrainer.chess.model

import kotlinx.serialization.Serializable

@Serializable
data class Solution(
    val moves: List<String> // Lista poteza u standardnoj notaciji (npr. "e2e4", "g1f3")
)