@file:OptIn(ExperimentalMaterial3Api::class)

package com.davi.meutreino

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

@Composable
fun DietaScreen(store: Store, padding: PaddingValues) {
    val data = store.data
    val total = data.dieta.size
    val feitos = data.dieta.count { it.feito }
    var adicionando by remember { mutableStateOf(false) }
    var editarLista by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().padding(padding)) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Dieta de hoje", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("$feitos de $total itens", style = MaterialTheme.typography.bodyMedium)
                        LinearProgressIndicator(
                            progress = { if (total == 0) 0f else feitos / total.toFloat() },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Água", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "${fmtLitros(data.aguaMl)} de ${fmtLitros(META_AGUA_ML)} L",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        LinearProgressIndicator(
                            progress = { (data.aguaMl / META_AGUA_ML.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                store.update { it.copy(aguaMl = (it.aguaMl - 250).coerceAtLeast(0)) }
                            }) { Text("−250") }
                            Button(onClick = { store.update { it.copy(aguaMl = it.aguaMl + 250) } }) { Text("+250 ml") }
                            Button(onClick = { store.update { it.copy(aguaMl = it.aguaMl + 500) } }) { Text("+500 ml") }
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { editarLista = !editarLista }) {
                        Text(if (editarLista) "Concluir edição" else "Editar lista")
                    }
                }
            }

            val refeicoes = (REFEICOES + data.dieta.map { it.refeicao }).distinct()
            refeicoes.forEach { r ->
                val itens = data.dieta.filter { it.refeicao == r }
                if (itens.isNotEmpty()) {
                    item(key = "h_$r") {
                        val ok = itens.count { it.feito }
                        Text(
                            "$r  ($ok/${itens.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                        )
                    }
                    items(itens, key = { it.id }) { item ->
                        ItemRow(
                            item = item,
                            editar = editarLista,
                            onToggle = { store.update { it.alternarItem(item.id) } },
                            onDelete = { store.update { d -> d.copy(dieta = d.dieta.filterNot { it.id == item.id }) } },
                        )
                    }
                }
            }

            item {
                TextButton(
                    onClick = { store.update { it.copy(dieta = it.dieta.map { i -> i.copy(feito = false) }) } },
                    modifier = Modifier.padding(top = 8.dp),
                ) { Text("Desmarcar tudo") }
            }
        }

        FloatingActionButton(
            onClick = { adicionando = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) { Icon(Icons.Filled.Add, contentDescription = "Adicionar alimento") }
    }

    if (adicionando) {
        NovoItemDialog(
            onDismiss = { adicionando = false },
            onSave = { refeicao, nome ->
                store.update { it.copy(dieta = it.dieta + ItemDieta(novoId(), refeicao, nome, false)) }
                adicionando = false
            },
        )
    }
}

@Composable
private fun ItemRow(item: ItemDieta, editar: Boolean, onToggle: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = item.feito, onCheckedChange = { onToggle() })
        Text(
            item.nome,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            textDecoration = if (item.feito) TextDecoration.LineThrough else null,
            color = if (item.feito) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
        if (editar) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Close, contentDescription = "Remover", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun NovoItemDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var refeicao by remember { mutableStateOf(REFEICOES.first()) }
    var nome by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar alimento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nome, onValueChange = { nome = it },
                    label = { Text("Alimento e quantidade") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Refeição", style = MaterialTheme.typography.labelMedium)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    REFEICOES.forEach { r ->
                        FilterChip(
                            selected = refeicao == r,
                            onClick = { refeicao = r },
                            label = { Text(r.substringAfter("· ")) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = nome.isNotBlank(), onClick = { onSave(refeicao, nome.trim()) }) { Text("Adicionar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
