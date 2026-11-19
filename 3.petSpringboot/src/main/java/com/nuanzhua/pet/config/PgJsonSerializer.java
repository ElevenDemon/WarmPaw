package com.nuanzhua.pet.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import org.postgresql.util.PGobject;
import org.springframework.boot.jackson.JsonComponent;
import java.io.IOException;

@JsonComponent
public class PgJsonSerializer extends JsonSerializer<PGobject> {
    /** 将PostgreSQL JSONB对象输出为JSON而非字符串。 */
    @Override public void serialize(PGobject value,JsonGenerator gen,SerializerProvider provider) throws IOException {
        if("json".equals(value.getType()) || "jsonb".equals(value.getType())) {
            if(value.getValue()==null) gen.writeNull(); else gen.writeTree(((ObjectMapper)gen.getCodec()).readTree(value.getValue()));
        } else gen.writeString(value.getValue());
    }
}
