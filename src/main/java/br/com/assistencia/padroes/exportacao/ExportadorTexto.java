package br.com.assistencia.padroes.exportacao;

public final class ExportadorTexto extends ExportadorOrdem {
    protected DocumentoOrdem criarDocumento() { return new DocumentoTexto(); }
}
