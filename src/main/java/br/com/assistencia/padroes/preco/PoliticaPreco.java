package br.com.assistencia.padroes.preco;

import java.math.BigDecimal;

public interface PoliticaPreco {
    String nome();
    BigDecimal desconto(BigDecimal totalServicos);
}
