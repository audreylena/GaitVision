package com.gaitvision.logic

object Log {
    fun d(tag: String, msg: String) {
        println("DEBUG [$tag]: $msg")
    }
    fun e(tag: String, msg: String, t: Throwable? = null) {
        println("ERROR [$tag]: $msg")
        t?.printStackTrace()
    }
    fun w(tag: String, msg: String) {
        println("WARN [$tag]: $msg")
    }
    fun i(tag: String, msg: String) {
        println("INFO [$tag]: $msg")
    }
}

fun formatFloat(value: Float, decimals: Int): String {
    val asString = value.toString()
    val parts = asString.split(".")
    if (parts.size == 1) return asString
    if (parts[1].length <= decimals) return asString
    return "${parts[0]}.${parts[1].substring(0, decimals)}"
}

fun formatFloat(value: Double, decimals: Int): String {
    val asString = value.toString()
    val parts = asString.split(".")
    if (parts.size == 1) return asString
    if (parts[1].length <= decimals) return asString
    return "${parts[0]}.${parts[1].substring(0, decimals)}"
}


fun formatString(format: String, vararg args: Any?): String {
    // Simple mock for basic logging needs
    var result = format
    for (arg in args) {
        val str = if (arg is Float) {
            val s = arg.toString()
            val dot = s.indexOf(".")
            if (dot > 0 && s.length > dot + 5) s.substring(0, dot + 5) else s
        } else if (arg is Double) {
            val s = arg.toString()
            val dot = s.indexOf(".")
            if (dot > 0 && s.length > dot + 5) s.substring(0, dot + 5) else s
        } else {
            arg.toString()
        }
        result = result.replaceFirst(Regex("%.[0-9]*[a-zA-Z]"), str)
    }
    return result
}

