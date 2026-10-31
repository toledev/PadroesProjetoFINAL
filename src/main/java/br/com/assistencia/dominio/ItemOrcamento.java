package br.com.assistencia.dominio;

import java.math.BigDecimal;
import br.com.assistencia.padroes.orcamento.ComponenteOrcamento;

public record ItemOrcamento(String descricao, Tipo tipo, int quantidade, BigDecimal valorUnitario)
        implements ComponenteOrcamento {
    public enum Tipo { PECA, SERVICO }

    public ItemOrcamento {
        descricao = Validacao.texto(descricao, "Descrição do item");
        tipo = Validacao.obrigatorio(tipo, "Tipo do item");
        if (quantidade < 1 || quantidade > 100) {
            throw new IllegalArgumentException("Quantidade deve estar entre 1 e 100.");
        }
        valorUnitario = Validacao.dinheiro(valorUnitario, "Valor unitário", false);
        Validacao.dinheiro(valorUnitario.multiply(BigDecimal.valueOf(quantidade)), "Subtotal", false);
    }

    public BigDecimal subtotal() {
        return valorUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public BigDecimal totalPecas() { return tipo == Tipo.PECA ? subtotal() : BigDecimal.ZERO; }
    public BigDecimal totalServicos() { return tipo == Tipo.SERVICO ? subtotal() : BigDecimal.ZERO; }
    public String detalhar(String recuo) {
        return recuo + tipo + " | " + descricao + " | " + quantidade + " x R$ "
                + valorUnitario.toPlainString() + " = R$ " + subtotal().toPlainString() + "\n";
    }
}
