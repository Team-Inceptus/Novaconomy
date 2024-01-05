package us.teaminceptus.novaconomy.api.business

// BusinessProduct

/**
 * Removes this product from the business.
 * @see Business.removeProduct
 */
fun BusinessProduct.remove() = business.removeProduct(this)