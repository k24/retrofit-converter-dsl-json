/*
 * Copyright (C) 2019 k24
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.k24.retrofit2.converter.dsl_json;

import com.dslplatform.json.DslJson;
import com.dslplatform.json.JsonWriter;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Converter;

import java.io.IOException;
import java.lang.reflect.Type;

final class DslJsonRequestBodyConverter<T> implements Converter<T, RequestBody> {
    private static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=UTF-8");

    private final DslJson dslJson;
    private final Type manifest;

    DslJsonRequestBodyConverter(DslJson dslJson, Type manifest) {
        this.dslJson = dslJson;
        this.manifest = manifest;
    }

    @Override
    public RequestBody convert(T value) throws IOException {
        JsonWriter writer = dslJson.newWriter();
        dslJson.serialize(writer, manifest, value);
        return RequestBody.create(MEDIA_TYPE, writer.toByteArray());
    }
}
