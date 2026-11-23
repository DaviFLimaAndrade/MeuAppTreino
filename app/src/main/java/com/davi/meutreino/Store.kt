package com.davi.meutreino

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

/** Guarda tudo no próprio celular (SharedPreferences, em JSON). */
class Store(context: Context) {
    private val prefs = context.getSharedPreferences("meu_treino", Context.MODE_PRIVATE)

    var data: AppData by mutableStateOf(carregar())
        private set

    fun update(f: (AppData) -> AppData) {
        data = f(data)
        salvar()
    }

    /** Zera a dieta e a água quando vira o dia. */
    fun checkNewDay() {
        val d = hoje()
        if (data.dietaData != d) update { it.novoDia(d) }
    }

    private fun carregar(): AppData {
        val json = prefs.getString(KEY, null) ?: return Padrao.dados()
        return runCatching { fromJson(JSONObject(json)) }.getOrElse { Padrao.dados() }
    }

    private fun salvar() {
        prefs.edit().putString(KEY, toJson(data).toString()).apply()
    }

    companion object {
        private const val KEY = "dados_v1"

        fun toJson(d: AppData): JSONObject = JSONObject()
            .put("dietaData", d.dietaData)
            .put("aguaMl", d.aguaMl)
            .put("cicloInicio", d.cicloInicio)
            .put("treinos", JSONArray().apply {
                d.treinos.forEach { t ->
                    put(JSONObject()
                        .put("id", t.id)
                        .put("nome", t.nome)
                        .put("exercicios", JSONArray().apply {
                            t.exercicios.forEach { e ->
                                put(JSONObject()
                                    .put("id", e.id)
                                    .put("nome", e.nome)
                                    .put("grupo", e.grupo)
                                    .put("series", e.series)
                                    .put("kg", e.kg)
                                    .put("tipo", e.tipo.name)
                                    .put("nota", e.nota)
                                    .put("historico", JSONArray().apply {
                                        e.historico.forEach { r ->
                                            put(JSONObject().put("d", r.data).put("kg", r.kg).put("r", r.reps))
                                        }
                                    }))
                            }
                        }))
                }
            })
            .put("dieta", JSONArray().apply {
                d.dieta.forEach { i ->
                    put(JSONObject()
                        .put("id", i.id)
                        .put("refeicao", i.refeicao)
                        .put("nome", i.nome)
                        .put("feito", i.feito))
                }
            })

        fun fromJson(o: JSONObject): AppData {
            val treinos = o.getJSONArray("treinos").objetos().map { t ->
                Treino(
                    id = t.getString("id"),
                    nome = t.getString("nome"),
                    exercicios = t.getJSONArray("exercicios").objetos().map { e ->
                        Exercicio(
                            id = e.getString("id"),
                            nome = e.getString("nome"),
                            grupo = e.optString("grupo", "Peito"),
                            series = e.optString("series", ""),
                            kg = e.optDouble("kg", 0.0),
                            tipo = runCatching { TipoCarga.valueOf(e.optString("tipo")) }
                                .getOrDefault(TipoCarga.TOTAL),
                            nota = e.optString("nota", ""),
                            historico = (e.optJSONArray("historico") ?: JSONArray()).objetos().map { r ->
                                Registro(r.optString("d"), r.optDouble("kg", 0.0), r.optString("r", ""))
                            },
                        )
                    },
                )
            }
            val dieta = (o.optJSONArray("dieta") ?: JSONArray()).objetos().map { i ->
                ItemDieta(
                    id = i.getString("id"),
                    refeicao = i.optString("refeicao", REFEICOES[0]),
                    nome = i.optString("nome"),
                    feito = i.optBoolean("feito", false),
                )
            }
            return AppData(
                treinos = treinos,
                dieta = dieta,
                dietaData = o.optString("dietaData", hoje()),
                aguaMl = o.optInt("aguaMl", 0),
                cicloInicio = o.optString("cicloInicio", "2026-11-14"),
            )
        }

        private fun JSONArray.objetos(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
    }
}
