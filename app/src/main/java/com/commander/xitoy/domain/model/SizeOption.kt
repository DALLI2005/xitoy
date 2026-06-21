package com.commander.xitoy.domain.model

import com.google.gson.annotations.SerializedName

data class SizeOption(
    @SerializedName("nomi") val nomi: String,
    @SerializedName("narx") val narx: Double
)
