package org.reekwest.http.lens

import org.reekwest.http.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.reekwest.http.core.Status.Companion.NOT_ACCEPTABLE
import org.reekwest.http.core.toUrlEncoded
import java.net.URLDecoder.decode

typealias FormFields = Map<String, List<String>>

object FormField : BiDiLensSpec<WebForm, String, String>("form field",
    Get { name, (fields) -> fields.getOrDefault(name, listOf()) },
    Set { name, values, target -> values.fold(target, { m, next -> m.plus(name to next) }) }
)

data class WebForm constructor(val fields: Map<String, List<String>>, val errors: List<Failure>) {
    operator fun plus(kv: Pair<String, String>): WebForm =
        copy(fields.plus(kv.first to fields.getOrDefault(kv.first, emptyList()).plus(kv.second)))

    fun with(vararg modifiers: (WebForm) -> WebForm): WebForm = modifiers.fold(this, { memo, next -> next(memo) })

    companion object {
        fun emptyForm() = WebForm(emptyMap(), emptyList())
    }
}

enum class FormValidator : (WebForm) -> WebForm {
    Strict {
        override fun invoke(form: WebForm): WebForm = if (form.errors.isEmpty()) form else throw LensFailure(form.errors, NOT_ACCEPTABLE)
    },
    Feedback {
        override fun invoke(form: WebForm): WebForm = form
    };
}

fun Body.webForm(validator: FormValidator, vararg formFields: Lens<WebForm, *>) =
    Body.string(APPLICATION_FORM_URLENCODED).map(
        { target -> WebForm(formParametersFrom(target), emptyList()) },
        { (fields) -> fields.flatMap { pair -> pair.value.map { pair.key to it } }.toUrlEncoded() }
    ).map(
        { webForm -> validateFields(webForm, validator, *formFields) },
        { webForm -> validateFields(webForm, validator, *formFields) }
    ).required()

private fun validateFields(webForm: WebForm, validator: FormValidator, vararg formFields: Lens<WebForm, *>): WebForm {
    val failures = formFields.fold(listOf<Failure>()) {
        memo, next ->
        try {
            next(webForm)
            memo
        } catch (e: LensFailure) {
            memo.plus(e.failures)
        }
    }
    return validator(webForm.copy(errors = failures))
}

private fun formParametersFrom(target: String): Map<String, List<String>> {
    return target
        .split("&")
        .filter { it.contains("=") }
        .map { it.split("=") }
        .map { decode(it[0], "UTF-8") to if (it.size > 1) decode(it[1], "UTF-8") else "" }
        .groupBy { it.first }
        .mapValues { it.value.map { it.second } }
}
