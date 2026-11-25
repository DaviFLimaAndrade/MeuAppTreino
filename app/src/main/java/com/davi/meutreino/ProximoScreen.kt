package com.davi.meutreino

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ProximoScreen(store: Store, padding: PaddingValues) {
    val data = store.data
    val context = LocalContext.current
    val (idxProx, emDias) = data.proximaSessao()
    var sel by rememberSaveable { mutableIntStateOf(idxProx) }
    val idx = sel.coerceIn(0, (data.treinos.size - 1).coerceAtLeast(0))
    val treino = data.treinos.getOrNull(idx)

    val quando = when (emDias) {
        0 -> "hoje"
        1 -> "amanhã"
        else -> LocalDate.now().plusDays(emDias.toLong())
            .format(DateTimeFormatter.ofPattern("EEEE, dd/MM", Locale.forLanguageTag("pt-BR")))
    }

    Column(Modifier.fillMaxSize().padding(padding)) {
        ScrollableTabRow(selectedTabIndex = idx, edgePadding = 12.dp) {
            data.treinos.forEachIndexed { i, t ->
                Tab(selected = i == idx, onClick = { sel = i }, text = { Text(t.nome) })
            }
        }
        if (treino != null) LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            if (idx == idxProx) "Próximo treino: ${treino.nome}" else treino.nome,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        if (idx == idxProx) Text(quando.replaceFirstChar { it.uppercase() })
                        Text(
                            "A meta usa a carga, as reps e como foi o último treino (fácil, médio ou pesado) " +
                                "para estimar sua força e calcular o peso da próxima sessão.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        TextButton(
                            onClick = {
                                val envio = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, data.textoExportacao())
                                }
                                context.startActivity(Intent.createChooser(envio, "Exportar cargas"))
                            },
                            contentPadding = PaddingValues(0.dp),
                        ) { Text("Exportar cargas") }
                    }
                }
            }
            items(treino.exercicios, key = { it.id }) { ex -> MetaCard(ex) }
        }
    }
}

@Composable
private fun MetaCard(ex: Exercicio) {
    val meta = ex.meta()
    val ultimo = ex.historico.lastOrNull()
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(ex.nome, style = MaterialTheme.typography.titleMedium)
                Text(
                    ex.series + if (ex.tipo != TipoCarga.CORPO) " · ${ex.tipo.label}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val ult = when {
                    ex.tipo == TipoCarga.CORPO -> ultimo?.reps?.takeIf { it.isNotBlank() }?.let { "Último: $it reps" }
                    ex.kg > 0 -> "Último: ${fmtKg(ex.kg)} kg" + (ultimo?.let { " (${fmtData(it.data)})" } ?: "")
                    else -> null
                }
                if (ult != null) {
                    Text(ult, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    meta.motivo,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (meta.sobe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text("Meta", style = MaterialTheme.typography.labelSmall)
                Text(
                    when {
                        ex.tipo == TipoCarga.CORPO -> if (meta.sobe) "+5 kg" else "Corpo"
                        meta.kg > 0 -> "${fmtKg(meta.kg)} kg"
                        else -> "—"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (meta.sobe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
                if (meta.sobe && ex.tipo != TipoCarga.CORPO) {
                    Text("▲ +${fmtKg(meta.kg - ex.kg)}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
