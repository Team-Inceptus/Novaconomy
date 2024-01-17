package us.teaminceptus.novaconomy.api.economy

// Economy$Builder

/**
 * Builds an Economy object.
 * @return The Economy object.
 * @see [Economy.Builder.build]
 */
operator fun Economy.Builder.invoke(): Economy = build()

/**
 * Builds an Economy object.
 * @param callback The Economy object.
 * @see [Economy.Builder.build]
 */
fun Economy.Builder.build(callback: Economy.() -> Unit) {
    callback(build())
}