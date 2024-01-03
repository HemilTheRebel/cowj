package cowj.plugins;

import cowj.DataSource;
import cowj.EitherMonad;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

record ResponseWrapper(byte[] bodyBytes, int status, Map<String, List<String>> headers) {
}

/**
 * Abstraction for a HTTP[s] web call
 */
public interface OkHttpWrapper {

    /**
     * Logger for the wrapper
     */
    Logger logger = LoggerFactory.getLogger(OkHttpWrapper.class);

    /**
     * Sends a payload to a remote server
     *
     * @param verb    HTTP verb ( get, post, etc )
     * @param url     entire url with encoded query parameters, e.g. http://localhost:5000/abc/def?a=b&c=d
     * @param headers to be sent
     * @param body    to be sent
     * @return EitherMonad of type ResponseWrapper
     */
    default EitherMonad<ResponseWrapper> send(String verb, String url, Map<String, String> headers, String body) {
        return send(verb, url, headers, body.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Sends a payload to a remote server
     *
     * @param verb    HTTP verb ( get, post, etc )
     * @param url     entire url with encoded query parameters, e.g. http://localhost:5000/abc/def?a=b&c=d
     * @param headers to be sent
     * @param body    to be sent
     * @return EitherMonad of type ResponseWrapper
     */
    EitherMonad<ResponseWrapper> send(String verb, String url, Map<String, String> headers, byte[] body);

    Set<String> bodyNotAllowed = Set.of("GET", "DELETE");

    /**
     * A DataSource.Creator for the CurlWrapper
     */
    DataSource.Creator OkHTTP = (name, config, parent) -> {
        final OkHttpWrapper okHttpWrapper = new OkHttpWrapper() {
            private final OkHttpClient client = new OkHttpClient();

            @Override
            public EitherMonad<ResponseWrapper> send(String verb, String url, Map<String, String> headers, byte[] body) {
                try {
                    RequestBody requestBody = bodyNotAllowed.contains(verb.toUpperCase()) ? null : RequestBody.create(body);

                    Request request = new Request.Builder()
                            .url(Objects.requireNonNull(HttpUrl.parse(url)))
                            .method(verb, requestBody)
                            .headers(Headers.of(headers))
                            .build();

                    try (Response response = client.newCall(request).execute()) {
                        ResponseBody responseBody = response.body();
                        byte[] bytes = responseBody != null ? responseBody.bytes() : new byte[]{};

                        return EitherMonad.value(new ResponseWrapper(bytes, response.code(), response.headers().toMultimap()));
                    }
                } catch (Throwable t) {
                    logger.error("{} : Error while Sending Request : {}", name, t.toString());
                    return EitherMonad.error(t);
                }
            }
        };
        return DataSource.dataSource(name, okHttpWrapper);
    };
}
