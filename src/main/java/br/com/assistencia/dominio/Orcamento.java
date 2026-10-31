package br.com.assistencia.dominio;

import br.com.assistencia.padroes.orcamento.ComponenteOrcamento;
import br.com.assistencia.padroes.preco.PoliticaPreco;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Papel no padrão Builder: Product. Orçamento imutável após a construção. */
public final class Orcamento implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String diagnostico;
    private final LocalDate emissao;
    private final LocalDate validade;
    // List.copyOf produz uma lista serializável quando os elementos são serializáveis.
    @SuppressWarnings("serial")
    private final List<ComponenteOrcamento> itens;
    private final String politica;
    private final BigDecimal desconto;
    private final BigDecimal total;

    private Orcamento(Builder b) {
        diagnostico = Validacao.texto(b.diagnostico, "Diagnóstico");
        emissao = Validacao.obrigatorio(b.emissao, "Emissão");
        validade = Validacao.obrigatorio(b.validade, "Validade");
        if (validade.isBefore(emissao)) throw new IllegalArgumentException("Validade anterior à emissão.");
        if (b.itens.isEmpty()) throw new IllegalArgumentException("Adicione pelo menos um item ao orçamento.");
        itens = List.copyOf(b.itens);
        politica = Validacao.texto(b.politica.nome(), "Política de preço");
        BigDecimal servicos = itens.stream().map(ComponenteOrcamento::totalServicos).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal bruto = itens.stream().map(ComponenteOrcamento::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        Validacao.dinheiro(bruto, "Total bruto", false);
        desconto = Validacao.dinheiro(b.politica.desconto(servicos), "Desconto", true);
        if (desconto.compareTo(servicos) > 0) throw new IllegalArgumentException("Desconto excede a mão de obra.");
        total = Validacao.dinheiro(bruto.subtract(desconto), "Total líquido", false);
    }

    public String diagnostico() { return diagnostico; }
    public LocalDate emissao() { return emissao; }
    public LocalDate validade() { return validade; }
    public List<ComponenteOrcamento> itens() { return itens; }
    public String politica() { return politica; }
    public BigDecimal desconto() { return desconto; }
    public BigDecimal total() { return total; }
    public String detalhar() {
        StringBuilder texto = new StringBuilder("Diagnóstico: " + diagnostico + "\nEmissão: " + emissao
                + " | Validade: " + validade + "\n");
        itens.forEach(item -> texto.append(item.detalhar("  ")));
        return texto + "Política: " + politica + "\nDesconto: R$ " + desconto.toPlainString()
                + "\nTotal autorizado em caso de aprovação: R$ " + total.toPlainString() + "\n";
    }

    /** Papel no padrão Builder: ConcreteBuilder. Recebe a Strategy pelo construtor. */
    public static final class Builder {
        private final PoliticaPreco politica;
        private String diagnostico;
        private LocalDate emissao;
        private LocalDate validade;
        private final List<ComponenteOrcamento> itens = new ArrayList<>();

        public Builder(PoliticaPreco politica) {
            this.politica = Validacao.obrigatorio(politica, "Política de preço");
        }
        public Builder diagnostico(String valor) { diagnostico = valor; return this; }
        public Builder emissao(LocalDate valor) { emissao = valor; return this; }
        public Builder validade(LocalDate valor) { validade = valor; return this; }
        public Builder adicionar(ComponenteOrcamento item) {
            if (itens.size() >= 30) throw new IllegalArgumentException("Limite de 30 itens/pacotes.");
            itens.add(Validacao.obrigatorio(item, "Item"));
            return this;
        }
        public Orcamento construir() { return new Orcamento(this); }
    }
}
