package com.program.braintrainer.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.program.braintrainer.R
import com.program.braintrainer.chess.model.Difficulty
import com.program.braintrainer.chess.model.Module

/**
 * Prevedeni nazivi modula i težina.
 *
 * Ranije su ove dve funkcije postojale u tri kopije - u AppNavigation, MainScreen
 * i ChessScreen - pa je dodavanje modula tražilo izmenu na tri mesta.
 */

@Composable
fun moduleTitle(module: Module): String = when (module) {
    Module.Module1 -> stringResource(R.string.module_1_title)
    Module.Module2 -> stringResource(R.string.module_2_title)
    Module.Module3 -> stringResource(R.string.module_3_title)
}

@Composable
fun difficultyLabel(difficulty: Difficulty): String = when (difficulty) {
    Difficulty.EASY -> stringResource(R.string.difficulty_easy)
    Difficulty.MEDIUM -> stringResource(R.string.difficulty_medium)
    Difficulty.HARD -> stringResource(R.string.difficulty_hard)
}
