package com.ahmedhnewa.services.secret_variables

object SecretVariablesService: SecretVariables {
    private val service: SecretVariables = DotenvSecretVariables()

    fun javaSystemEnvironment(): SecretVariables = JavaDotenvSecretVariables()
    fun dotenv(): SecretVariables = DotenvSecretVariables()

    override fun get(name: SecretVariableName, defaultValue: String): String = service.get(name, defaultValue)

    override fun get(name: SecretVariableName): String? = service.get(name)

    override fun require(name: SecretVariableName): String = service.require(name)
}