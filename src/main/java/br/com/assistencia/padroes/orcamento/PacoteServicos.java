package br.com.assistencia.padroes.orcamento;

import br.com.assistencia.dominio.Validacao;
import java.math.BigDecimal;
import java.util.List;

public record PacoteServicos(String nome, List<ComponenteOrcamento> componentes)
        implements ComponenteOrcamento {
    public PacoteServicos {
        nome = Validacao.texto(nome, "Nome do pacote");
        Validacao.obrigatorio(componentes, "Itens do pacote");
        if (componentes.isEmpty() || componentes.size() > 30 || componentes.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("Pacote deve ter de 1 a 30 itens válidos.");
        }
        componentes = List.copyOf(componentes);
        Validacao.dinheiro(componentes.stream().map(ComponenteOrcamento::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add), "Total do pacote", false);
    }
    public BigDecimal totalPecas() {
        return componentes.stream().map(ComponenteOrcamento::totalPecas).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    public BigDecimal totalServicos() {
        return componentes.stream().map(ComponenteOrcamento::totalServicos).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    public String detalhar(String recuo) {
        StringBuilder texto = new StringBuilder(recuo + "Pacote: " + nome + "\n");
        componentes.forEach(item -> texto.append(item.detalhar(recuo + "  ")));
        return texto.toString();
    }
}
