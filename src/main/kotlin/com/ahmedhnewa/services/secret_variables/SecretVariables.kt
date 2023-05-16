package com.ahmedhnewa.services.secret_variables

interface SecretVariables {
    fun get(name: SecretVariableName, defaultValue: String): String
    fun get(name: SecretVariableName): String?
    fun require(name: SecretVariableName): String
}