package br.com.assistencia.padroes.exportacao;

/**
 * @note Papel no Padrão: Concrete Product (Factory Method)
 */
public final class DocumentoTexto implements DocumentoOrdem {
    public String extensao() { return ".txt"; }
    public String renderizar(String ficha) { return ficha; }
}
