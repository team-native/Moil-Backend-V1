package com.teamnative.moil.global.util

@DslMarker
annotation class HtmlDsl

@HtmlDsl
internal class CssBuilder {
    private val sb = StringBuilder()

    private fun prop(name: String, value: String) { sb.append("$name:$value;") }

    fun margin(value: String) = prop("margin", value)
    fun padding(value: String) = prop("padding", value)
    fun background(value: String) = prop("background", value)
    fun fontFamily(vararg families: String) = prop("font-family", families.joinToString(","))
    fun fontSize(value: String) = prop("font-size", value)
    fun fontWeight(value: String) = prop("font-weight", value)
    fun color(value: String) = prop("color", value)
    fun letterSpacing(value: String) = prop("letter-spacing", value)
    fun width(value: String) = prop("width", value)
    fun height(value: String) = prop("height", value)
    fun borderRadius(value: String) = prop("border-radius", value)
    fun lineHeight(value: String) = prop("line-height", value)
    fun textAlign(value: String) = prop("text-align", value)
    fun minHeight(value: String) = prop("min-height", value)

    internal fun build(): String = sb.toString()
}

@HtmlDsl
internal class Tag(private val name: String, private val selfClosing: Boolean = false) {
    private val attrs = StringBuilder()
    private val content = StringBuilder()

    fun attr(key: String, value: String) { attrs.append(""" $key="${value.replace("\"", "&quot;")}"""") }
    fun style(block: CssBuilder.() -> Unit) = attr("style", CssBuilder().apply(block).build())
    operator fun String.unaryPlus() { content.append(this.htmlEscape()) }

    private fun tag(name: String, block: Tag.() -> Unit = {}) {
        content.append(Tag(name).apply(block).render())
    }

    fun meta(block: Tag.() -> Unit = {}) {
        content.append(Tag("meta", selfClosing = true).apply(block).render())
    }

    fun head(block: Tag.() -> Unit) = tag("head", block)
    fun body(block: Tag.() -> Unit) = tag("body", block)
    fun table(block: Tag.() -> Unit) = tag("table", block)
    fun tr(block: Tag.() -> Unit) = tag("tr", block)
    fun td(block: Tag.() -> Unit) = tag("td", block)
    fun div(block: Tag.() -> Unit) = tag("div", block)
    fun p(block: Tag.() -> Unit) = tag("p", block)

    internal fun render(): String = if (selfClosing) "<$name$attrs/>" else "<$name$attrs>$content</$name>"
}

internal fun buildHtml(block: Tag.() -> Unit): String =
    "<!DOCTYPE html>" + Tag("html").apply {
        attr("lang", "ko")
        block()
    }.render()

private fun String.htmlEscape(): String = replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&#x27;")
