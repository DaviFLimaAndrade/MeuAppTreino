package com.davi.meutreino

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID

// ---------- Tipos ----------

enum class TipoCarga(val label: String, val mult: Double) {
    TOTAL("total", 1.0),
    POR_LADO("por lado", 2.0),
    CADA_HALTER("cada halter", 2.0),
    CORPO("peso corporal", 0.0),
}

data class Registro(val data: String, val kg: Double, val reps: String)

data class Exercicio(
    val id: String,
    val nome: String,
    val grupo: String,
    val series: String,
    val kg: Double,
    val tipo: TipoCarga,
    val nota: String,
    val historico: List<Registro>,
) {
    /** Carga total aproximada (por lado / cada halter contam em dobro; barra não incluída). */
    val kgEquivalente: Double get() = kg * tipo.mult
}

data class Treino(val id: String, val nome: String, val exercicios: List<Exercicio>)

data class ItemDieta(val id: String, val refeicao: String, val nome: String, val feito: Boolean)

data class AppData(
    val treinos: List<Treino>,
    val dieta: List<ItemDieta>,
    val dietaData: String,
    val aguaMl: Int,
    /** Data em que o ciclo começou (dia do Lower A). */
    val cicloInicio: String,
)

// ---------- Constantes ----------

val GRUPOS = listOf(
    "Peito", "Costas", "Ombros", "Bíceps", "Tríceps",
    "Quadríceps", "Posterior", "Glúteos", "Panturrilha", "Abdômen",
)

val REFEICOES = listOf(
    "08:30 · Café da manhã",
    "12:30 · Almoço",
    "16:00 · Pré-treino",
    "21:00 · Jantar",
)

const val META_AGUA_ML = 3000

/** Ciclo de 8 dias: posição do treino (índice na lista) em cada dia; -1 = descanso. */
private val CICLO = intArrayOf(0, 1, -1, -1, 2, 3, -1, -1)

/** Dia do ciclo em que cada treino cai (índice do treino -> dia). */
private val DIA_DO_TREINO = intArrayOf(0, 1, 4, 5)

// ---------- Utilidades ----------

private val ptBR: Locale = Locale.forLanguageTag("pt-BR")

fun novoId(): String = UUID.randomUUID().toString()

fun hoje(): String = LocalDate.now().toString()

fun fmtKg(v: Double): String =
    if (v % 1.0 == 0.0) v.toLong().toString() else String.format(ptBR, "%.1f", v)

fun fmtLitros(ml: Int): String = String.format(ptBR, "%.2f", ml / 1000.0)

fun fmtData(iso: String): String =
    runCatching { LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("dd/MM")) }.getOrDefault(iso)

fun parseKg(s: String): Double? = s.trim().replace(',', '.').toDoubleOrNull()

/** Índice do treino de hoje, ou -1 se for descanso. */
fun AppData.treinoDeHoje(): Int {
    val inicio = runCatching { LocalDate.parse(cicloInicio) }.getOrDefault(LocalDate.now())
    val dias = ChronoUnit.DAYS.between(inicio, LocalDate.now())
    val pos = Math.floorMod(dias, 8L).toInt()
    return CICLO[pos]
}

/** Próximo treino a partir de amanhã (para dias de descanso). */
fun AppData.proximoTreino(): Int {
    val inicio = runCatching { LocalDate.parse(cicloInicio) }.getOrDefault(LocalDate.now())
    val dias = ChronoUnit.DAYS.between(inicio, LocalDate.now())
    for (i in 1..8) {
        val pos = Math.floorMod(dias + i, 8L).toInt()
        if (CICLO[pos] >= 0) return CICLO[pos]
    }
    return 0
}

/** Ajusta o ciclo para que o treino [indice] caia hoje. */
fun AppData.definirHojeComo(indice: Int): AppData {
    val dia = DIA_DO_TREINO.getOrElse(indice) { 0 }
    return copy(cicloInicio = LocalDate.now().minusDays(dia.toLong()).toString())
}

