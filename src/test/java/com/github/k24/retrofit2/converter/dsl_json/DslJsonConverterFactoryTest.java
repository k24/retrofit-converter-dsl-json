package com.github.k24.retrofit2.converter.dsl_json;

import com.dslplatform.json.DslJson;
import com.dslplatform.json.JsonReader;
import com.dslplatform.json.JsonWriter;
import com.dslplatform.json.runtime.Settings;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.Body;
import retrofit2.http.POST;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by k24 on 2016/11/22.
 */
public class DslJsonConverterFactoryTest {
    // DslJson 1.9.3 cannot read interface even if public and specified deserializedAs
    interface AnInterface {
        String getName();
    }

    // DslJson 1.9.3 can read only public constructor of public class nevertheless CompiledJson
    public static class AnImplementation implements AnInterface {
        private String theName;

        AnImplementation() {
        }

        public AnImplementation(String name) {
            theName = name;
        }

        @Override
        public String getName() {
            return theName;
        }
    }

    interface Service {
        @POST("/")
        Call<AnImplementation> anImplementation(@Body AnImplementation impl);

        @POST("/")
        Call<AnInterface> anInterface(@Body AnInterface impl);
    }

    @Rule
    public final MockWebServer server = new MockWebServer();

    private Service service;

    @Before
    public void setUp() {
        DslJson<Object> dslJson = new DslJson<>(Settings.withRuntime().includeServiceLoader());
        dslJson.registerReader(AnInterface.class, (JsonReader.ReadObject<AnInterface>) jsonReader -> {
            // reader has already read '{'...
            byte[] bytes = jsonReader.toString().getBytes();
            return dslJson.newReader(bytes).next(AnImplementation.class);
        });
        dslJson.registerWriter(AnInterface.class, JsonWriter::serializeObject);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(DslJsonConverterFactory.create(dslJson))
                .build();
        service = retrofit.create(Service.class);
    }

    @Test
    public void anInterface() throws IOException, InterruptedException {
        server.enqueue(new MockResponse().setBody("{\"name\":\"value\"}"));

        Call<AnInterface> call = service.anInterface(new AnImplementation("value"));
        Response<AnInterface> response = call.execute();
        AnInterface body = response.body();
        assertThat(body.getName()).isEqualTo("value");

        RecordedRequest request = server.takeRequest();
        assertThat(request.getBody().readUtf8()).isEqualTo("{\"name\":\"value\"}");
        assertThat(request.getHeader("Content-Type")).isEqualTo("application/json; charset=UTF-8");
    }

    @Test
    public void anImplementation() throws IOException, InterruptedException {
        // DslJson 1.9.3 does not support private field, so 'theName' cannot be applied.
        server.enqueue(new MockResponse().setBody("{\"name\":\"value\"}"));

        Call<AnImplementation> call = service.anImplementation(new AnImplementation("value"));
        Response<AnImplementation> response = call.execute();
        AnImplementation body = response.body();
        assertThat(body.theName).isEqualTo("value");

        RecordedRequest request = server.takeRequest();
        assertThat(request.getBody().readUtf8()).isEqualTo("{\"name\":\"value\"}");
        assertThat(request.getHeader("Content-Type")).isEqualTo("application/json; charset=UTF-8");
    }

    @Test
    public void simple() throws IOException, InterruptedException {
        DslJson<Object> dslJson = new DslJson<>(Settings.withRuntime().includeServiceLoader());
        dslJson.registerReader(AnInterface.class, new JsonReader.ReadObject<AnInterface>() {
            @Override
            public AnInterface read(JsonReader jsonReader) throws IOException {
                // reader has already read '{'...
                byte[] bytes = jsonReader.toString().getBytes();
                return dslJson.newReader(bytes).next(AnImplementation.class);
            }
        });

        byte[] body = "{\"name\":\"value\"}".getBytes();
        AnImplementation anImplementation = dslJson.deserialize(AnImplementation.class, body, body.length);
        assertThat(anImplementation.getName()).isEqualTo("value");

        AnInterface anInterface = dslJson.deserialize(AnInterface.class, body, body.length);
        assertThat(anInterface.getName()).isEqualTo("value");
    }
}