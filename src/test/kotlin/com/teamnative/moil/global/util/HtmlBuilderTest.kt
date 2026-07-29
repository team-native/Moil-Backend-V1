package com.teamnative.moil.global.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HtmlBuilderTest {

    @Test
    fun `buildHtml wraps output in DOCTYPE and html tag with lang attribute`() {
        val html = buildHtml {}
        assertTrue(html.startsWith("<!DOCTYPE html>"))
        assertTrue(html.contains("""<html lang="ko">"""))
        assertTrue(html.endsWith("</html>"))
    }

    @Test
    fun `nested tags render in correct order`() {
        val html = buildHtml {
            body {
                p { +"hello" }
            }
        }
        assertTrue(html.contains("<body><p>hello</p></body>"))
    }

    @Test
    fun `meta renders as self-closing tag`() {
        val html = buildHtml {
            head {
                meta { attr("charset", "UTF-8") }
            }
        }
        assertTrue(html.contains("""<meta charset="UTF-8"/>"""))
        assertFalse(html.contains("</meta>"))
    }

    @Test
    fun `attr value escapes double quotes`() {
        val html = buildHtml {
            div { attr("data-x", """say "hi"""") }
        }
        assertTrue(html.contains("""data-x="say &quot;hi&quot;""""))
    }

    @Test
    fun `unaryPlus escapes HTML special characters`() {
        val html = buildHtml {
            p { +"<script>alert('xss')</script> & more" }
        }
        assertFalse(html.contains("<script>"))
        assertTrue(html.contains("&lt;script&gt;"))
        assertTrue(html.contains("&amp;"))
        assertTrue(html.contains("&#x27;"))
    }

    @Test
    fun `multiple siblings render in order`() {
        val html = buildHtml {
            body {
                p { +"first" }
                p { +"second" }
                p { +"third" }
            }
        }
        val firstIdx = html.indexOf("first")
        val secondIdx = html.indexOf("second")
        val thirdIdx = html.indexOf("third")
        assertTrue(firstIdx < secondIdx && secondIdx < thirdIdx)
    }

    @Test
    fun `style block generates correct inline css`() {
        val html = buildHtml {
            p {
                style {
                    margin("0")
                    fontSize("16px")
                    color("#ffffff")
                }
                +"text"
            }
        }
        assertTrue(html.contains("margin:0;"))
        assertTrue(html.contains("font-size:16px;"))
        assertTrue(html.contains("color:#ffffff;"))
        assertTrue(html.contains("""style="margin:0;font-size:16px;color:#ffffff;""""))
    }

    @Test
    fun `fontFamily joins families with comma`() {
        val html = buildHtml {
            body {
                style { fontFamily("'Noto Sans'", "Arial", "sans-serif") }
            }
        }
        assertTrue(html.contains("font-family:'Noto Sans',Arial,sans-serif;"))
    }

    @Test
    fun `forEach inside tr generates correct number of td elements`() {
        val items = listOf("a", "b", "c")
        val html = buildHtml {
            table {
                tr {
                    items.forEach { item ->
                        td { +"$item" }
                    }
                }
            }
        }
        assertEquals(3, "<td>".toRegex().findAll(html).count())
    }
}