// ---------- Alterações ----------

fun AppData.salvarExercicio(treinoId: String, ex: Exercicio): AppData = copy(
    treinos = treinos.map { t ->
        when {
            t.id != treinoId -> t
            t.exercicios.any { it.id == ex.id } ->
                t.copy(exercicios = t.exercicios.map { if (it.id == ex.id) ex else it })
            else -> t.copy(exercicios = t.exercicios + ex)
        }
    }
)

fun AppData.removerExercicio(treinoId: String, exId: String): AppData = copy(
    treinos = treinos.map { t ->
        if (t.id != treinoId) t else t.copy(exercicios = t.exercicios.filterNot { it.id == exId })
    }
)

fun AppData.alternarItem(id: String): AppData =
    copy(dieta = dieta.map { if (it.id == id) it.copy(feito = !it.feito) else it })

fun AppData.novoDia(data: String): AppData =
    copy(dieta = dieta.map { it.copy(feito = false) }, aguaMl = 0, dietaData = data)

// ---------- Dados iniciais (cargas de nov/2026) ----------

object Padrao {
    private fun ex(
        nome: String,
        grupo: String,
        series: String,
        kg: Double,
        tipo: TipoCarga,
        data: String,
        nota: String = "",
    ) = Exercicio(
        id = novoId(),
        nome = nome,
        grupo = grupo,
        series = series,
        kg = kg,
        tipo = tipo,
        nota = nota,
        historico = if (kg > 0) listOf(Registro(data, kg, "")) else emptyList(),
    )

    private fun item(refeicao: Int, nome: String) =
        ItemDieta(novoId(), REFEICOES[refeicao], nome, false)

