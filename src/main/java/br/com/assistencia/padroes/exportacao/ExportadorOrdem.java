package br.com.assistencia.padroes.exportacao;

import br.com.assistencia.dominio.OrdemServico;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.UUID;

/**
 * @note Papel no Padrão: Creator (Factory Method)
 */
public abstract class ExportadorOrdem {
    /**
     * @note Papel no Padrão: Factory Method
     */
    protected abstract DocumentoOrdem criarDocumento();

    public final Path exportar(OrdemServico ordem, Path diretorio) throws IOException {
        DocumentoOrdem documento = criarDocumento();
        Files.createDirectories(diretorio);
        Path destino = diretorio.resolve("os-" + ordem.numero() + "-" + UUID.randomUUID() + documento.extensao());
        Files.writeString(destino, documento.renderizar(ordem.ficha()), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        return destino;
    }
}
