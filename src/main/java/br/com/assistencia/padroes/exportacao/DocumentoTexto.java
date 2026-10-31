package br.com.assistencia.padroes.exportacao;

public final class DocumentoTexto implements DocumentoOrdem {
    public String extensao() { return ".txt"; }
    public String renderizar(String ficha) { return ficha; }
}
