package io.dala.pawapaydemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.dala.pawapaydemo.ui.theme.PawaPayDemoTheme
import io.dala.pawapaykotlin.di.initKoin
import io.dala.pawapaykotlin.domain.TransactionType
import io.dala.pawapaykotlin.repository.PawaPayRepository
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    // Resolve repository using Koin
    private val repository: PawaPayRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize SDK (Do this once, usually in Application or MainActivity)
        initKoin(
            baseUrl = "https://api.sandbox.pawapay.io/v2/",
            apiToken = "eyJraWQiOiIxIiwiYWxnIjoiRVMyNTYifQ.eyJ0dCI6IkFBVCIsInN1YiI6Ijc1OTkiLCJtYXYiOiIxIiwiZXhwIjoyMDgyMDE5NjUxLCJpYXQiOjE3NjY0ODY4NTEsInBtIjoiREFGLFBBRiIsImp0aSI6IjRhZGFkOGRmLWE4MWQtNDlmMy1iZjU0LTQ3MTZjNmVkNTkyMSJ9.ep7MY5Hvu7o08_Jhk5E9DikvloUdxuD-IsZyQqyPk40TfYwzPDJuCwUfsrlBN6wkpWEgKEmkS9sZsRuxHA0PNg"
        )

        setContent {
            PawaPayDemoTheme {
                val scope = rememberCoroutineScope()
                var statusText by remember { mutableStateOf("Ready to Pay") }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp)
                    ) {
                        Text(text = statusText, style = MaterialTheme.typography.headlineSmall)

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(onClick = {
                            statusText = "Initiating payment..."
                            scope.launch {
                                // 2. Initiate a Deposit
                                val result = repository.pay(
                                    amount = "1200",
                                    phoneNumber = "256778529661",
                                    currency = "UGX",
                                    provider = "MTN_MOMO_UGA"
                                )

                                result.onSuccess { depositResponse ->
                                    statusText = "Request accepted. Polling status..."
                                    // 3. Poll for final status (COMPLETED/FAILED)
                                    pollPayment(depositResponse.depositId) { finalStatus ->
                                        statusText = finalStatus
                                    }
                                }.onFailure { error ->
                                    statusText = "Error: ${error.message}"
                                }
                            }
                        }) {
                            Text("Pay 1000 UGX")
                        }
                    }
                }
            }
        }
    }

    private suspend fun pollPayment(id: String, onResult: (String) -> Unit) {
        repository.pollTransactionStatus(id, TransactionType.DEPOSIT).fold(
            onSuccess = { response ->
                // response.data contains the final status objects
                onResult("Payment Successful: ${response.data?.status}")
            },
            onFailure = { error ->
                onResult("Payment Failed: ${error.message}")
            }
        )
    }
}