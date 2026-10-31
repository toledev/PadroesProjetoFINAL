package br.com.assistencia.padroes.preco;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class DescontoMaoDeObra implements PoliticaPreco {
    public String nome() { return "Campanha: 10% na mão de obra"; }
    public BigDecimal desconto(BigDecimal totalServicos) {
        return totalServicos.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
    }
}
