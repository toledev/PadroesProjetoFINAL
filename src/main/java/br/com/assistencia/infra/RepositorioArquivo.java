package br.com.assistencia.infra;

import br.com.assistencia.dominio.OrdemServico;
import br.com.assistencia.servicos.RepositorioOrdens;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Arquivo local versionado. Uma instância por diretório, com trava entre processos.
 * Cada operação carrega objetos novos; só o arquivo substituído com sucesso é confirmado.
 */
public final class RepositorioArquivo implements RepositorioOrdens, AutoCloseable {
    private final Path arquivo;
    private final FileChannel canal;
    private final FileLock trava;

    public RepositorioArquivo(Path diretorio) throws IOException {
        Files.createDirectories(diretorio);
        arquivo = diretorio.resolve("ordens.bin");
        canal = FileChannel.open(diretorio.resolve("assistencia.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        FileLock obtida;
        try {
            obtida = canal.tryLock();
        } catch (OverlappingFileLockException e) {
            canal.close();
            throw new IOException("Este diretório de dados já está aberto em outra instância.", e);
        } catch (IOException e) {
            canal.close();
            throw e;
        }
        if (obtida == null) {
            canal.close();
            throw new IOException("Este diretório de dados já está aberto em outra instância.");
        }
        trava = obtida;
    }

    public List<OrdemServico> carregar() throws IOException {
        if (!Files.exists(arquivo)) return new ArrayList<>();
        try (ObjectInputStream entrada = new ObjectInputStream(Files.newInputStream(arquivo))) {
            entrada.setObjectInputFilter(ObjectInputFilter.Config.createFilter(
                    "maxdepth=100;maxrefs=100000;maxbytes=20000000;br.com.assistencia.**;java.base/*;!*"));
            if (entrada.readInt() != 1) throw new IOException("Versão de dados incompatível.");
            Object conteudo = entrada.readObject();
            if (!(conteudo instanceof List<?> lista)) throw new IOException("Formato de dados inválido.");
            List<OrdemServico> ordens = new ArrayList<>();
            for (Object item : lista) {
                if (!(item instanceof OrdemServico ordem)) throw new IOException("Ordem inválida no arquivo.");
                ordens.add(ordem);
            }
            return ordens;
        } catch (ClassNotFoundException | RuntimeException e) {
            throw new IOException("Não foi possível ler os dados. Preserve o arquivo e verifique o backup.", e);
        }
    }

    public void salvar(List<OrdemServico> ordens) throws IOException {
        Path temporario = Files.createTempFile(arquivo.getParent(), "ordens-", ".tmp");
        try {
            try (FileOutputStream arquivoSaida = new FileOutputStream(temporario.toFile());
                 ObjectOutputStream saida = new ObjectOutputStream(arquivoSaida)) {
                saida.writeInt(1);
                saida.writeObject(new ArrayList<>(ordens));
                saida.flush();
                arquivoSaida.getFD().sync();
            }
            if (Files.exists(arquivo)) {
                Files.copy(arquivo, arquivo.resolveSibling("ordens.bak"), StandardCopyOption.REPLACE_EXISTING);
            }
            // Não faz fallback para gravação não atômica: falhar preserva a versão anterior.
            Files.move(temporario, arquivo, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(temporario);
        }
    }

    public void close() throws IOException {
        try { trava.release(); } finally { canal.close(); }
    }
}
