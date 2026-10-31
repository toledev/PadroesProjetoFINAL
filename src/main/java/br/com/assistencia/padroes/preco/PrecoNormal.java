package br.com.assistencia.padroes.preco;

import java.math.BigDecimal;

public final class PrecoNormal implements PoliticaPreco {
    public String nome() { return "Preço normal"; }
    public BigDecimal desconto(BigDecimal totalServicos) { return BigDecimal.ZERO.setScale(2); }
}
