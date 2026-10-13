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

    val engancheDesengancheCarretas = StructuredForm(
        id = "2-CM-F004",
        name = "Permiso de Enganche y Desenganche de Carretas",
        category = "Cosecha Mecanizada",
        organization = "Pantaleon",
        documentTitle = "REGISTRO DE PERMISO PARA REALIZAR ENGANCHE Y DESENGANCHE DE CARRETAS, VOLTEOS, PLATAFORMA, NODRIZA Y CABLE PARA REMOLQUE",
        revision = "Actualización 4: 11/09/2025",
        code = "2-CM-F004",
        footer = "Lugar: Proceso de Corte de Caña   Medio: Papel   Responsable: Supervisor de Turno   Retención: 6 meses   Disposición: Destruir",
        pages = listOf(
            FormPage(
                id = "eng",
                title = "PERMISO DE ENGANCHE Y DESENGANCHE DE CARRETAS Y EQUIPOS DE ARRASTRE",
                headerFields = listOf(
                    HeaderField("fecha", "FECHA DE EJECUCIÓN", isDate = true),
                    HeaderField("frente", "FRENTE DE TRABAJO"),
                    HeaderField("finca_lote", "FINCA - LOTE"),
                    HeaderField("sup_turno", "SUPERVISOR DE TURNO"),
                    HeaderField("turno", "TURNO"),
                    HeaderField("hora_inicio", "HORA INICIO"),
                    HeaderField("hora_fin", "HORA FIN")
                ),
                sections = listOf(
                    FormSection(
                        "eng_equipo", "Equipo de Arrastre Seleccionado", RowKind.YES_NO,
                        items(
                            "eng_equipo",
                            "Carretas",
                            "Plataforma",
                            "Volteo",
                            "Nodriza",
                            "Cable Remolque"
                        )
                    ),
                    FormSection(
                        "eng_epp", "Verificación de Equipo de Protección Personal (EPP)", RowKind.YES_NO,
                        items(
                            "eng_epp",
                            "Casco con barbiquejo en buen estado?",
                            "Botas con punta metálica?",
                            "Uniforme reglamentario para el puesto?",
                            "Lentes de seguridad transparentes/oscuros?",
                            "Guantes de cuero largo?"
                        )
                    ),
                    FormSection(
                        "eng_peligros", "Lista de Chequeo de Peligros Controlados", RowKind.YES_NO,
                        items(
                            "eng_peligros",
                            "¿En el ambiente hay presencia de lluvia, tormenta eléctrica o vientos fuertes?",
                            "¿Presencia de humedad o inestabilidad en el suelo?",
                            "¿El colaborador que realiza la labor cuenta con las competencias requeridas?",
                            "¿El ayudante IM VERIFICA que los dos seguros del tornamesa estén cerrados?",
                            "¿En el área de trabajo cuenta con los ayudantes establecidos (2 requeridos)?",
                            "¿El personal dispone de radio de comunicación para maniobras?",
                            "¿Se utiliza una misma frecuencia y existe comunicación doble vía?",
                            "¿Los equipos (tractor y cabezal) tienen alarma sonora de retroceso funcional?",
                            "¿El Ayudante IM con radio NO es el mismo que manipula herramientas?",
                            "¿Se cuenta con iluminación adecuada y suficiente para la labor?",
                            "¿El lugar de enganche y desenganche está señalizado y delimitado?",
                            "¿Están debidamente señalizados los puntos de enganche (color rojo)?",
                            "¿Mecánicamente están funcionales los puntos de enganche?",
                            "¿Los trabajadores tienen definida un área de seguridad donde ubicarse?",
                            "¿El terreno donde se realizará el enganche tiene pendiente o inclinación?",
                            "¿Hay personas ajenas a la actividad en el área de enganche?",
                            "¿En el punto de trabajo se encuentra el tubo saca cuchillas?",
                            "¿Fueron ubicadas las cuñas de seguridad antes de desenganchar las carretas?",
                            "¿Se colocó tubo de enganche en dolly de carreta primaria antes del enganche?",
                            "¿En el punto de trabajo se encuentra la cadena de alineación en buen estado?",
                            "¿El personal cuenta con el tubo de enganche y desenganche en buen estado?",
                            "¿El tractor de movimiento interno cuenta con compresor de aire y enganche autónomo?"
                        )
                    )
                ),
                commentsLabel = "NOTA: EL INCUMPLIMIENTO DE CUALQUIERA DE LAS MEDIDAS ES UNA FALTA GRAVE. Observaciones:",
                signatureLabels = listOf(
                    "FIRMA PERSONAL QUE REALIZA EL TRABAJO",
                    "FIRMA AUTORIZACIÓN APERTURA DE LABOR",
                    "FIRMA AUTORIZACIÓN CIERRE DE LABOR"
                )
            )
        )
    )

    val all: List<StructuredForm> = listOf(recepcionEntregaLotes, engancheDesengancheCarretas)

    fun byId(id: String): StructuredForm? = all.firstOrNull { it.id == id }
}
