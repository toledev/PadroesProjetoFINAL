package br.com.assistencia.padroes.preco;

import java.math.BigDecimal;

/**
 * @note Papel no Padrão: Strategy (Strategy)
 */
public interface PoliticaPreco {
    String nome();
    BigDecimal desconto(BigDecimal totalServicos);
}
