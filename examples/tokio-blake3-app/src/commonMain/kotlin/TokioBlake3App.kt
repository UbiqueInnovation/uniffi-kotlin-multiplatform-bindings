/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.uniffi.examples.tokioblake3app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun TokioBlake3App() {
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
            var url by remember { mutableStateOf("https://example.com") }
            var body by remember { mutableStateOf<String?>(null) }
            val coroutineScope = rememberCoroutineScope()
            val retrieveFromUrl: () -> Unit = {
                coroutineScope.launch {
                    try {
                        body = null
                        val newBody = retrieveFrom(url)
                        body = newBody
                        snackbarHostState.showSnackbar("Hash: ${hashString(newBody)}")
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        println(e)
                        body = "Failed"
                    }
                }
            }

            LaunchedEffect(Unit) {
                retrieveFromUrl()
            }

            Column(
                Modifier.fillMaxSize().padding(8.dp),
                verticalArrangement = Arrangement.SpaceAround,
            ) {
                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = url,
                    onValueChange = { url = it },
                    placeholder = { Text("URL") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions { retrieveFromUrl() },
                    singleLine = true,
                )
                Button(retrieveFromUrl) {
                    Text("Retrieve using reqwest")
                }
                Text(
                    text = body ?: "Loading...",
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                )
            }
        }
    }
}
