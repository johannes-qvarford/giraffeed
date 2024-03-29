package net.qvarford.giraffeed.infrastructure.quarkus

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.quarkus.jackson.ObjectMapperCustomizer
import jakarta.inject.Singleton

@Singleton
class GiraffeedObjectMapperCustomizer : ObjectMapperCustomizer {
    override fun customize(mapper: ObjectMapper) {
        mapper.propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
        mapper.registerKotlinModule()
    }
}