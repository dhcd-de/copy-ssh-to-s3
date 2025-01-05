package de.dhcd

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.enterprise.context.Dependent
import jakarta.enterprise.inject.Produces
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest

@Dependent
class InitConfig(
    private val secretsManagerClient: SecretsManagerClient,
    private val objectMapper: ObjectMapper,
) {

    @Produces
    fun configurations(): Configurations {
        val secretValue = secretsManagerClient.getSecretValue(
            GetSecretValueRequest.builder()
                .secretId("copy-ssh-to-s3--source-target-configurations")
                .build()
        )
        val configurations = objectMapper.readValue(secretValue.secretString(), object : TypeReference<List<Configuration>>() {})
        return Configurations(configurations)
    }
}