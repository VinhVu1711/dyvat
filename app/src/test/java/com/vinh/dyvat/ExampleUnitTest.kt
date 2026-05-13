package com.vinh.dyvat

import com.vinh.dyvat.domain.validation.ProductValidator
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
//    @Test
//    fun addition_isCorrect() {
//        assertEquals(4, 2 + 2)
//    }    
    @Test
    fun purchase_price_equal_zero(){
        val result = ProductValidator.validatePrices(
            purchasePrice = 0L,
            salePrice = 10L
        )

        assertEquals("Purchase price must greater than zero", result)
    }
    @Test
    fun sale_price_equal_zero(){
        val result = ProductValidator.validatePrices(
            purchasePrice = 10L,
            salePrice = 0L
        )
        assertEquals("Sale price must greater than zero", result)
    }
    @Test
    fun sale_price_small_than_purchase_price(){
        val result = ProductValidator.validatePrices(
            purchasePrice = 10L,
            salePrice = 9L
        )

        assertEquals("Sale price must greater than purchase price", result)
    }
    @Test
    fun sale_price_greater_than_purchase_price(){
        val result = ProductValidator.validatePrices(
            purchasePrice = 9L,
            salePrice = 10L
        )
        assertEquals("Perfect", result)
    }

}