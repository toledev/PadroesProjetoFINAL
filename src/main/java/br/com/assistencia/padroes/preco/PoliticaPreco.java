package br.com.assistencia.padroes.preco;

import java.math.BigDecimal;

/** Papel no padrão Strategy: Strategy. Calcula desconto apenas sobre a mão de obra. */
public interface PoliticaPreco {
    String nome();
    BigDecimal desconto(BigDecimal totalServicos);
}
