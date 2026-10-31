package br.com.assistencia.dominio;

import java.io.Serializable;

/** Dados de contato do responsável pelo atendimento. */
public record Cliente(String nome, String telefone) implements Serializable {
    public Cliente {
        nome = Validacao.texto(nome, "Nome do cliente");
        telefone = Validacao.texto(telefone, "Telefone").replaceAll("[ ()+.-]", "");
        if (!telefone.matches("[0-9]{10,13}")) {
            throw new IllegalArgumentException("Informe telefone com DDD, entre 10 e 13 dígitos.");
        }
    }
}
