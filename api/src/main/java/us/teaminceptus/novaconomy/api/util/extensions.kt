package us.teaminceptus.novaconomy.api.util

// Price

/**
 * Adds two prices together.
 * @param price The price to add.
 * @return The sum of the original price and the given price.
 */
operator fun Price.plus(price: Price): Price = Price(this.amount + price.amount)

/**
 * Adds two prices together.
 * @param price The price to add.
 * @return The sum of the original price and the given price.
 */
operator fun Price.plus(price: Double): Price = Price(this.amount + price)

/**
 * Adds two prices together.
 * @param price The price to add.
 * @return The sum of the original price and the given price.
 */
operator fun Price.plusAssign(price: Price) { add(price) }

/**
 * Adds two prices together.
 * @param price The price to add.
 * @return The sum of the original price and the given price.
 */
operator fun Price.plusAssign(price: Double) { add(price) }

/**
 * Increments the price's amount by 1.
 * @return The price with the amount incremented by 1.
 */
operator fun Price.inc() = add(1.0)

/**
 * Subtracts two prices.
 * @param price The price to subtract.
 * @return The difference between the original price and the given price.
 */
operator fun Price.minus(price: Price): Price = Price(this.amount - price.amount)

/**
 * Subtracts two prices.
 * @param price The price to subtract.
 * @return The difference between the original price and the given price.
 */
operator fun Price.minus(price: Double): Price = Price(this.amount - price)

/**
 * Subtracts two prices.
 * @param price The price to subtract.
 * @return The difference between the original price and the given price.
 */
operator fun Price.minusAssign(price: Price) { remove(price) }

/**
 * Subtracts two prices.
 * @param price The price to subtract.
 * @return The difference between the original price and the given price.
 */
operator fun Price.minusAssign(price: Double) { remove(price) }

/**
 * Decrements the price's amount by 1.
 * @return The price with the amount decremented by 1.
 */
operator fun Price.dec() = remove(1.0)

/**
 * Multiplies two prices.
 * @param price The price to multiply.
 * @return The product of the original price and the given price.
 */
operator fun Price.times(price: Price): Price = Price(this.amount * price.amount)

/**
 * Multiplies two prices.
 * @param price The price to multiply.
 * @return The product of the original price and the given price.
 */
operator fun Price.times(price: Double): Price = Price(this.amount * price)

/**
 * Multiplies two prices.
 * @param price The price to multiply.
 * @return The product of the original price and the given price.
 */
operator fun Price.timesAssign(price: Price) { times(price) }

/**
 * Multiplies two prices.
 * @param price The price to multiply.
 * @return The product of the original price and the given price.
 */
operator fun Price.timesAssign(price: Double) { times(price) }

/**
 * Divides two prices.
 * @param price The price to divide.
 * @return The quotient of the original price and the given price.
 */
operator fun Price.div(price: Price): Price = Price(this.amount / price.amount)

/**
 * Divides two prices.
 * @param price The price to divide.
 */
operator fun Price.div(price: Double): Price = Price(this.amount / price)

/**
 * Divides two prices.
 * @param price The price to divide.
 */
operator fun Price.divAssign(price: Price) { div(price) }

/**
 * Divides two prices.
 * @param price The price to divide.
 */
operator fun Price.divAssign(price: Double) { div(price) }

operator fun Price.unaryMinus() = Price(-this.amount)
operator fun Price.unaryPlus() = Price(+this.amount)
