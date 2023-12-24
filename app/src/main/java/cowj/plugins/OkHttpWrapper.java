package cowj.plugins;

import cowj.DataSource;
import cowj.EitherMonad;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

record ResponseWrapper(byte[] bodyBytes, int status, Map<String, String> headers) {
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
     * @param url     url, e.g. <a href="http://localhost:5000/abc/def">http://localhost:5000/abc/def</a> is the url
     * @param headers to be sent
     * @param params  to be sent
     * @param body    to be sent
     * @return EitherMonad of type ResponseWrapper
     */
    default EitherMonad<ResponseWrapper> send(String verb, String url, Map<String, String> headers, Map<String, String> params, String body) {
        return send(verb, url, headers, params, body.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Sends a payload to a remote server
     *
     * @param verb    HTTP verb ( get, post, etc )
     * @param url     url, e.g. <a href="http://localhost:5000/abc/def">http://localhost:5000/abc/def</a> is the url
     * @param headers to be sent
     * @param params  to be sent
     * @param body    to be sent
     * @return EitherMonad of type ResponseWrapper
     */
    EitherMonad<ResponseWrapper> send(String verb, String url, Map<String, String> headers, Map<String, String> params, byte[] body);

    /**
     * A DataSource.Creator for the CurlWrapper
     */
    DataSource.Creator OkHTTP = (name, config, parent) -> {
        final OkHttpWrapper okHttpWrapper = new OkHttpWrapper() {
            private final OkHttpClient client = new OkHttpClient();

            @Override
            public EitherMonad<ResponseWrapper> send(String verb, String url, Map<String, String> headers, Map<String, String> params, byte[] body) {
                try {
                    RequestBody requestBody = RequestBody.create(body);
                    HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
                    params.forEach(urlBuilder::addQueryParameter);

                    Request request = new Request.Builder()
                            .url(urlBuilder.build())
                            .method(verb, requestBody)
                            .headers(Headers.of(headers))
                            .build();

                    try (Response response = client.newCall(request).execute()) {
                        ResponseBody responseBody = response.body();
                        byte[] bytes = responseBody != null ? responseBody.bytes() : new byte[]{};
                        Map<String, String> responseHeaders = response.headers().toMultimap()
                                .entrySet().stream()
                                .collect(Collectors.toMap(Map.Entry::getKey,
                                        (entry) -> String.join(",", entry.getValue())
                                ));
                        return EitherMonad.value(new ResponseWrapper(bytes, response.code(), responseHeaders));
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
