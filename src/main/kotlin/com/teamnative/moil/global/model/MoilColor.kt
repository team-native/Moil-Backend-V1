package com.teamnative.moil.global.model

enum class MoilColor(
    val id: String,
    val displayName: String,
) {
    RED("RED", "빨간색"),
    ORANGE("ORANGE", "주황색"),
    YELLOW("YELLOW", "노란색"),
    GREEN("GREEN", "초록색"),
    BLUE("BLUE", "파란색"),
    NAVY("NAVY", "남색"),
    INDIGO("INDIGO", "남색"),
    PURPLE("PURPLE", "보라색"),
    PINK("PINK", "핑크색"),

    CREAM("CREAM", "크림색"),
    PEACH("PEACH", "피치색"),
    APRICOT("APRICOT", "살구색"),
    TAN("TAN", "탄색"),
    GOLD("GOLD", "금색"),
    CORAL("CORAL", "코랄색"),
    ROSE("ROSE", "장미색"),
    SKY("SKY", "하늘색"),
    LIGHT_BLUE("LIGHT_BLUE", "연파랑색"),
    MINT("MINT", "민트색"),
    TEAL("TEAL", "청록색"),
    LIGHT_GREEN("LIGHT_GREEN", "연초록색"),
    VIVID_GREEN("VIVID_GREEN", "선명한 초록색"),
    VIOLET("VIOLET", "보라색"),
    MAGENTA("MAGENTA", "자주색"),
    VIVID_ORANGE("VIVID_ORANGE", "선명한 주황색"),
    VIVID_RED("VIVID_RED", "선명한 빨간색"),
    WARM_PINK("WARM_PINK", "웜 핑크색"),
    ;

    companion object {
        private val colorsById = entries.associateBy { it.id }

        fun exists(id: String): Boolean = id in colorsById
    }
}
