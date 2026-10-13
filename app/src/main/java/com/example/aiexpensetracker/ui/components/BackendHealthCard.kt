package com.example.aiexpensetracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.aiexpensetracker.ui.viewmodel.BackendHealthUiState

/**
 * Temporary Step 2 UI Component:
 * Provides a minimal, non-intrusive card to verify backend connectivity.
 * Easily removed in future steps without affecting core expense/budget UI.
 */
@Composable
fun BackendHealthCard(
    uiState: BackendHealthUiState,
    onCheckBackend: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Backend Connection (Step 2)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    when (uiState) {
                        is BackendHealthUiState.Idle -> {
                            Text(
                                text = "Status: Not checked yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is BackendHealthUiState.Loading -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Checking backend health...",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        is BackendHealthUiState.Success -> {
                            Text(
                                text = "OK: ${uiState.service} (v${uiState.version})",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        is BackendHealthUiState.Error -> {
                            Text(
                                text = uiState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onCheckBackend,
                    enabled = uiState !is BackendHealthUiState.Loading
                ) {
                    Text("Check backend")
                }
            }
        }
    }
}
