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

data class FormItem(
    val id: String,
    val label: String,
    val unit: String? = null,
    val positiveAnswer: String? = null,
    val maxAllowedValue: Double? = null,
    val minAllowedValue: Double? = null
) {
    /** Returns true if numeric value complies with max/min thresholds, false if non-compliant, null if no threshold or invalid number. */
    fun checkNumericCompliance(valueText: String): Boolean? {
        if (maxAllowedValue == null && minAllowedValue == null) return null
        val clean = valueText
            .replace("%", "")
            .replace("cm", "")
            .replace("t/ha", "")
            .replace("m", "")
            .trim()
            .replace(",", ".")
        val num = clean.toDoubleOrNull() ?: return null

        if (maxAllowedValue != null && num > maxAllowedValue) return false
        if (minAllowedValue != null && num < minAllowedValue) return false
        return true
    }

    fun formatThreshold(): String {
        val u = unit ?: ""
        return when {
            maxAllowedValue != null && minAllowedValue != null -> "entre $minAllowedValue y $maxAllowedValue $u"
            maxAllowedValue != null -> "máximo $maxAllowedValue $u"
            minAllowedValue != null -> "mínimo $minAllowedValue $u"
            else -> ""
        }
    }
}

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

    // 1. Formato 2-CM-F005: Entrega y Recepción de Lote (.xlsx)
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
                        listOf(
                            FormItem("rec_car1", "La Finca lote cuenta con acceso adecuado ?"),
                            FormItem("rec_car2", "Tiene cruceros intermedios adecuados ?"),
                            FormItem("rec_car3", "Tiene bahías de descargue (trasiego de caña) ?"),
                            FormItem("rec_car4", "El lote dispone de rondas mayores a 4 metros y libres de obstáculos?")
                        )
                    ),
                    FormSection(
                        "rec_obs", "Obstáculos dentro del lote", RowKind.YES_NO,
                        listOf(
                            FormItem("rec_obs1", "El lote presenta exceso de humedad por riego?", positiveAnswer = "NO"),
                            FormItem("rec_obs2", "El lote tiene señalizado y despejado los hidrantes dentro del campo?"),
                            FormItem("rec_obs3", "El lote está libre de tubería de riego dentro del área productiva?"),
                            FormItem("rec_obs4", "El lote está libre de obstáculos (causes, troncos, tendidos eléctricos, bases de concreto, pozos, piedras, árboles, etc.)?")
                        )
                    ),
                    FormSection(
                        "rec_med", "Mediciones antes del Corte", RowKind.MEASURE,
                        listOf(
                            FormItem("rec_med1", "% De cepa expuesta antes del corte (debe ser 0%)", unit = "%", maxAllowedValue = 0.0),
                            FormItem("rec_med2", "% De espacios vacíos antes del corte mayor a 1metro", unit = "%", maxAllowedValue = 0.0),
                            FormItem("rec_med3", "% De despoblación antes del corte =< 7% (suma de espacios vacíos más cepa expuesta)", unit = "%", maxAllowedValue = 7.0),
                            FormItem("rec_med4", "Presencia de caña podrida y Seca"),
                            FormItem("rec_med5", "Altura del lomo del surco. (No mayor de 20 cm)", unit = "cm", maxAllowedValue = 20.0),
                            FormItem("rec_med6", "Surcos por debajo del nivel del corte. (No mayor de 3 cm)", unit = "cm", maxAllowedValue = 3.0),
                            FormItem("rec_med7", "Presencia de maleza en campo")
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
                        listOf(
                            FormItem("ent_par1", "Caña dejada Max 2.5t / ha", unit = "t/ha", maxAllowedValue = 2.5),
                            FormItem("ent_par2", "Área productiva limpia de caña corta"),
                            FormItem("ent_par3", "Trasiego limpia de caña corta"),
                            FormItem("ent_par4", "Se levantó en tiempo la caña dejada (24 Hr después del corte)."),
                            FormItem("ent_par5", "Hidrantes en buen estado")
                        )
                    ),
                    FormSection(
                        "ent_val", "Validación y entrega", RowKind.COMPLY,
                        listOf(
                            FormItem("ent_val1", "Validó el supervisor y/o guarda finca las condiciones del lote con el supervisor de cosecha"),
                            FormItem("ent_val2", "Entregó el supervisor de cosecha mecanizada al supervisor de zona y/o Resp. de Finca")
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

    // 2. Formato 2-CM-F004: Permiso de Enganche y Desenganche de Carretas (.xlsx)
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
                        listOf(
                            FormItem("eng_eq1", "Carretas"),
                            FormItem("eng_eq2", "Plataforma"),
                            FormItem("eng_eq3", "Volteo"),
                            FormItem("eng_eq4", "Nodriza"),
                            FormItem("eng_eq5", "Cable Remolque")
                        )
                    ),
                    FormSection(
                        "eng_epp", "Verificación de Equipo de Protección Personal (EPP)", RowKind.YES_NO,
                        listOf(
                            FormItem("eng_epp1", "Casco con barbiquejo en buen estado?"),
                            FormItem("eng_epp2", "Botas con punta metálica?"),
                            FormItem("eng_epp3", "Uniforme reglamentario para el puesto?"),
                            FormItem("eng_epp4", "Lentes de seguridad transparentes/oscuros?"),
                            FormItem("eng_epp5", "Guantes de cuero largo?")
                        )
                    ),
                    FormSection(
                        "eng_peligros", "Lista de Chequeo de Peligros Controlados", RowKind.YES_NO,
                        listOf(
                            FormItem("eng_pel1", "¿En el ambiente hay presencia de lluvia, tormenta eléctrica o vientos fuertes?", positiveAnswer = "NO"),
                            FormItem("eng_pel2", "¿Presencia de humedad o inestabilidad en el suelo?", positiveAnswer = "NO"),
                            FormItem("eng_pel3", "¿El colaborador que realiza la labor cuenta con las competencias requeridas?"),
                            FormItem("eng_pel4", "¿El ayudante IM VERIFICA que los dos seguros del tornamesa estén cerrados?"),
                            FormItem("eng_pel5", "¿En el área de trabajo cuenta con los ayudantes establecidos (2 requeridos)?"),
                            FormItem("eng_pel6", "¿El personal dispone de radio de comunicación para maniobras?"),
                            FormItem("eng_pel7", "¿Se utiliza una misma frecuencia y existe comunicación doble vía?"),
                            FormItem("eng_pel8", "¿Los equipos (tractor y cabezal) tienen alarma sonora de retroceso funcional?"),
                            FormItem("eng_pel9", "¿El Ayudante IM con radio NO es el mismo que manipula herramientas?"),
                            FormItem("eng_pel10", "¿Se cuenta con iluminación adecuada y suficiente para la labor?"),
                            FormItem("eng_pel11", "¿El lugar de enganche y desenganche está señalizado y delimitado?"),
                            FormItem("eng_pel12", "¿Están debidamente señalizados los puntos de enganche (color rojo)?"),
                            FormItem("eng_pel13", "¿Mecánicamente están funcionales los puntos de enganche?"),
                            FormItem("eng_pel14", "¿Los trabajadores tienen definida un área de seguridad donde ubicarse?"),
                            FormItem("eng_pel15", "¿El terreno donde se realizará el enganche tiene pendiente o inclinación?", positiveAnswer = "NO"),
                            FormItem("eng_pel16", "¿Hay personas ajenas a la actividad en el área de enganche?", positiveAnswer = "NO"),
                            FormItem("eng_pel17", "¿En el punto de trabajo se encuentra el tubo saca cuchillas?"),
                            FormItem("eng_pel18", "¿Fueron ubicadas las cuñas de seguridad antes de desenganchar las carretas?"),
                            FormItem("eng_pel19", "¿Se colocó tubo de enganche en dolly de carreta primaria antes del enganche?"),
                            FormItem("eng_pel20", "¿En el punto de trabajo se encuentra la cadena de alineación en buen estado?"),
                            FormItem("eng_pel21", "¿El personal cuenta con el tubo de enganche y desenganche en buen estado?"),
                            FormItem("eng_pel22", "¿El tractor de movimiento interno cuenta con compresor de aire y enganche autónomo?")
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

    // 3. Formato 2-CM-F006: Análisis de Trabajo Seguro ATS (.pdf)
    val atsCosechaMecanizada = StructuredForm(
        id = "2-CM-F006",
        name = "Análisis de Trabajo Seguro (ATS) Cosecha Mecanizada",
        category = "Seguridad",
        organization = "Pantaleon",
        documentTitle = "ANÁLISIS DE TRABAJO SEGURO (ATS)\nCOSECHA MECANIZADA",
        revision = "Actualización 2: 15/10/2025",
        code = "2-CM-F006",
        footer = "Lugar: Frente de Cosecha Mecanizada   Medio: Papel   Responsable: Supervisor de Seguridad   Retención: 1 año   Disposición: Archivo",
        pages = listOf(
            FormPage(
                id = "ats_p1",
                title = "IDENTIFICACIÓN Y EVALUACIÓN DE RIESGOS EN CAMPO",
                headerFields = listOf(
                    HeaderField("fecha", "FECHA DE EVALUACIÓN", isDate = true),
                    HeaderField("frente", "FRENTE DE CORTE"),
                    HeaderField("finca_lote", "FINCA / LOTE"),
                    HeaderField("sup_seguridad", "SUPERVISOR DE SEGURIDAD"),
                    HeaderField("equipo_operativo", "EQUIPO Y MAQUINARIA")
                ),
                sections = listOf(
                    FormSection(
                        "ats_pasos", "Evaluación de Pasos Críticos de la Tarea", RowKind.YES_NO,
                        listOf(
                            FormItem("ats_pas1", "Inspección pre-operacional de maquinaria realizada?"),
                            FormItem("ats_pas2", "Verificación de vías de acceso y pendiente del terreno?"),
                            FormItem("ats_pas3", "Distanciamiento seguro entre cosechadoras y tractores?"),
                            FormItem("ats_pas4", "Bloqueo de energías peligrosas previo a mantenimientos?")
                        )
                    ),
                    FormSection(
                        "ats_epp", "Medidas de Control y Equipo de Protección", RowKind.COMPLY,
                        listOf(
                            FormItem("ats_epp1", "Uso obligatorio de EPP completo en campo"),
                            FormItem("ats_epp2", "Delimitación y señalización de zona de maniobras"),
                            FormItem("ats_epp3", "Radios de comunicación operacional probados y activos"),
                            FormItem("ats_epp4", "Protocolo de emergencia y botiquín disponible en frente")
                        )
                    )
                ),
                commentsLabel = "Acciones Correctivas Inmediatas y Observaciones:",
                signatureLabels = listOf(
                    "FIRMA SUPERVISOR DE SEGURIDAD",
                    "FIRMA DEL OPERADOR RESPONSABLE"
                )
            )
        )
    )

    // 4. Formato 2-CM-F008: Registro de Validación de Lotes (.xlsx)
    val validacionLotes = StructuredForm(
        id = "2-CM-F008",
        name = "Registro de Validación de Lotes",
        category = "Cosecha Mecanizada",
        organization = "Pantaleon",
        documentTitle = "REGISTRO DE VALIDACIÓN DE LOTES",
        revision = "Actualización 10: 11/09/2025",
        code = "2-CM-F008",
        footer = "Lugar: Cosecha Mecanizada   Medio: Papel   Responsable: Jefe de Cosecha Mecanizada   Retención: 6 meses   Disposición: Destruir",
        pages = listOf(
            FormPage(
                id = "val_lotes",
                title = "EVALUACIÓN Y PONDERACIÓN DE CRITERIOS DE LOTE",
                headerFields = listOf(
                    HeaderField("fecha", "FECHA", isDate = true),
                    HeaderField("nombre_finca", "NOMBRE DE FINCA"),
                    HeaderField("zona_produccion", "ZONA DE PRODUCCIÓN"),
                    HeaderField("codigo_finca", "CÓDIGO DE FINCA"),
                    HeaderField("correlativo", "CORRELATIVO")
                ),
                sections = listOf(
                    FormSection(
                        "val_crit1", "Evaluación de Infraestructura y Vías", RowKind.COMPLY,
                        listOf(
                            FormItem("val_crit1_1", "Longitud de surcos conforme (> 150m)", unit = "m", minAllowedValue = 150.0),
                            FormItem("val_crit1_2", "Existencia de Bahía de Descargue (300m a 1km)", unit = "m", minAllowedValue = 300.0, maxAllowedValue = 1000.0),
                            FormItem("val_crit1_3", "Anchos de rondas libres de obstáculos (4m a 6m)", unit = "m", minAllowedValue = 4.0, maxAllowedValue = 6.0),
                            FormItem("val_crit1_4", "Circuito cañero adecuado (6m a 8m)", unit = "m", minAllowedValue = 6.0, maxAllowedValue = 8.0),
                            FormItem("val_crit1_5", "Crucero intermedio conforme a diseño (3m a 4m)", unit = "m", minAllowedValue = 3.0, maxAllowedValue = 4.0)
                        )
                    ),
                    FormSection(
                        "val_crit2", "Condiciones Agrícolas y de Suelo", RowKind.COMPLY,
                        listOf(
                            FormItem("val_crit2_1", "Rendimientos agrícolas adecuados (>= 80 t/ha)", unit = "t/ha", minAllowedValue = 80.0),
                            FormItem("val_crit2_2", "Lote libre de obstáculos (piedras, árboles, terrazas)"),
                            FormItem("val_crit2_3", "Distancia entre surcos adecuada (>= 1.50m)", unit = "m", minAllowedValue = 1.5),
                            FormItem("val_crit2_4", "Números de cortes en rango (0 a 3 cortes)", maxAllowedValue = 3.0),
                            FormItem("val_crit2_5", "Condición de humedad del terreno (Seco)"),
                            FormItem("val_crit2_6", "Nivelación del crucero con respecto al cuadro (<= 15cm)", unit = "cm", maxAllowedValue = 15.0)
                        )
                    )
                ),
                commentsLabel = "Comentarios Generales y Resultado de Validación:",
                signatureLabels = listOf(
                    "FIRMA SUPERVISOR DE COSECHA MECANIZADA",
                    "FIRMA JEFE DE COSECHA MECANIZADA"
                )
            )
        )
    )

    val all: List<StructuredForm> = listOf(
        recepcionEntregaLotes,
        engancheDesengancheCarretas,
        atsCosechaMecanizada,
        validacionLotes
    )

    fun byId(id: String): StructuredForm? = all.firstOrNull { it.id == id }
}
