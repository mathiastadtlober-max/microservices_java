package br.edu.atitus.currencyapi.configs;

import br.edu.atitus.currencyapi.clients.BcbClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.time.Duration;

@Configuration
public class BcbClientConfig {

    @Bean
    BcbClient getBcbClient(
            RestClient.Builder builder,
            @Value("${app.bcb.base-url:https://olinda.bcb.gov.br}") String baseUrl) {
        // Timeout Pattern: 2s para conectar e 5s para ler a resposta do BCB
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        RestClient restClient = builder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();

        RestClientAdapter adapter = RestClientAdapter.create(restClient);

        HttpServiceProxyFactory factory =
                HttpServiceProxyFactory.builderFor(adapter).build();

        return factory.createClient(BcbClient.class);
    }
}
