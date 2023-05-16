package com.ahmedhnewa.services.secret_variables

import com.ahmedhnewa.dotenv

class DotenvSecretVariables: SecretVariables {
    override fun get(name: SecretVariableName, defaultValue: String): String {
        return dotenv[name.value] ?: defaultValue
    }

    override fun get(name: SecretVariableName): String? {
        return dotenv[name.value]
    }

    override fun require(name: SecretVariableName): String {
        return get(name) ?: throw IllegalArgumentException("'${name.value}' environment variable is required")
    }
}