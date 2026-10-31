package br.com.assistencia.padroes.exportacao;

/**
 * @note Papel no Padrão: Concrete Product (Factory Method)
 */
public final class DocumentoHtml implements DocumentoOrdem {
    public String extensao() { return ".html"; }
    public String renderizar(String ficha) {
        String seguro = ficha.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
        return "<!doctype html><html lang=\"pt-BR\"><meta charset=\"utf-8\">"
                + "<title>Ficha da assistência técnica</title><style>body{font:16px Arial;margin:32px;}"
                + "pre{white-space:pre-wrap;overflow-wrap:anywhere;line-height:1.5}</style>"
                + "<h1>Assistência técnica</h1><pre>" + seguro + "</pre></html>";
    }
}
