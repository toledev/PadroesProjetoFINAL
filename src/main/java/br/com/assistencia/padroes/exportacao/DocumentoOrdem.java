package br.com.assistencia.padroes.exportacao;

/** Papel no padrão Factory Method: Product, documento que representa uma ficha. */
public interface DocumentoOrdem {
    String extensao();
    String renderizar(String ficha);
}
