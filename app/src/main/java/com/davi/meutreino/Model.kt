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

/** sensacao: "facil", "ok", "pesado" ou "" (não informado). */
data class Registro(val data: String, val kg: Double, val reps: String, val sensacao: String = "")

val SENSACOES = listOf("facil" to "Fácil", "ok" to "Médio", "pesado" to "Pesado")

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

/** trocadoPor: substituto escolhido hoje (vazio = comeu o item original). */
data class ItemDieta(
    val id: String,
    val refeicao: String,
    val nome: String,
    val feito: Boolean,
    val trocadoPor: String = "",
)

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

/** Próxima sessão: índice do treino e em quantos dias (0 = hoje). */
fun AppData.proximaSessao(): Pair<Int, Int> {
    val inicio = runCatching { LocalDate.parse(cicloInicio) }.getOrDefault(LocalDate.now())
    val dias = ChronoUnit.DAYS.between(inicio, LocalDate.now())
    val hojeIso = hoje()
    for (i in 0..8) {
        val pos = Math.floorMod(dias + i, 8L).toInt()
        val c = CICLO[pos]
        if (c < 0) continue
        // Se o treino de hoje já foi registrado, a próxima sessão é a seguinte
        val jaFeito = i == 0 && treinos.getOrNull(c)?.exercicios
            ?.any { e -> e.historico.any { it.data == hojeIso } } == true
        if (!jaFeito) return c to i
    }
    return 0 to 0
}

// ---------- Metas ----------
// Conta usada por profissionais: estima a força máxima (1RM) pela fórmula de Epley,
// considerando as reps feitas + as reps que sobraram (RIR, pela sensação), e
// prescreve a carga para o mínimo da faixa de reps deixando 2 reps sobrando.
// Limite de ±10% por sessão e arredondado para o salto de peso do equipamento.

data class Meta(val kg: Double, val motivo: String, val sobe: Boolean)

/** Faixa de reps a partir de "3x8–10" (8 a 10) ou "2x15" (15 a 15). */
fun faixaReps(series: String): Pair<Int, Int>? {
    val m = Regex("x\\s*(\\d+)(?:\\s*[–-]\\s*(\\d+))?").find(series) ?: return null
    val min = m.groupValues[1].toIntOrNull() ?: return null
    val max = m.groupValues[2].toIntOrNull() ?: min
    return min to max
}

fun repsFeitas(s: String): List<Int> = Regex("\\d+").findAll(s).mapNotNull { it.value.toIntOrNull() }.toList()

private val INFERIORES = setOf("Quadríceps", "Posterior", "Glúteos", "Panturrilha")

/** Menor salto de peso do equipamento. */
fun Exercicio.passo(): Double = when (tipo) {
    TipoCarga.POR_LADO -> 2.5
    TipoCarga.CADA_HALTER -> if (kg < 10) 1.0 else 2.0
    TipoCarga.TOTAL -> if (grupo in INFERIORES) 5.0 else 2.5
    TipoCarga.CORPO -> 0.0
}

/** Reps em reserva estimadas pela sensação. */
private fun rir(sensacao: String) = when (sensacao) {
    "facil" -> 4.0
    "pesado" -> 0.5
    else -> 2.0
}

fun Exercicio.meta(): Meta {
    val (min, max) = faixaReps(series) ?: (8 to 12)
    val ultimo = historico.lastOrNull()
    val sens = ultimo?.sensacao.orEmpty()
    val digitadas = repsFeitas(ultimo?.reps.orEmpty())
    val bateuTopo = digitadas.isNotEmpty() && digitadas.all { it >= max }
    val abaixo = digitadas.isNotEmpty() && digitadas.any { it < min }
    val semInfo = sens.isBlank() && digitadas.isEmpty()

    if (tipo == TipoCarga.CORPO) {
        return if (bateuTopo || sens == "facil") Meta(0.0, "Foi fácil: segure uma anilha de 5 kg", true)
        else Meta(0.0, "Peso corporal, buscar $max reps", false)
    }
    if (kg <= 0.0) return Meta(0.0, "Defina a carga neste treino", false)

    // Sem reps digitadas, estima pelas sensações: fácil = topo, médio = meio, pesado = mínimo
    val reps = digitadas.minOrNull()?.toDouble() ?: when (sens) {
        "facil" -> max.toDouble()
        "pesado" -> min.toDouble()
        else -> (min + max) / 2.0
    }
    val e1rm = kg * (1 + (reps + rir(sens)) / 30.0)
    val alvo = (e1rm / (1 + (min + 2.0) / 30.0)).coerceIn(kg * 0.9, kg * 1.1)
    val p = passo()
    val diff = alvo - kg
    val novo = when {
        sens == "pesado" && !abaixo -> kg
        diff > 0 && (sens == "facil" || bateuTopo) -> kg + p * maxOf(1.0, Math.round(diff / p).toDouble())
        diff >= p * 0.75 -> kg + p * Math.round(diff / p)
        diff <= -p * 0.75 -> (kg - p * maxOf(1.0, Math.round(-diff / p).toDouble())).coerceAtLeast(p)
        else -> kg
    }
    val motivo = when {
        novo > kg && bateuTopo -> "Sobe: bateu o topo da faixa"
        novo > kg && sens == "facil" -> "Sobe: o último foi fácil"
        novo > kg -> "Sobe: sobrou força no último"
        novo < kg -> "Baixa: ficou abaixo de $min reps"
        sens == "pesado" -> "Mesma carga: o último foi pesado"
        semInfo -> "Mesma carga. Marque como foi para a meta ficar precisa"
        else -> "Mesma carga, buscar $max reps"
    }
    return Meta(novo, motivo, novo > kg)
}

