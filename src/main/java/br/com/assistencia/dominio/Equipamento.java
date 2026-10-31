package br.com.assistencia.dominio;

import java.io.Serializable;
import java.util.Locale;

/** O identificador deve ser o número de série ou uma etiqueta física da loja. */
public record Equipamento(String descricao, String identificador, String condicaoFisica,
                          String acessorios) implements Serializable {
    public Equipamento {
        descricao = Validacao.texto(descricao, "Descrição do equipamento");
        identificador = Validacao.texto(identificador, "Série/etiqueta").toUpperCase(Locale.ROOT);
        condicaoFisica = Validacao.texto(condicaoFisica, "Condição física");
        acessorios = Validacao.texto(acessorios, "Acessórios (ou nenhum)");
    }
}
