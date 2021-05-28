package io.drogue.iot.ditto;

import static io.drogue.iot.ditto.Attributes.isNonEmptyString;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.jayway.jsonpath.JsonPath;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.v1.CloudEventBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Path("/")
public class Converter {

    private static final Logger LOG = LoggerFactory.getLogger(Converter.class);
    private static final Gson GSON = new GsonBuilder().create();

    @POST
    public Response convert(final CloudEvent event) throws IOException {

        if (event == null || event.getData() == null) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(new ErrorInformation("NoData", "No data in cloud event"))
                    .build();
        }

        var dataSchema = event.getDataSchema();
        var appIdValue = event.getExtension("application");
        var deviceIdValue = event.getExtension("device");

        LOG.debug("Converting - dataSchema: {}, appId: {}, deviceId: {}", dataSchema, appIdValue, deviceIdValue);

        if ((dataSchema == null || !isNonEmptyString(appIdValue) || !isNonEmptyString(deviceIdValue))) {
            return Response.ok(event).build();
        }

        var scheme = dataSchema.getScheme();

        LOG.debug("Scheme: {}", scheme);

        if (!scheme.equals("ditto")) {
            // must be prefixed with "ditto:"
            return Response.ok(event).build();
        }

        var appId = (String) appIdValue;
        var deviceId = (String) deviceIdValue;

        LOG.debug("CloudEvent: {}", event);

        final String data = new String(event.getData().toBytes());

        if (LOG.isDebugEnabled()) {
            LOG.debug("Data: {}", data);
        }

        var temp = JsonPath.read(data, "$.temp");

        String template = "{\"path\":\"/features\",\"topic\":\"app_id/simple-thing/things/twin/commands/modify\",\"value\":{\"temp\":{\"properties\":{\"value\":19.0}}}}";

        var ditto = JsonPath.parse(template);
        ditto.set("$.value.temp.properties.value", temp);
        ditto.set("$.topic", appId+"/"+deviceId+"/things/twin/commands/modify");

        var output = ditto.jsonString();

        var result = new CloudEventBuilder(event)
        .withData(MediaType.APPLICATION_JSON, output.getBytes(StandardCharsets.UTF_8))
        .withDataSchema(URI.create("ditto:drogue-iot"))
        .build();

        LOG.debug("Outcome: {}", output);

        return Response.ok(result).build();

    }

    /**
     * Test if a mime type reflects JSON.
     *
     * @param type The type to test.
     * @return {@code true} if the type is JSON, {@code false} otherwise.
     */
    static boolean isJson(final String type) {

        if (type == null) {
            return false;
        }

        final MimeType parsed;
        try {
            parsed = new MimeType(type);
        } catch (MimeTypeParseException e) {
            return false;
        }

        var sub = parsed.getSubType();

        if (sub.equals("json")) {
            return true;
        }

        if (sub.endsWith("+json")) {
            return true;
        }

        return false;

    }
}