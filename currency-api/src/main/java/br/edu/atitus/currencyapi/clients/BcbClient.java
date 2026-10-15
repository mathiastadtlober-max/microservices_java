package br.edu.atitus.currencyapi.clients;

import br.edu.atitus.currencyapi.dtos.BcbCotacaoResponse;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

// Cliente HTTP da API de cotações (PTAX) do Banco Central do Brasil
@HttpExchange("/olinda/servico/PTAX/versao/v1/odata")
public interface BcbClient {

    // moeda e dataCotacao devem ir entre aspas simples: 'USD' e 'MM-dd-yyyy'
    @GetExchange("/CotacaoMoedaDia(moeda=@moeda,dataCotacao=@dataCotacao)")
    BcbCotacaoResponse getCotacaoMoedaDia(
            @RequestParam("@moeda") String moeda,
            @RequestParam("@dataCotacao") String dataCotacao,
            @RequestParam("$format") String format
    );
}
