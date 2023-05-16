package com.ahmedhnewa.services.secret_variables

enum class SecretVariableName(val value: String) {
    ServerPort("PORT"),
    ProductionServer("PRODUCTION_SERVER"),
    DevelopmentMode("DEVELOPMENT_MODE")
}