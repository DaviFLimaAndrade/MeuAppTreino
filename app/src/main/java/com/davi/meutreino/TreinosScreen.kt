@file:OptIn(ExperimentalMaterial3Api::class)

package com.davi.meutreino

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun TreinosScreen(store: Store, padding: PaddingValues) {
    val data = store.data
    val deHoje = data.treinoDeHoje()
    var sel by rememberSaveable { mutableIntStateOf(if (deHoje >= 0) deHoje else data.proximoTreino()) }
    val idx = sel.coerceIn(0, (data.treinos.size - 1).coerceAtLeast(0))
    val treino = data.treinos.getOrNull(idx)

    var editando by remember { mutableStateOf<Exercicio?>(null) }
    var criando by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().padding(padding)) {
        Column(Modifier.fillMaxSize()) {
            ScrollableTabRow(selectedTabIndex = idx, edgePadding = 12.dp) {
                data.treinos.forEachIndexed { i, t ->
                    Tab(selected = i == idx, onClick = { sel = i }, text = { Text(t.nome) })
                }
            }
            if (treino != null) LazyColumn(
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    HojeBanner(
                        deHoje = deHoje,
                        nomeHoje = data.treinos.getOrNull(deHoje)?.nome,
                        nomeProximo = data.treinos.getOrNull(data.proximoTreino())?.nome,
                        selecionado = treino.nome,
                        selecionadoEhHoje = idx == deHoje,
                        onDefinirHoje = { store.update { it.definirHojeComo(idx) } },
                    )
                }
                items(treino.exercicios, key = { it.id }) { ex ->
                    ExercicioCard(ex) { editando = ex }
                }
            }
        }
        FloatingActionButton(
            onClick = { criando = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) { Icon(Icons.Filled.Add, contentDescription = "Adicionar exercício") }
    }

    if (treino == null) return
    val t: Treino = treino
    editando?.let { ex ->
        ExercicioDialog(
            inicial = ex,
            onDismiss = { editando = null },
            onSave = { novo -> store.update { it.salvarExercicio(t.id, novo) }; editando = null },
            onDelete = { store.update { it.removerExercicio(t.id, ex.id) }; editando = null },
        )
    }
    if (criando) {
        ExercicioDialog(
            inicial = null,
            onDismiss = { criando = false },
            onSave = { novo -> store.update { it.salvarExercicio(t.id, novo) }; criando = false },
            onDelete = null,
        )
    }
}

@Composable
private fun HojeBanner(
    deHoje: Int,
    nomeHoje: String?,
    nomeProximo: String?,
    selecionado: String,
    selecionadoEhHoje: Boolean,
    onDefinirHoje: () -> Unit,
) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                if (deHoje >= 0) "Hoje: $nomeHoje" else "Hoje: descanso",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (deHoje < 0 && nomeProximo != null) {
                Text("Próximo treino: $nomeProximo", style = MaterialTheme.typography.bodyMedium)
            }
            if (!selecionadoEhHoje) {
                TextButton(onClick = onDefinirHoje, contentPadding = PaddingValues(0.dp)) {
                    Text("Hoje vou fazer o $selecionado (ajustar ciclo)")
                }
            }
        }
    }
}

@Composable
private fun ExercicioCard(ex: Exercicio, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(ex.nome, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${ex.grupo} · ${ex.series}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (ex.nota.isNotBlank()) {
                    Text(
                        ex.nota,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                if (ex.tipo == TipoCarga.CORPO) {
                    Text("Corpo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                } else if (ex.kg <= 0.0) {
                    Text("—", style = MaterialTheme.typography.titleMedium)
                } else {
                    Text(
                        "${fmtKg(ex.kg)} kg",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(ex.tipo.label, style = MaterialTheme.typography.labelSmall)
                }
                ex.historico.lastOrNull()?.let {
                    Text(fmtData(it.data), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ExercicioDialog(
    inicial: Exercicio?,
    onDismiss: () -> Unit,
    onSave: (Exercicio) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var nome by remember { mutableStateOf(inicial?.nome ?: "") }
    var grupo by remember { mutableStateOf(inicial?.grupo ?: GRUPOS.first()) }
    var series by remember { mutableStateOf(inicial?.series ?: "3x10–12") }
    var kgTexto by remember {
        mutableStateOf(inicial?.kg?.takeIf { it > 0 }?.let { fmtKg(it) } ?: "")
    }
    var tipo by remember { mutableStateOf(inicial?.tipo ?: TipoCarga.TOTAL) }
    var reps by remember { mutableStateOf("") }
    var nota by remember { mutableStateOf(inicial?.nota ?: "") }
    var confirmarExclusao by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (inicial == null) "Novo exercício" else "Registrar / editar") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = nome, onValueChange = { nome = it },
                    label = { Text("Exercício") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Grupo muscular", style = MaterialTheme.typography.labelMedium)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    GRUPOS.forEach { g ->
                        FilterChip(selected = grupo == g, onClick = { grupo = g }, label = { Text(g) })
                    }
                }
                OutlinedTextField(
                    value = series, onValueChange = { series = it },
                    label = { Text("Séries x reps") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = kgTexto, onValueChange = { kgTexto = it },
                    label = { Text("Carga (kg)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    TipoCarga.entries.forEach { tc ->
                        FilterChip(selected = tipo == tc, onClick = { tipo = tc }, label = { Text(tc.label) })
                    }
                }
                OutlinedTextField(
                    value = reps, onValueChange = { reps = it },
                    label = { Text("Reps de hoje (ex.: 10, 10, 9)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = nota, onValueChange = { nota = it },
                    label = { Text("Observação") },
                    modifier = Modifier.fillMaxWidth(),
                )
                val hist = inicial?.historico.orEmpty()
                if (hist.isNotEmpty()) {
                    Text("Histórico", style = MaterialTheme.typography.labelMedium)
                    hist.takeLast(8).reversed().forEach { r ->
                        val repsTxt = if (r.reps.isBlank()) "" else " · ${r.reps}"
                        Text(
                            "${fmtData(r.data)} · ${fmtKg(r.kg)} kg$repsTxt",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = nome.isNotBlank(),
                onClick = {
                    val kg = parseKg(kgTexto) ?: 0.0
                    val base = inicial ?: Exercicio(novoId(), "", grupo, "", 0.0, tipo, "", emptyList())
                    val mudouCarga = inicial == null || kg != inicial.kg
                    val novoHist = if ((mudouCarga && kg > 0) || reps.isNotBlank()) {
                        base.historico + Registro(hoje(), kg, reps.trim())
                    } else base.historico
                    onSave(
                        base.copy(
                            nome = nome.trim(),
                            grupo = grupo,
                            series = series.trim(),
                            kg = kg,
                            tipo = tipo,
                            nota = nota.trim(),
                            historico = novoHist,
                        )
                    )
                },
            ) { Text("Salvar") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = { if (confirmarExclusao) onDelete() else confirmarExclusao = true }) {
                        Text(
                            if (confirmarExclusao) "Confirmar" else "Excluir",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        },
    )
}
