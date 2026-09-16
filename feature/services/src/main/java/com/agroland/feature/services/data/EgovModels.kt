package com.agroland.feature.services.data

import com.agroland.core.network.json.JsonParser
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * EGOV сервисінің статикалық каталог жазбасы (Flutter egov_service_model.dart, 1:1).
 * `name`/`category` — ресми орысша атаулар, Flutter-дегідей локальденбейді.
 */
data class EgovService(
    val code: String,
    val name: String,
    val category: String,
)

/** EGOV статикалық каталогы — Flutter `egovServices` тізімінің толық көшірмесі. */
val egovServices: List<EgovService> = listOf(
    // Министерство Здравоохранения РК
    EgovService("APP_MedicalBook", "Личные медицинские книжки", "МЗ РК"),
    EgovService("DocumentIdentifierRegister", "Сервис по приему регистрационных данных (ID ЕПС, ID МИС)", "МЗ РК"),
    EgovService("DocumentRepository1", "Сервис регистрации/извлечении медицинских форм", "МЗ РК"),
    EgovService("DocumentStorage", "Cервис по приему данных выполненных услуг из МИС", "МЗ РК"),
    EgovService("EHD-DATA-RECEIVER", "Сервис ЕХД по приему данных", "МЗ РК"),
    EgovService("EHD-FILE-RECEIVER", "Сервис ЕХД по приему документов", "МЗ РК"),
    EgovService("EHEALTH_lABORATORY_RESEARCH", "Универсальный сервис по приему лабораторных исследований", "МЗ РК"),
    EgovService("EHEALTH_LABORATORY_RESEARCH_DELETE", "Универсальный сервис по удалению лабораторных исследований", "МЗ РК"),
    EgovService("EHEALTH_MEDICAL_EXEMPTION", "Сервис по приему данных по медотводам от МИС", "МЗ РК"),
    EgovService("EHEALTH_MEDICAL_EXEMPTION_REVOCATION", "Сервис по приему данных от МИС для отзыва медотвода", "МЗ РК"),
    EgovService("EHEALTH_UNIFIED_CLASSIFIER", "Сервис по наполнению ЕК ЛС и МИ", "МЗ РК"),
    EgovService("EHEALTH_UNIFIED_CLASSIFIER_ACTUALIZATION", "Сервис по отправке ЕК ЛС и МИ", "МЗ РК"),
    EgovService("EHEALTH_UNIFIED_CLASSIFIER_ACTUALIZATION_BY_REQUEST", "Сервис для обеспечения отправки данных из ЕК ИС ЛО", "МЗ РК"),
    EgovService("EHEALTH_UNIFIED_CLASSIFIER_DICTIONARY", "Сервис по актуализации справочников ЕК ЛС и МИ", "МЗ РК"),
    EgovService("EHEALTH_UNIFIED_CLASSIFIER_DICTIONARY_ACTUALIZATION", "Сервис по передаче и актуализации справочников Клиентов ЕК ЛС и МИ", "МЗ РК"),
    EgovService("EsmoService", "Сервис по приёму данных по медицинскому освидетельствованию", "МЗ РК"),
    EgovService("examination", "Сервис для взаимодействия по медицинской формой 031/у", "МЗ РК"),
    EgovService("RdbMISservice", "Сервис постановки на диспансерный учёт", "МЗ РК"),
    EgovService("RESERVATION_ASYNC", "Сервис для записи граждан на проведение вакцинации", "МЗ РК"),
    EgovService("RESERVATION_SYNC", "Сервис для получения списка МО, проводящих вакцинацию", "МЗ РК"),
    EgovService("RPN_ATTACHMENTS_INFO", "Сервис передачи данных о прикреплении физического лица", "МЗ РК"),
    EgovService("ScheduleGrid_vshep", "Сервис для передачи информации о графиках работ врача", "МЗ РК"),
    EgovService("shep_bg_service", "Сервис по предоставлению сведений о госпитализации/направления", "МЗ РК"),
    EgovService("VisitPatientMZ", "Сервис подтверждение участия пациента при получении медицинских услуг", "МЗ РК"),
    EgovService("VSHEP_EROB", "Сервис получения сведений по картам (ЭРОБ)", "МЗ РК"),
    EgovService("vshep_ersb_service", "Сервис по предоставлению сведений о выписке из стационара", "МЗ РК"),
    EgovService("vshep_rpn_service", "Сервис по передаче данных о ФЛ, прикреплении ФЛ ИС \"РПН\"", "МЗ РК"),
    EgovService("vshep_sur_service", "Сервис по передаче данных об организации здравоохранения", "МЗ РК"),

    // МВД РК
    EgovService("rpdrn-egov-duty-get-information", "Сервис по приему информации о платежных реквизитах", "МВД РК"),
    EgovService("rpdrn-egov-duty-register-payment", "Сервис по получению информации об оплате государственной пошлины", "МВД РК"),
    EgovService("MIA_AISSC_VEHICLE_INFO", "Сервис предоставления сведений о ТС", "МВД РК"),
    EgovService("MIA_AISSC_VEHICLE_INFO_EXTENDED", "Сервис предоставления сведений о ТС (расширенный)", "МВД РК"),
    EgovService("MIA_AISSC_LIMITS_INFO", "Сервис предоставления ограничений на ТС", "МВД РК"),
    EgovService("MIA_AISSC_SET_ARREST", "Сервис наложения ареста на ТС", "МВД РК"),
    EgovService("MIA_AISSC_REMOVE_ARREST", "Сервис снятия ареста на ТС", "МВД РК"),
    EgovService("MIA_AISSC_SET_BOOKING_SRNP_STATUS", "Сервис бронирования ГРНЗ", "МВД РК"),
    EgovService("MIA_AISSC_ISSUANCE_DUPLICATE_SRNP", "Сервис приема заявок на изготовление дубликатов ГРНЗ", "МВД РК"),
    EgovService("MIA_AISSC_FORWARD_DRIVER_LICENSE_SYNC", "Сервис приема заявок на выдачу ВУ", "МВД РК"),
    EgovService("MIA_AISAS_GetSchoolInfo", "Сервис получения данных о автошколе", "МВД РК"),

    // АФМ
    EgovService("EIASFTSync", "Сведения о наличии/отсутствии лиц в перечне организаций, связанных с финансированием терроризма", "АФМ"),
    EgovService("EIASPDLSync", "Сведения о наличии/отсутствии лиц в перечне публичных должностных лиц", "АФМ"),
    EgovService("EIASNotary", "Прием сведений по нотариальным действиям", "АФМ"),
    EgovService("EIASBSSync", "Сведения о наличии/отсутствии лиц в реестре бенефициарных собственников", "АФМ"),

    // Электронные паспорта ТС
    EgovService("epts_green", "Сервис по предоставлению сведений об электронном паспорте ТС", "ЭПТС"),

    // МСХ РК
    EgovService("GRST_TI", "Информация о сельскохозяйственной технике", "МСХ РК"),

    // МТСЗН
    EgovService("TD_OPV_difference_check", "Сервис передачи информации о расхождениях трудовых договоров и ОПВ", "МТСЗН"),
    EgovService("CheckB2L_response", "Сервис проверки статуса освобождения от социальных отчислений (ответ)", "МТСЗН"),
    EgovService("CheckB2L_request", "Сервис проверки статуса освобождения от социальных отчислений (запрос)", "МТСЗН"),
    EgovService("insurance_actualization_service", "Сервис актуализации договоров страхования", "МТСЗН"),
    EgovService("Kazpens7dList_MTSZN", "Сервис по передаче списка получателей пенсий и пособий", "МТСЗН"),
    EgovService("KSZH_OSNS_service", "Сервис по приему данных по договорам предпенсионного аннуитета", "МТСЗН"),
    EgovService("MIGRED_AKK_RT", "Сервис по приему заявлений на выдачу микрокредита", "МТСЗН"),
    EgovService("providingCatalog_MTSZN", "Сервис передачи справочной информации по трудовым договорам", "МТСЗН"),
    EgovService("UniversalDeduc_request", "Сервис проверки банкротства или восстановления платежеспособности", "МТСЗН"),
    EgovService("PersonStatusUniversalServiceSync", "Универсальный сервис по проверке социального статуса", "МТСЗН"),

    // Финансы / Налоги / ЭСФ
    EgovService("WcfFsmsMt102", "Сервис по предоставлению данных МТ-102", "Финансы"),
    EgovService("CHECK_SCP", "Сервис разовой проверки плательщика в МинТруда", "Финансы"),
    EgovService("ESF_ESS", "Получить сообщение с данными сертификатов", "Финансы"),
    EgovService("ESF_MPT01", "Сведений об обороте маркированного товара", "Финансы"),
    EgovService("ESF_EC", "Сервис по приему Электронных договоров в ИС ЭСФ", "Финансы"),
    EgovService("ESF_DICT", "Сервис по передаче справочников ИС ЭСФ", "Финансы"),
    EgovService("CITS_DEBTS", "Сервис предоставления сведений об отсутствии (наличии) задолженности", "Финансы"),
    EgovService("S_ASTANA-1_LICENCE", "Процесс предоставления разрешительных документов из ИС АСТАНА-1", "Финансы"),
    EgovService("ISNA_VIS_IP_REG_ACT_CHECK", "Проверка возможности регистрации в качестве ИП", "Финансы"),
    EgovService("STM_SNR_CHECK", "Проверка возможности по применению режима налогообложения", "Финансы"),
    EgovService("USC_IBD_REGDATA", "Передача регистрационных данных ИП", "Финансы"),
    EgovService("USC_IDB_FINANCESTABLE", "Сервис по передаче данных по показателю финансовой устойчивости", "Финансы"),
    EgovService("GbdrnIdbAktualLaw", "Сервис актуализации данных ЮЛ", "Финансы"),
    EgovService("FNO_INTEGRATION", "Сервис по приему форм налоговой отчетности", "Финансы"),
    EgovService("FNO_INTEGRATION_STATUS", "Сервис по предоставлению информации о статусе обработки формы налоговой отчетности", "Финансы"),
    EgovService("KNP_TAX_REP_CHECK_NZ_001", "Сервис сведений о приостановлении представления налоговой отчетности", "Финансы"),
    EgovService("KEDEN_FS", "Хранилище электронных документов", "Финансы"),

    // Кеден
    EgovService("Keden_Kaspi_Pay_Info", "Прием оплаты Kaspi", "Кеден"),
    EgovService("Keden_Kaspi_Status", "Запрос статуса оплаты", "Кеден"),
    EgovService("keden_express", "Подключение к ИС \"Keden\" по ПТДЭГ/ДТЭГ", "Кеден"),

    // КДП
    EgovService("GbdulAffilFace_KDP", "Сервис предоставления сведений об аффилированных лицах ЮЛ", "КДП"),
    EgovService("KDP_SERVICE", "Сервис контроля доступа к персональным данным", "КДП"),
    EgovService("KDP_SEND_SOLUTION", "Решение по заявке на отзыв согласия на доступ к персональным данным", "КДП"),
    EgovService("KDP_LIST_TO_REVOKE", "Сервис получения списка заявок на отзыв согласия на доступ к персональным данным", "КДП"),

    // ЕНСИ
    EgovService("ENSI_SeInformationGetAvailableEntities", "Сервис получения доступных справочников ЕНСИ", "ЕНСИ"),

    // Статистика
    EgovService("KLASS_ESTAT_SPRAV_new", "Сервис по предоставлению данных из классификаторов/справочников", "Статистика"),

    // Долевое участие
    EgovService("QAZREESTR_VerificationOfRegistrationOfTheEPA", "Проверка регистрации договора о долевом участии в жилищном строительстве", "Долевое участие"),

    // АИС ОИП
    EgovService("SHEP_CHSIRefs", "Сервис по передаче списка судебных исполнителей", "АИС ОИП"),
)

