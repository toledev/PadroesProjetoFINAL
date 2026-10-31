package br.com.assistencia.padroes.exportacao;

/**
 * @note Papel no Padrão: Product (Factory Method)
 */
public interface DocumentoOrdem {
    String extensao();
    String renderizar(String ficha);
}
