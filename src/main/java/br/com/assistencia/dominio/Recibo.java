package br.com.assistencia.dominio;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

public record Recibo(String referencia, BigDecimal valor, String forma, LocalDate data)
        implements Serializable {
    public Recibo {
        referencia = Validacao.texto(referencia, "Referência do pagamento");
        valor = Validacao.dinheiro(valor, "Valor recebido", false);
        forma = Validacao.texto(forma, "Forma de pagamento");
        if (!forma.equals("PIX") && !forma.equals("CARTAO") && !forma.equals("DINHEIRO")) {
            throw new IllegalArgumentException("Forma de pagamento inválida.");
        }
        data = Validacao.obrigatorio(data, "Data do pagamento");
    }
}
