package br.com.assistencia.padroes.exportacao;

/** Papel no padrão Factory Method: ConcreteCreator de DocumentoHtml. */
public final class ExportadorHtml extends ExportadorOrdem {
    protected DocumentoOrdem criarDocumento() { return new DocumentoHtml(); }
}
