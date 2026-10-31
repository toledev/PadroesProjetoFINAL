package br.com.assistencia.padroes.exportacao;

public interface DocumentoOrdem {
    String extensao();
    String renderizar(String ficha);
}
