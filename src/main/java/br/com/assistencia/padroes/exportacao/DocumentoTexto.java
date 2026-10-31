package br.com.assistencia.padroes.exportacao;

/** Papel no padrão Factory Method: ConcreteProduct, ficha em texto UTF-8. */
public final class DocumentoTexto implements DocumentoOrdem {
    public String extensao() { return ".txt"; }
    public String renderizar(String ficha) { return ficha; }
}
