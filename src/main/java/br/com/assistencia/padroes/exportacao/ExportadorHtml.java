package br.com.assistencia.padroes.exportacao;

/**
 * @note Papel no Padrão: Concrete Creator (Factory Method)
 */
public final class ExportadorHtml extends ExportadorOrdem {
    protected DocumentoOrdem criarDocumento() { return new DocumentoHtml(); }
}
