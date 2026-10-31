package br.com.assistencia.padroes.exportacao;

public final class ExportadorHtml extends ExportadorOrdem {
    protected DocumentoOrdem criarDocumento() { return new DocumentoHtml(); }
}
