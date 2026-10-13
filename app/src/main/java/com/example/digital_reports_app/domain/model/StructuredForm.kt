package com.example.digital_reports_app.domain.model

import org.json.JSONArray
import org.json.JSONObject

/** Tipo de fila dentro de una sección tipo tabla. */
enum class RowKind(val positive: String, val negative: String) {
    YES_NO("SI", "NO"),
    COMPLY("CUMPLE", "NO CUMPLE"),
    /** Medición: valor medido + cumple / no cumple. */
    MEASURE("CUMPLE", "NO CUMPLE")
}

data class FormItem(val id: String, val label: String)

data class FormSection(
    val id: String,
    val title: String,
    val kind: RowKind,
    val items: List<FormItem>
)

data class HeaderField(val id: String, val label: String, val isDate: Boolean = false)

/** Una "página" del formato (ej. Recepción de lote / Entrega de lote). */
data class FormPage(
    val id: String,
    val title: String,
    val headerFields: List<HeaderField>,
    val sections: List<FormSection>,
    val commentsLabel: String,
    val signatureLabels: List<String>
)

data class StructuredForm(
    val id: String,
    val name: String,
    val category: String,
    val organization: String,
    val documentTitle: String,
    val revision: String,
    val code: String,
    val footer: String,
    val pages: List<FormPage>
)

/** Convención de claves para las respuestas (Map<String,String>). */
object FormKeys {
    fun answer(itemId: String) = "$itemId.ans"
    fun value(itemId: String) = "$itemId.val"
    fun obs(itemId: String) = "$itemId.obs"
    fun header(pageId: String, fieldId: String) = "$pageId.h.$fieldId"
    fun comments(pageId: String) = "$pageId.comments"
    fun sigName(pageId: String, index: Int) = "$pageId.sig$index.name"
    fun sigStrokes(pageId: String, index: Int) = "$pageId.sig$index.strokes"
}

fun answersToJson(answers: Map<String, String>): String {
    val obj = JSONObject()
    answers.forEach { (k, v) -> if (v.isNotEmpty()) obj.put(k, v) }
    return obj.toString()
}

fun jsonToAnswers(json: String): Map<String, String> {
    if (json.isBlank()) return emptyMap()
    return try {
        val obj = JSONObject(json)
        val map = mutableMapOf<String, String>()
        obj.keys().forEach { key -> map[key] = obj.optString(key, "") }
        map
    } catch (_: Exception) {
        emptyMap()
    }
}

/** Trazos de firma: lista de trazos, cada trazo lista de puntos normalizados [0..1]. */
fun strokesToJson(strokes: List<List<Pair<Float, Float>>>): String {
    val arr = JSONArray()
    strokes.forEach { stroke ->
        val s = JSONArray()
        stroke.forEach { (x, y) -> s.put(JSONArray().put(x.toDouble()).put(y.toDouble())) }
        arr.put(s)
    }
    return arr.toString()
}

fun jsonToStrokes(json: String): List<List<Pair<Float, Float>>> {
    if (json.isBlank()) return emptyList()
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val s = arr.getJSONArray(i)
            (0 until s.length()).map { j ->
                val p = s.getJSONArray(j)
                p.getDouble(0).toFloat() to p.getDouble(1).toFloat()
            }
        }
    } catch (_: Exception) {
        emptyList()
    }
}

object FormCatalog {

    private fun items(prefix: String, vararg labels: String) =
        labels.mapIndexed { i, l -> FormItem("$prefix${i + 1}", l) }

