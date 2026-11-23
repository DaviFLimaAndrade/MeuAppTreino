package com.davi.meutreino

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class GrupoStat(val grupo: String, val media: Double, val max: Double, val n: Int)

fun calcularStats(data: AppData): List<GrupoStat> =
    data.treinos.flatMap { it.exercicios }
        .filter { it.tipo != TipoCarga.CORPO && it.kg > 0 }
        .groupBy { it.grupo }
        .map { (g, lista) ->
            val kgs = lista.map { it.kgEquivalente }
            GrupoStat(g, kgs.average(), kgs.max(), lista.size)
        }
        .sortedByDescending { it.media }

@Composable
fun CorpoScreen(store: Store, padding: PaddingValues) {
    val data = store.data
    val stats = remember(data) { calcularStats(data) }
    val maxMedia = stats.maxOfOrNull { it.media } ?: 1.0

    val neutro = MaterialTheme.colorScheme.surfaceVariant
    val baixo = MaterialTheme.colorScheme.primaryContainer
    val alto = MaterialTheme.colorScheme.primary
    val cores: Map<String, Color> = stats.associate { s ->
        val t = (s.media / maxMedia).toFloat().coerceIn(0f, 1f)
        s.grupo to lerp(baixo, alto, t)
    }
    val contorno = MaterialTheme.colorScheme.outlineVariant

    Column(
        Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Seu corpo", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Carga média por grupo muscular. Quanto mais forte a cor, mais peso você move ali.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Corpo(cores, costas = false, neutro = neutro, contorno = contorno, modifier = Modifier.fillMaxWidth())
                Text("Frente", style = MaterialTheme.typography.labelMedium)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Corpo(cores, costas = true, neutro = neutro, contorno = contorno, modifier = Modifier.fillMaxWidth())
                Text("Costas", style = MaterialTheme.typography.labelMedium)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("menos", style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.weight(1f).height(10.dp)
                    .background(Brush.horizontalGradient(listOf(baixo, alto)), RoundedCornerShape(5.dp))
            )
            Spacer(Modifier.width(8.dp))
            Text("mais", style = MaterialTheme.typography.labelSmall)
        }

        if (stats.isEmpty()) {
            Text("Registre cargas na aba Treinos para ver seu corpo aqui.")
        }
        stats.forEach { s ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(14.dp).background(cores[s.grupo] ?: neutro, CircleShape))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.grupo, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${s.n} exercício${if (s.n > 1) "s" else ""} · máx. ${fmtKg(s.max)} kg",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        "${fmtKg(Math.round(s.media * 10) / 10.0)} kg",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Text(
            "Carga total aproximada: o que é \"por lado\" ou \"cada halter\" conta em dobro. " +
                "O peso da barra não entra. Exercícios com peso corporal (abdômen) ficam de fora.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Silhueta simples desenhada em uma grade de 100 x 200 unidades.
 * Cada parte do corpo é pintada com a cor do grupo muscular (ou neutra, sem dados).
 */
@Composable
private fun Corpo(
    cores: Map<String, Color>,
    costas: Boolean,
    neutro: Color,
    contorno: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.aspectRatio(0.5f)) {
        val u = size.width / 100f
        fun cor(g: String) = cores[g] ?: neutro

        // base (pele / partes sem grupo)
        val base = contorno
        oval(41f, 4f, 59f, 24f, base, u)                 // cabeça
        parte(45f, 22f, 55f, 30f, base, u)               // pescoço
        parte(32f, 28f, 68f, 88f, base, u, 10f)          // tronco
        parte(18f, 62f, 28f, 90f, base, u)               // antebraço esq.
        parte(72f, 62f, 82f, 90f, base, u)               // antebraço dir.
        parte(33f, 84f, 67f, 100f, base, u, 8f)          // quadril
        parte(36f, 140f, 48f, 148f, base, u)             // joelho esq.
        parte(52f, 140f, 64f, 148f, base, u)             // joelho dir.
        parte(35f, 182f, 48f, 190f, base, u)             // pé esq.
        parte(52f, 182f, 65f, 190f, base, u)             // pé dir.

        // ombros (aparecem nas duas vistas)
        oval(21f, 27f, 37f, 41f, cor("Ombros"), u)
        oval(63f, 27f, 79f, 41f, cor("Ombros"), u)

        if (!costas) {
            parte(34f, 32f, 49.5f, 49f, cor("Peito"), u)
            parte(50.5f, 32f, 66f, 49f, cor("Peito"), u)
            parte(39f, 51f, 61f, 83f, cor("Abdômen"), u)
            parte(20f, 40f, 29f, 61f, cor("Bíceps"), u)
            parte(71f, 40f, 80f, 61f, cor("Bíceps"), u)
            parte(34f, 99f, 49f, 139f, cor("Quadríceps"), u)
            parte(51f, 99f, 66f, 139f, cor("Quadríceps"), u)
            parte(37f, 149f, 47f, 181f, cor("Panturrilha"), u)
            parte(53f, 149f, 63f, 181f, cor("Panturrilha"), u)
        } else {
            parte(34f, 31f, 66f, 70f, cor("Costas"), u)
            parte(20f, 40f, 29f, 61f, cor("Tríceps"), u)
            parte(71f, 40f, 80f, 61f, cor("Tríceps"), u)
            parte(34f, 86f, 49.5f, 104f, cor("Glúteos"), u)
            parte(50.5f, 86f, 66f, 104f, cor("Glúteos"), u)
            parte(34f, 105f, 49f, 139f, cor("Posterior"), u)
            parte(51f, 105f, 66f, 139f, cor("Posterior"), u)
            parte(36f, 149f, 48f, 181f, cor("Panturrilha"), u)
            parte(52f, 149f, 64f, 181f, cor("Panturrilha"), u)
        }
    }
}

private fun DrawScope.parte(l: Float, t: Float, r: Float, b: Float, c: Color, u: Float, raio: Float = 4f) {
    drawRoundRect(
        color = c,
        topLeft = Offset(l * u, t * u),
        size = Size((r - l) * u, (b - t) * u),
        cornerRadius = CornerRadius(raio * u, raio * u),
    )
}

private fun DrawScope.oval(l: Float, t: Float, r: Float, b: Float, c: Color, u: Float) {
    drawOval(color = c, topLeft = Offset(l * u, t * u), size = Size((r - l) * u, (b - t) * u))
}
