package br.edu.atitus.currencyapi.servicesimpl;

import br.edu.atitus.currencyapi.clients.BcbClient;
import br.edu.atitus.currencyapi.dtos.BcbCotacaoResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class BcbClientService {

    private static final Logger log = LoggerFactory.getLogger(BcbClientService.class);

    private final BcbClient bcbClient;

    public BcbClientService(BcbClient bcbClient) {
        this.bcbClient = bcbClient;
    }

    /**
     * Retorna a cotação de venda (em reais) de 1 unidade da moeda na data informada,
     * ou null quando o BCB responde normalmente mas não tem cotação para moeda/data.
     *
     * Ordem dos interceptadores: Cache (99) -> Retry (100) -> Circuit Breaker (101) -> BCB.
     * "unless" garante que respostas sem cotação (null) não sejam guardadas no cache;
     * exceções (falhas de comunicação) também nunca são armazenadas.
     */
    @Cacheable(
            value = "bcb-cotacoes",
            key = "#moeda + '-' + #dataCotacao",
            unless = "#result == null"
    )
    @Retry(name = "bcb")
    @CircuitBreaker(name = "bcb")
    public Double getCotacaoEmReais(String moeda, String dataCotacao) {
        // Log para observar nos testes quando a chamada realmente chega ao BCB
        log.info("Consultando BCB: moeda={} data={}", moeda, dataCotacao);

        BcbCotacaoResponse response = bcbClient.getCotacaoMoedaDia(
                "'" + moeda + "'",
                "'" + dataCotacao + "'",
                "json"
        );

        if (response == null || response.value() == null || response.value().isEmpty()) {
            log.info("BCB sem cotação para moeda={} data={}", moeda, dataCotacao);
            return null;
        }

        // Prioriza o boletim de fechamento (PTAX); se não houver, usa o último boletim do dia
        var cotacao = response.value().stream()
                .filter(c -> "Fechamento PTAX".equalsIgnoreCase(c.tipoBoletim()))
                .findFirst()
                .orElse(response.value().getLast());

        return cotacao.cotacaoVenda();
    }
}
