package com.vinh.dyvat.domain.validation

object ProductValidator{

    fun validatePrices(
        purchasePrice: Long,
        salePrice: Long
    ) : String{
        if (purchasePrice <=0) return "Purchase price must greater than zero"
        if (salePrice <= 0 ) return "Sale price must greater than zero"
        if(salePrice < purchasePrice) return  "Sale price must greater than purchase price"
        return "Perfect"
    }

}