package br.edu.atitus.currencyapi.servicesimpl;

import br.edu.atitus.currencyapi.dtos.CurrencyResponse;
import br.edu.atitus.currencyapi.repositories.CurrencyRepository;
import br.edu.atitus.currencyapi.services.CurrencyService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CurrencyServiceJpa implements CurrencyService {

    private static final Logger log = LoggerFactory.getLogger(CurrencyServiceJpa.class);

    private final CurrencyRepository repository;
    private final BcbClientService bcbClientService;

    public CurrencyServiceJpa(CurrencyRepository repository, BcbClientService bcbClientService) {
        this.repository = repository;
        this.bcbClientService = bcbClientService;
    }

    @Value("${server.port:8080}")
    private String serverPort;

    // Data fixa de um dia útil com cotação disponível no BCB (formato MM-dd-yyyy)
    @Value("${app.bcb.data-cotacao:10-01-2026}")
    private String dataCotacao;

    @Override
    public CurrencyResponse findBySourceCurrencyAndTargetCurrency(
            String sourceCurrency,
            String targetCurrency
    ) throws Exception {
        String environment = "Currency API running in Port: " + serverPort;

        // 1. Tenta obter a cotação na API do Banco Central
        try {
            Double rate = getRateFromBcb(sourceCurrency, targetCurrency);
            if (rate != null) {
                return new CurrencyResponse(
                        sourceCurrency,
                        targetCurrency,
                        rate,
                        environment + " - Source: BCB PTAX " + dataCotacao
                );
            }
            environment += " - BCB sem cotação";
        } catch (Exception e) {
            // Falha de comunicação, timeout, retries esgotados ou circuito aberto
            log.warn("Falha ao consultar o BCB ({} -> {}): {}", sourceCurrency, targetCurrency, e.getMessage());
            environment += " - BCB indisponível: " + e.getMessage();
        }

        // 2. Fallback Pattern: usa a cotação cadastrada no banco de dados local
        var currency = repository.findBySourceCurrencyAndTargetCurrency(sourceCurrency, targetCurrency)
                .orElseThrow(() -> new EntityNotFoundException("Cotação não encontrada"));
        return CurrencyResponse.fromEntity(currency, environment + " - Source: banco local (fallback)");
    }

    // O BCB informa quantos reais vale 1 unidade de cada moeda.
    // Para pares sem BRL, calcula a taxa cruzada: (source em BRL) / (target em BRL).
    private Double getRateFromBcb(String sourceCurrency, String targetCurrency) {
        if (sourceCurrency.equalsIgnoreCase(targetCurrency)) {
            return 1.0;
        }
        Double sourceEmReais = cotacaoEmReais(sourceCurrency);
        if (sourceEmReais == null) {
            return null;
        }
        Double targetEmReais = cotacaoEmReais(targetCurrency);
        if (targetEmReais == null) {
            return null;
        }
        return sourceEmReais / targetEmReais;
    }

    private Double cotacaoEmReais(String moeda) {
        if ("BRL".equalsIgnoreCase(moeda)) {
            return 1.0;
        }
        return bcbClientService.getCotacaoEmReais(moeda.toUpperCase(), dataCotacao);
    }
}