/** Texto com as cargas atuais de cada treino, para exportar. */
fun AppData.textoExportacao(): String = buildString {
    append("Meu Treino: cargas em ").append(fmtData(hoje())).append("\n")
    treinos.forEach { t ->
        append("\n").append(t.nome).append("\n")
        t.exercicios.forEach { e ->
            val u = e.historico.lastOrNull()
            append("- ").append(e.nome).append(" (").append(e.series).append("): ")
            if (e.tipo == TipoCarga.CORPO) append("peso corporal")
            else if (e.kg > 0) append(fmtKg(e.kg)).append(" kg ").append(e.tipo.label)
            else append("sem carga")
            if (u != null) {
                append(" · último ").append(fmtData(u.data))
                if (u.reps.isNotBlank()) append(", reps ").append(u.reps)
                SENSACOES.toMap()[u.sensacao]?.let { append(", ").append(it.lowercase()) }
            }
            val m = e.meta()
            if (m.sobe && e.tipo != TipoCarga.CORPO) append(" → meta ").append(fmtKg(m.kg)).append(" kg")
            append("\n")
        }
    }
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
    copy(dieta = dieta.map { it.copy(feito = false, trocadoPor = "") }, aguaMl = 0, dietaData = data)

fun AppData.trocarItem(id: String, substituto: String): AppData =
    copy(dieta = dieta.map { if (it.id == id) it.copy(feito = substituto.isNotBlank(), trocadoPor = substituto) else it })

// ---------- Substituições do plano alimentar (Yasmin, 06/11/2026) ----------

private val TROCA_FRANGO = listOf(
    "Carne vermelha magra (120 g)",
    "Peixe branco (160 g)",
    "Carne suína magra (120 g)",
)
private val TROCA_ARROZ = listOf(
    "Macarrão cozido (300 g)",
    "Batata inglesa cozida (750 g)",
    "Batata inglesa assada (430 g)",
    "Batata-doce cozida (360 g)",
    "Batata-doce assada (270 g)",
    "Abóbora japonesa cozida (750 g)",
    "Mandioca cozida (300 g)",
    "Mandioca assada (210 g)",
)

/** Opções de troca equivalentes para um item, conforme a refeição. */
fun trocasPara(refeicao: String, nome: String): List<String> {
    val n = nome.lowercase()
    val cafe = refeicao == REFEICOES[0]
    val almoco = refeicao == REFEICOES[1]
    val jantar = refeicao == REFEICOES[3]
    return when {
        cafe && n.startsWith("pão francês") -> listOf("Pão de forma (2 fatias, 50 g)", "Aveia (40 g)", "Tapioca (60 g)")
        cafe && n.startsWith("banana") -> listOf("Mamão (250 g)", "Abacaxi (200 g)", "Melancia (380 g)")
        almoco && n.startsWith("frango") -> TROCA_FRANGO
        jantar && n.startsWith("frango") -> TROCA_FRANGO + "3 ovos (148 g)"
        almoco && n.startsWith("arroz") -> TROCA_ARROZ
        jantar && n.startsWith("arroz") -> TROCA_ARROZ + listOf(
            "Pão francês (2 unid., 100 g)", "Rap10 (3 unid., 120 g)",
            "Pão de forma (6 fatias, 150 g)", "Pão de hambúrguer (2 unid., 140 g)",
        )
        almoco && n.startsWith("feijão") -> listOf("Lentilha cozida (135 g)", "Grão-de-bico cozido (120 g)")
        else -> emptyList()
    }
}

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

        // Lower A: 14/11 e 22/11
        fun h(kg1: Double, kg2: Double, s2: String = "") =
            listOf(Registro(la, kg1, ""), Registro("2026-11-22", kg2, "", s2))
        val lowerA = Treino(
            novoId(), "Lower A", listOf(
                ex("Agachamento livre ou hack", "Quadríceps", "3x6–8", 10.0, TipoCarga.POR_LADO, la, "5 e 7 kg/lado = aquecimento")
                    .copy(historico = h(10.0, 10.0, "facil")),
                ex("Leg press", "Quadríceps", "3x10–12", 50.0, TipoCarga.TOTAL, la).copy(historico = h(40.0, 50.0, "facil")),
                ex("Stiff", "Posterior", "3x8–10", 25.0, TipoCarga.TOTAL, la).copy(historico = h(20.0, 25.0, "facil")),
                ex("Cadeira extensora", "Quadríceps", "3x12–15", 45.0, TipoCarga.TOTAL, la, "Confira a execução: subiu de 20 para 45")
                    .copy(historico = h(20.0, 45.0)),
                ex("Cadeira flexora", "Posterior", "3x12", 54.0, TipoCarga.TOTAL, la).copy(historico = h(40.0, 54.0)),
                ex("Panturrilha em pé", "Panturrilha", "3x12–15", 30.0, TipoCarga.POR_LADO, la).copy(historico = h(20.0, 30.0)),
                ex("Abdominal no banco declinado", "Abdômen", "3x12–15", 0.0, TipoCarga.CORPO, la)
                    .copy(historico = listOf(Registro("2026-11-22", 0.0, "", "facil"))),
                ex("Elevação de pernas", "Abdômen", "3x10–12", 0.0, TipoCarga.CORPO, la)
                    .copy(historico = listOf(Registro("2026-11-22", 0.0, "8"))),
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
