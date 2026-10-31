package br.com.assistencia.padroes.exportacao;

/**
 * @note Papel no Padrão: Concrete Creator (Factory Method)
 */
public final class ExportadorTexto extends ExportadorOrdem {
    protected DocumentoOrdem criarDocumento() { return new DocumentoTexto(); }
}
