package com.teamnative.moil.global.model

enum class MoilColor(
    val id: String,
    val displayName: String,
    val red: Int,
    val green: Int,
    val blue: Int,
) {
    RED("RED", "빨간색", 244, 67, 54),
    ORANGE("ORANGE", "주황색", 255, 152, 0),
    YELLOW("YELLOW", "노란색", 255, 235, 59),
    GREEN("GREEN", "초록색", 76, 175, 80),
    BLUE("BLUE", "파란색", 33, 150, 243),
    NAVY("NAVY", "남색", 25, 55, 109),
    INDIGO("INDIGO", "남색", 63, 81, 181),
    PURPLE("PURPLE", "보라색", 156, 39, 176),
    PINK("PINK", "핑크색", 233, 30, 99),
    CREAM("CREAM", "크림색", 255, 248, 225),
    PEACH("PEACH", "피치색", 255, 204, 188),
    APRICOT("APRICOT", "살구색", 255, 183, 77),
    TAN("TAN", "탄색", 188, 143, 107),
    GOLD("GOLD", "금색", 255, 193, 7),
    CORAL("CORAL", "코랄색", 255, 111, 97),
    ROSE("ROSE", "장미색", 233, 90, 119),
    SKY("SKY", "하늘색", 3, 169, 244),
    LIGHT_BLUE("LIGHT_BLUE", "연파랑색", 129, 212, 250),
    MINT("MINT", "민트색", 128, 203, 196),
    TEAL("TEAL", "청록색", 0, 150, 136),
    LIGHT_GREEN("LIGHT_GREEN", "연초록색", 139, 195, 74),
    VIVID_GREEN("VIVID_GREEN", "선명한 초록색", 0, 200, 83),
    VIOLET("VIOLET", "보라색", 103, 58, 183),
    MAGENTA("MAGENTA", "자주색", 216, 27, 96),
    VIVID_ORANGE("VIVID_ORANGE", "선명한 주황색", 255, 87, 34),
    VIVID_RED("VIVID_RED", "선명한 빨간색", 213, 0, 0),
    WARM_PINK("WARM_PINK", "웜 핑크색", 255, 64, 129),
    ;

    companion object {
        private val colorsById = entries.associateBy { it.id }

        fun exists(id: String): Boolean = id in colorsById

        fun nearest(red: Int, green: Int, blue: Int): MoilColor = entries.minBy { color ->
            val redDistance = red - color.red
            val greenDistance = green - color.green
            val blueDistance = blue - color.blue
            redDistance * redDistance + greenDistance * greenDistance + blueDistance * blueDistance
        }
    }
}
