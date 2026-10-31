package br.com.assistencia.padroes.exportacao;

/** Papel no padrão Factory Method: ConcreteCreator de DocumentoTexto. */
public final class ExportadorTexto extends ExportadorOrdem {
    protected DocumentoOrdem criarDocumento() { return new DocumentoTexto(); }
}
