package com.teamnative.moil.domain.auth.mail

import com.teamnative.moil.global.util.buildHtml

object VerificationEmailTemplate {

    fun build(code: String): String = buildHtml {
        head {
            meta { attr("charset", "UTF-8") }
        }
        body {
            style {
                margin("0")
                padding("0")
                background("#1a1a1a")
                fontFamily("'Apple SD Gothic Neo'", "Arial", "sans-serif")
            }
            table {
                attr("width", "100%")
                attr("cellpadding", "0")
                attr("cellspacing", "0")
                style {
                    background("#1a1a1a")
                    minHeight("500px")
                }
                tr {
                    td {
                        attr("align", "center")
                        style { padding("60px 20px 0") }
                        p {
                            style {
                                margin("0 0 16px")
                                fontSize("20px")
                                fontWeight("600")
                                color("#9a9a9a")
                                letterSpacing("4px")
                            }
                            +"이메일 인증"
                        }
                        p {
                            style {
                                margin("0 0 40px")
                                fontSize("16px")
                                color("#cccccc")
                                letterSpacing("1px")
                            }
                            +"이메일 인증을 위한 인증번호 6자리입니다."
                        }
                        table {
                            attr("cellpadding", "0")
                            attr("cellspacing", "0")
                            tr {
                                code.forEach { digit ->
                                    td {
                                        style { padding("0 6px") }
                                        div {
                                            style {
                                                width("64px")
                                                height("72px")
                                                background("#2a2a2a")
                                                borderRadius("12px")
                                                fontSize("28px")
                                                fontWeight("700")
                                                color("#ffffff")
                                                lineHeight("72px")
                                                textAlign("center")
                                            }
                                            +"$digit"
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                tr {
                    td {
                        attr("align", "center")
                        style { padding("80px 20px 60px") }
                        p {
                            style {
                                margin("0")
                                fontSize("13px")
                                color("#666666")
                                letterSpacing("1px")
                            }
                            +"본인이 요청하지 않았다면 이 메일을 무시하셔도 됩니다."
                        }
                    }
                }
            }
        }
    }
}
