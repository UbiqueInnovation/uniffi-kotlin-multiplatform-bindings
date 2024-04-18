/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.uniffi.examples.audiocppapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun AudioCppApp() {
    MaterialTheme {
        val snackbarHostState = remember { SnackbarHostState() }
        Scaffold(
            snackbarHost = {
                SnackbarHost(snackbarHostState) {
                    Snackbar(
                        modifier = Modifier.padding(12.dp),
                        content = {
                            Text(
                                text = it.visuals.message,
                                maxLines = 1,
                            )
                        }
                    )
                }
            },
            modifier = Modifier.displayCutoutPadding(),
        ) {
            var frequencyString by remember { mutableStateOf("440") }
            var frequencyBeingPlayed: Float? by remember { mutableStateOf(null) }
            val coroutineScope = rememberCoroutineScope()
            val playSoundWithFrequency: () -> Unit = {
                coroutineScope.launch {
                    val frequency = frequencyString.toFloat()
                    frequencyBeingPlayed = frequency
                    try {
                        playSound(frequency)
                    } catch (e: PlaySoundException) {
                        snackbarHostState.showSnackbar("Failed to play wave!")
                    } finally {
                        frequencyBeingPlayed = null
                    }
                }
            }

            Column(Modifier.fillMaxSize().padding(8.dp)) {
                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = frequencyString,
                    onValueChange = { frequencyString = it },
                    placeholder = { Text("Frequency") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions { playSoundWithFrequency() },
                    singleLine = true,
                )
                Button(
                    onClick = playSoundWithFrequency,
                    enabled = frequencyBeingPlayed == null && frequencyString.toFloatOrNull() != null
                ) {
                    Text(
                        text = when {
                            frequencyBeingPlayed != null -> "Playing $frequencyBeingPlayed Hz Sine Wave"
                            frequencyString.toFloatOrNull() == null -> "Invalid Frequency"
                            else -> "Play $frequencyString Hz Sine Wave"
                        }
                    )
                }
            }
        }
    }
}
