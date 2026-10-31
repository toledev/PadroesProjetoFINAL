package br.com.assistencia.servicos;

import br.com.assistencia.dominio.*;
import br.com.assistencia.padroes.exportacao.ExportadorOrdem;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @note Papel no Padrão: Facade (Facade)
 */
public final class AssistenciaFacade {
    private final RepositorioOrdens repositorio;
    private final Clock relogio;
    private final Map<String, ExportadorOrdem> exportadores;
    private final Path pastaExportacao;

    public AssistenciaFacade(RepositorioOrdens repositorio, Clock relogio,
                            Map<String, ExportadorOrdem> exportadores, Path pastaExportacao) {
        this.repositorio = Validacao.obrigatorio(repositorio, "Repositório");
        this.relogio = Validacao.obrigatorio(relogio, "Relógio");
        this.exportadores = Map.copyOf(Validacao.obrigatorio(exportadores, "Exportadores"));
        this.pastaExportacao = Validacao.obrigatorio(pastaExportacao, "Pasta de exportação");
    }

    public LocalDate hoje() { return LocalDate.now(relogio); }
    public List<OrdemServico> listar() throws IOException { return List.copyOf(repositorio.carregar()); }

    public OrdemServico consultar(long numero) throws IOException { return localizar(repositorio.carregar(), numero); }

    public long abrir(Cliente cliente, Equipamento equipamento, String defeito) throws IOException {
        Validacao.obrigatorio(equipamento, "Equipamento");
        List<OrdemServico> ordens = repositorio.carregar();
        boolean jaRecebido = ordens.stream().anyMatch(o -> !o.estado().encerrada()
                && o.equipamento().identificador().equals(equipamento.identificador()));
        if (jaRecebido) throw new IllegalStateException("Equipamento já possui uma ordem aberta; consulte o atendimento existente.");
        long numero = Math.addExact(ordens.stream().mapToLong(OrdemServico::numero).max().orElse(0), 1);
        ordens.add(new OrdemServico(numero, cliente, equipamento, defeito, hoje()));
        repositorio.salvar(ordens);
        return numero;
    }

    public void orcar(long numero, Orcamento orcamento) throws IOException {
        alterar(numero, ordem -> ordem.orcar(orcamento, hoje()));
    }
    public void aprovar(long numero, String responsavel) throws IOException {
        alterar(numero, ordem -> ordem.aprovar(responsavel, hoje()));
    }
    public void recusar(long numero, String responsavel, String motivo) throws IOException {
        alterar(numero, ordem -> ordem.recusar(responsavel, motivo, hoje()));
    }
    public void cancelar(long numero, String motivo) throws IOException {
        alterar(numero, ordem -> ordem.cancelar(motivo, hoje()));
    }
    public void iniciar(long numero, String tecnico, boolean disponibilidade) throws IOException {
        alterar(numero, ordem -> ordem.iniciar(tecnico, disponibilidade, hoje()));
    }
    public void concluir(long numero, String relato, String testes, boolean aprovado) throws IOException {
        alterar(numero, ordem -> ordem.concluir(relato, testes, aprovado, hoje()));
    }
    public void interromper(long numero, String tecnico, String motivo, boolean isencao) throws IOException {
        alterar(numero, ordem -> ordem.interromper(tecnico, motivo, isencao, hoje()));
    }
    public void pagar(long numero, Recibo recibo) throws IOException {
        Validacao.obrigatorio(recibo, "Recibo");
        List<OrdemServico> ordens = repositorio.carregar();
        OrdemServico ordem = localizar(ordens, numero);
        if (ordens.stream().anyMatch(o -> o.recibo() != null
                && o.recibo().referencia().equalsIgnoreCase(recibo.referencia()))) {
            throw new IllegalArgumentException("Referência de pagamento já utilizada em outra conferência.");
        }
        ordem.pagar(recibo, hoje());
        repositorio.salvar(ordens);
    }
    public void entregar(long numero, String recebedor) throws IOException {
        alterar(numero, ordem -> ordem.entregar(recebedor, hoje()));
    }
    public void devolver(long numero, String recebedor) throws IOException {
        alterar(numero, ordem -> ordem.devolver(recebedor, hoje()));
    }
    public Path exportar(long numero, String formato) throws IOException {
        ExportadorOrdem exportador = exportadores.get(formato);
        if (exportador == null) throw new IllegalArgumentException("Formato de exportação inválido.");
        return exportador.exportar(consultar(numero), pastaExportacao);
    }

    private void alterar(long numero, Consumer<OrdemServico> operacao) throws IOException {
        List<OrdemServico> ordens = repositorio.carregar();
        operacao.accept(localizar(ordens, numero));
        repositorio.salvar(ordens);
    }

    private OrdemServico localizar(List<OrdemServico> ordens, long numero) {
        return ordens.stream().filter(o -> o.numero() == numero).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Ordem " + numero + " não encontrada."));
    }
}
