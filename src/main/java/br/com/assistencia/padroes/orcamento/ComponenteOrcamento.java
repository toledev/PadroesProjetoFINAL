package br.com.assistencia.padroes.orcamento;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @note Papel no Padrão: Component (Composite)
 */
public interface ComponenteOrcamento extends Serializable {
    BigDecimal totalPecas();
    BigDecimal totalServicos();
    String detalhar(String recuo);

    default BigDecimal total() { return totalPecas().add(totalServicos()); }
}