/**
 * /agri-machinery/{vin} сұрауының нәтижесі. Дев-ортада record пішімі
 * белгісіз (SCSE002 конверт келеді) — толығымен generic көрсетеміз.
 */
sealed interface EgovMachineryLookup {
    /** Табылды — `data` объектісінің primitive жолдары key-value ретінде. */
    data class Found(val fields: List<Pair<String, String>>) : EgovMachineryLookup

    /** Табылмады (`status:"error"` / `data:null`) — backend message-і адам тілінде. */
    data class NotFound(val message: String?) : EgovMachineryLookup
}

/** EGOV конверт парсері: `{status, code, data, message}`. */
object EgovParser {

    fun parseMachineryLookup(root: JsonElement): EgovMachineryLookup {
        val obj = root as? JsonObject
            ?: return EgovMachineryLookup.NotFound(message = null)
        val status = JsonParser.string(obj, "status")
        val message = JsonParser.string(obj, "message")
        val data = obj["data"]
        if (status == "error" || data == null || data is JsonNull) {
            return EgovMachineryLookup.NotFound(message = message)
        }
        val dataObj = data as? JsonObject
            ?: return EgovMachineryLookup.NotFound(message = message)
        val fields = dataObj.entries.mapNotNull { (key, value) ->
            when (value) {
                is JsonPrimitive -> value.contentOrNull?.takeIf { it.isNotBlank() }?.let { key to it }
                is JsonObject -> key to value.toString()
                else -> null
            }
        }
        if (fields.isEmpty()) {
            return EgovMachineryLookup.NotFound(message = message)
        }
        return EgovMachineryLookup.Found(fields)
    }
}