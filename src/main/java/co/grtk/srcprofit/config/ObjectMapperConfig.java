package co.grtk.srcprofit.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class ObjectMapperConfig {

    @Bean
    public ObjectMapper getObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.registerModule(produceJavaTimeModule());
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.getFactory().enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
        objectMapper.getFactory().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        return objectMapper;
    }

    private JavaTimeModule produceJavaTimeModule() {
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        // Feign client by default deserializes OffsetDateTime and ZonedDateTime into UTC time,
        // but when we call toLocalDate() on them it can cause unexpected behaviour.
        //
        // For example APIAccountMgmtService replies with a response including 2008-07-18T00:00:00+02:00 datetime
        // and in GeorgeAccount we expect the toLocalDate() call to return 2008-07-18,
        // but Feign by default deserializes the datetime into 2008-07-17T22:00:00UTC
        // and the toLocalDate() call on it returns 2008-07-17 (the day before the expected day).
        //
        // The following deserializers always return datetimes in Europe/Budapest time zone.
        //javaTimeModule.addDeserializer(ZonedDateTime.class, new BudapestZonedDateTimeDeserializer());
        //javaTimeModule.addDeserializer(OffsetDateTime.class, new BudapestOffsetDateTimeDeserializer());
        return javaTimeModule;
    }

}


