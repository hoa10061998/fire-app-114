package com.huster.myapplication.models

data class UserLocationInfo(
    var lastUploadTime: Long = 0,
    var imagesHistory: List<ImageInfo>
)