    val recepcionEntregaLotes = StructuredForm(
        id = "2-CM-F005",
        name = "Recepción y Entrega de Lotes (Cosecha Mecanizada)",
        category = "Inspección",
        organization = "Pantaleon",
        documentTitle = "FORMATO DE RECEPCIÓN Y ENTREGA DE FINCAS LOTES\nCOSECHA MECANIZADA",
        revision = "Actualización 3: 11/09/2025",
        code = "2-CM-F005",
        footer = "Lugar: Oficina de Cosecha Mecanizada   Medio: Papel   Responsable: Supervisor Cosecha Mecanizada   Retención: 6 meses   Disposición: Destruir",
        pages = listOf(
            FormPage(
                id = "rec",
                title = "RECEPCIÓN DE LOTE ANTES DE INICIAR CORTE",
                headerFields = listOf(
                    HeaderField("fecha", "FECHA", isDate = true),
                    HeaderField("finca_lote", "FINCA LOTE"),
                    HeaderField("nombre_finca", "NOMBRE DE FINCA"),
                    HeaderField("sup_entrega", "SUPERVISOR O GUARDA FINCA QUE ENTREGA"),
                    HeaderField("sup_cosecha", "SUPERVISOR DE COSECHA")
                ),
                sections = listOf(
                    FormSection(
                        "rec_car", "Características del lote", RowKind.YES_NO,
                        items(
                            "rec_car",
                            "La Finca lote cuenta con acceso adecuado ?",
                            "Tiene cruceros intermedios adecuados ?",
                            "Tiene bahías de descargue (trasiego de caña) ?",
                            "El lote dispone de rondas mayores o 4 metros y libres de obstáculos?"
                        )
                    ),
                    FormSection(
                        "rec_obs", "Obstáculos dentro del lote", RowKind.YES_NO,
                        items(
                            "rec_obs",
                            "El lote presenta exceso de humedad por riego?",
                            "El lote tiene señalizado y despejado los hidrantes dentro del campo?",
                            "El lote está libre de tubería de riego dentro del área productiva?",
                            "El lote está libre de obstáculos (causes, troncos, tendidos eléctricos, bases de concreto, pozos, piedras, árboles, etc.)?"
                        )
                    ),
                    FormSection(
                        "rec_med", "Mediciones antes del Corte", RowKind.MEASURE,
                        items(
                            "rec_med",
                            "% De cepa expuesta antes del corte (debe ser 0%)",
                            "% De espacios vacíos antes del corte mayor a 1metro",
                            "% De despoblación antes del corte =< 7% (suma de espacios vacíos más cepa expuesta)",
                            "Presencia de caña podrida y Seca",
                            "Altura del lomo del surco. (No mayor de 20 cm)",
                            "Surcos por debajo del nivel del corte. (No mayor de 3 cm)",
                            "Presencia de maleza en campo"
                        )
                    )
                ),
                commentsLabel = "Cualquier hallazgo o condición no descrita anteriormente detallarlo aquí:",
                signatureLabels = listOf(
                    "FIRMA ENTREGA EL LOTE PARA COSECHA",
                    "FIRMA RECIBIDO EL SUPERVISOR DE COSECHA"
                )
            ),
            FormPage(
                id = "ent",
                title = "ENTREGA DE LOTE COSECHADO",
                headerFields = listOf(
                    HeaderField("fecha", "FECHA", isDate = true),
                    HeaderField("finca_lote", "FINCA LOTE"),
                    HeaderField("nombre_finca", "NOMBRE DE FINCA"),
                    HeaderField("sup_recibe", "SUPERVISOR O GUARDA FINCA QUE RECIBE"),
                    HeaderField("sup_entrega_mec", "SUPERVISOR QUE ENTREGA MECANIZADA")
                ),
                sections = listOf(
                    FormSection(
                        "ent_par", "PARÁMETROS DE POS COSECHA", RowKind.COMPLY,
                        items(
                            "ent_par",
                            "Caña dejada Max 2.5t / ha",
                            "Área productiva limpia de caña corta",
                            "Trasiego limpia de caña corta",
                            "Se levantó en tiempo la caña dejada (24 Hr después del corte).",
                            "Hidrantes en buen estado"
                        )
                    ),
                    FormSection(
                        "ent_val", "Validación y entrega", RowKind.COMPLY,
                        items(
                            "ent_val",
                            "Validó el supervisor y/o guarda finca las condiciones del lote con el supervisor de cosecha",
                            "Entregó el supervisor de cosecha mecanizada al supervisor de zona y/o Resp. de Finca"
                        )
                    )
                ),
                commentsLabel = "COMENTARIOS:",
                signatureLabels = listOf(
                    "FIRMA ENTREGA SUPERVISOR DE COSECHA",
                    "FIRMA DE RECIBIDO DE FINCA"
                )
            )
        )
    )

    val inspeccionSeguridad = StructuredForm(
        id = "INSP-SEG",
        name = "Inspección de Seguridad (genérico)",
        category = "Seguridad",
        organization = "Mi Empresa",
        documentTitle = "FORMATO DE INSPECCIÓN DE SEGURIDAD",
        revision = "Actualización 1",
        code = "INSP-SEG-001",
        footer = "Medio: Digital   Retención: 6 meses",
        pages = listOf(
            FormPage(
                id = "seg",
                title = "INSPECCIÓN DE SEGURIDAD EN ÁREA DE TRABAJO",
                headerFields = listOf(
                    HeaderField("fecha", "FECHA", isDate = true),
                    HeaderField("area", "ÁREA / UBICACIÓN"),
                    HeaderField("inspector", "INSPECTOR"),
                    HeaderField("responsable", "RESPONSABLE DEL ÁREA")
                ),
                sections = listOf(
                    FormSection(
                        "seg_epp", "Equipo de protección personal", RowKind.COMPLY,
                        items(
                            "seg_epp",
                            "El personal usa casco, chaleco y calzado de seguridad",
                            "Se usa protección auditiva y visual donde se requiere",
                            "Los EPP están en buen estado"
                        )
                    ),
                    FormSection(
                        "seg_con", "Condiciones del área", RowKind.YES_NO,
                        items(
                            "seg_con",
                            "Las salidas de emergencia están señalizadas y libres?",
                            "Los extintores están vigentes y accesibles?",
                            "El área está limpia y ordenada?"
                        )
                    )
                ),
                commentsLabel = "Hallazgos / acciones correctivas:",
                signatureLabels = listOf("FIRMA INSPECTOR", "FIRMA RESPONSABLE DEL ÁREA")
            )
        )
    )

    val all: List<StructuredForm> = listOf(recepcionEntregaLotes, inspeccionSeguridad)

    fun byId(id: String): StructuredForm? = all.firstOrNull { it.id == id }
}