    fun dados(): AppData {
        val la = "2026-11-14"
        val ua = "2026-11-15"
        val lb = "2026-11-18"
        val ub = "2026-11-19"

        val lowerA = Treino(
            novoId(), "Lower A", listOf(
                ex("Agachamento livre ou hack", "Quadríceps", "3x6–8", 10.0, TipoCarga.POR_LADO, la, "5 e 7 kg/lado = aquecimento"),
                ex("Leg press", "Quadríceps", "3x10–12", 40.0, TipoCarga.TOTAL, la),
                ex("Stiff", "Posterior", "3x8–10", 20.0, TipoCarga.TOTAL, la),
                ex("Cadeira extensora", "Quadríceps", "3x12–15", 20.0, TipoCarga.TOTAL, la),
                ex("Cadeira flexora", "Posterior", "3x12", 40.0, TipoCarga.TOTAL, la),
                ex("Panturrilha em pé", "Panturrilha", "3x12–15", 40.0, TipoCarga.TOTAL, la),
                ex("Abdominal no banco declinado", "Abdômen", "3x12–15", 0.0, TipoCarga.CORPO, la, "10 reps por série em 14/11"),
                ex("Elevação de pernas", "Abdômen", "3x10–12", 0.0, TipoCarga.CORPO, la, "8 e 6 reps em 14/11"),
            )
        )
        val upperA = Treino(
            novoId(), "Upper A", listOf(
                ex("Supino reto com barra", "Peito", "4x6–8", 5.0, TipoCarga.POR_LADO, ua),
                ex("Supino inclinado com halteres", "Peito", "3x8–10", 12.0, TipoCarga.CADA_HALTER, ua),
                ex("Crossover", "Peito", "3x12–15", 0.0, TipoCarga.POR_LADO, ua, "Em 15/11 fez peck deck com 25 kg"),
                ex("Remada curvada", "Costas", "3x8–10", 25.0, TipoCarga.TOTAL, ua),
                ex("Puxada frontal", "Costas", "3x10–12", 25.0, TipoCarga.TOTAL, ua),
                ex("Elevação lateral", "Ombros", "3x12–15", 7.0, TipoCarga.CADA_HALTER, ua),
                ex("Rosca direta", "Bíceps", "2x10–12", 2.5, TipoCarga.POR_LADO, ua),
                ex("Tríceps na polia", "Tríceps", "2x10–12", 18.0, TipoCarga.TOTAL, ua),
            )
        )
        val lowerB = Treino(
            novoId(), "Lower B", listOf(
                ex("RDL", "Posterior", "3x8–10", 70.0, TipoCarga.TOTAL, lb, "Barra fina"),
                ex("Hip thrust", "Glúteos", "3x10–12", 40.0, TipoCarga.TOTAL, lb),
                ex("Agachamento búlgaro", "Quadríceps", "3x8–10 cada perna", 5.0, TipoCarga.CADA_HALTER, lb),
                ex("Cadeira flexora", "Posterior", "3x12", 46.0, TipoCarga.TOTAL, lb),
                ex("Panturrilha no leg press", "Panturrilha", "3x12–15", 25.0, TipoCarga.POR_LADO, lb),
                ex("Abdominal no banco declinado", "Abdômen", "3x12–15", 0.0, TipoCarga.CORPO, lb, "3x12 em 18/11"),
                ex("Elevação de pernas", "Abdômen", "3x10–12", 0.0, TipoCarga.CORPO, lb, "3x6 em 18/11"),
            )
        )
        val upperB = Treino(
            novoId(), "Upper B", listOf(
                ex("Supino inclinado com halteres", "Peito", "4x6–8", 12.0, TipoCarga.CADA_HALTER, ub),
                ex("Supino reto", "Peito", "3x8–10", 7.5, TipoCarga.POR_LADO, ub),
                ex("Peck deck", "Peito", "3x12–15", 26.0, TipoCarga.TOTAL, ub, "Em 19/11 usou +1 placa: confira o peso"),
                ex("Remada baixa", "Costas", "3x10–12", 7.5, TipoCarga.POR_LADO, ub),
                ex("Puxada supinada", "Costas", "3x8–12", 26.0, TipoCarga.TOTAL, ub, "Em 19/11 usou +1 placa: confira o peso"),
                ex("Elevação lateral", "Ombros", "3x12–15", 7.0, TipoCarga.CADA_HALTER, ub, "13 reps em 19/11"),
                ex("Face pull", "Ombros", "2x15", 20.0, TipoCarga.TOTAL, ub, "2x10 em 19/11: próximo com 15 kg"),
                ex("Rosca martelo", "Bíceps", "2x10–12", 12.0, TipoCarga.CADA_HALTER, ub, "10 reps em 19/11"),
                ex("Tríceps testa", "Tríceps", "2x10–12", 10.0, TipoCarga.TOTAL, ub, "10 reps em 19/11"),
            )
        )

        val dieta = listOf(
            item(0, "2 ovos"),
            item(0, "Muçarela (1 fatia, 20 g)"),
            item(0, "Peito de peru (1 fatia, 20 g)"),
            item(0, "Pão francês (50 g)"),
            item(0, "Requeijão (20 g)"),
            item(0, "Banana grande"),
            item(0, "Leite integral (150 ml)"),
            item(0, "Nescau (20 g)"),
            item(0, "Café sem açúcar"),
            item(1, "Frango (130 g)"),
            item(1, "Arroz (300 g)"),
            item(1, "Feijão (150 g)"),
            item(1, "Legumes cozidos (mín. 100 g)"),
            item(1, "Salada de folhas"),
            item(2, "Hipercalórico (60 g)"),
            item(2, "Leite integral (200 ml)"),
            item(2, "Creatina (6 g)"),
            item(2, "Banana grande"),
            item(2, "Aveia (20 g)"),
            item(3, "Frango (130 g)"),
            item(3, "Arroz (300 g)"),
            item(3, "Legumes cozidos (mín. 100 g)"),
            item(3, "Salada de folhas"),
        )

        return AppData(
            treinos = listOf(lowerA, upperA, lowerB, upperB),
            dieta = dieta,
            dietaData = hoje(),
            aguaMl = 0,
            cicloInicio = la,
        )
    }
}
