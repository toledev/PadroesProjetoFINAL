package br.com.assistencia.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Validacao {
    private Validacao() { }

    public static String texto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " é obrigatório.");
        }
        String normalizado = valor.strip();
        if (normalizado.length() > 500) {
            throw new IllegalArgumentException(campo + " deve ter até 500 caracteres.");
        }
        return normalizado;
    }

    public static <T> T obrigatorio(T valor, String campo) {
        if (valor == null) throw new IllegalArgumentException(campo + " é obrigatório.");
        return valor;
    }

    public static BigDecimal dinheiro(BigDecimal valor, String campo, boolean aceitaZero) {
        obrigatorio(valor, campo);
        if (valor.signum() < 0 || (!aceitaZero && valor.signum() == 0)) {
            throw new IllegalArgumentException(campo + (aceitaZero ? " não pode ser negativo." : " deve ser positivo."));
        }
        try {
            BigDecimal exato = valor.setScale(2, RoundingMode.UNNECESSARY);
            if (exato.compareTo(new BigDecimal("9999999.99")) > 0) {
                throw new IllegalArgumentException(campo + " excede o limite de R$ 9.999.999,99.");
            }
            return exato;
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(campo + " deve ter no máximo duas casas decimais.");
        }
    }
}
