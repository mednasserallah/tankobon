package eu.kanade.tachiyomi.ui.reader.textdetection.translation

import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.MR

/**
 * The translation engine the user has selected for the text-detection sheet.
 *
 * [MLKIT] is the default: on-device, free, offline after a one-time language-pack download. [DEEPL]
 * is online and needs the user's own API key (see [DeepLTranslator]).
 */
enum class TranslationEngine(
    val titleRes: StringResource,
    /** Attribution shown adjacent to results while this engine is active. */
    val attributionRes: StringResource,
) {
    MLKIT(
        titleRes = MR.strings.translation_engine_mlkit,
        attributionRes = MR.strings.translation_powered_by_google,
    ),
    DEEPL(
        titleRes = MR.strings.translation_engine_deepl,
        attributionRes = MR.strings.translation_powered_by_deepl,
    ),
}
