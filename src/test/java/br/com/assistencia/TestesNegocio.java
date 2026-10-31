package br.com.assistencia;

import br.com.assistencia.dominio.*;
import br.com.assistencia.infra.RepositorioArquivo;
import br.com.assistencia.padroes.estado.EstadoOrdem;
import br.com.assistencia.padroes.exportacao.*;
import br.com.assistencia.padroes.orcamento.*;
import br.com.assistencia.padroes.preco.*;
import br.com.assistencia.servicos.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Testes executáveis sem bibliotecas externas, centrados nos riscos de negócio. */
public final class TestesNegocio {
    private static final LocalDate HOJE = LocalDate.of(2026, 10, 30);
    private static int verificacoes;

    public static void main(String[] args) throws Exception {
        validarObjetos();
        validarOrcamentos();
        validarFluxo();
        validarAlternativas();
        validarPersistencia();
        validarTerminal();
        System.out.println("OK - " + verificacoes + " verificacoes de negocio, persistencia e terminal.");
    }

    private static void validarObjetos() throws Exception {
        rejeita(IllegalArgumentException.class, () -> new Cliente(null, "11999999999"));
        rejeita(IllegalArgumentException.class, () -> new Cliente(" ", "11999999999"));
        rejeita(IllegalArgumentException.class, () -> new Cliente("Ana", "telefone"));
        rejeita(IllegalArgumentException.class, () -> new Cliente("Ana", "123"));
        igual("5511999999999", new Cliente("Ana", "+55 (11) 99999-9999").telefone());
        rejeita(IllegalArgumentException.class, () -> new Equipamento("PC", " ", "OK", "nenhum"));
        rejeita(IllegalArgumentException.class, () -> new Equipamento("PC", "A", null, "nenhum"));
        igual("ABC", equipamento("abc").identificador());
        rejeita(IllegalArgumentException.class, () -> item("Peça", ItemOrcamento.Tipo.PECA, 0, "10"));
        rejeita(IllegalArgumentException.class, () -> item("Peça", ItemOrcamento.Tipo.PECA, 101, "10"));
        rejeita(IllegalArgumentException.class, () -> item("Peça", ItemOrcamento.Tipo.PECA, 1, "-1"));
        rejeita(IllegalArgumentException.class, () -> item("Peça", ItemOrcamento.Tipo.PECA, 1, "0"));
        rejeita(IllegalArgumentException.class, () -> item("Peça", ItemOrcamento.Tipo.PECA, 1, "1.001"));
        rejeita(IllegalArgumentException.class, () -> item("Peça", ItemOrcamento.Tipo.PECA, 100, "9999999"));
        rejeita(IllegalArgumentException.class, () -> new ItemOrcamento("Peça", null, 1, BigDecimal.ONE));
        rejeita(IllegalArgumentException.class, () -> new Recibo(null, BigDecimal.ONE, "PIX", HOJE));
        rejeita(IllegalArgumentException.class, () -> new Recibo("R", null, "PIX", HOJE));
        rejeita(IllegalArgumentException.class, () -> new Recibo("R", new BigDecimal("-1"), "PIX", HOJE));
        rejeita(IllegalArgumentException.class, () -> new Recibo("R", BigDecimal.ZERO, "PIX", HOJE));
        rejeita(IllegalArgumentException.class, () -> new Recibo("R", BigDecimal.ONE, null, HOJE));
        rejeita(IllegalArgumentException.class, () -> new Recibo("R", BigDecimal.ONE, "CHEQUE", HOJE));
        rejeita(IllegalArgumentException.class, () -> new Recibo("R", BigDecimal.ONE, "PIX", null));
        rejeita(IllegalArgumentException.class, () -> new OrdemServico(0, cliente(), equipamento("A"), "Não liga", HOJE));
        rejeita(IllegalArgumentException.class, () -> new OrdemServico(1, null, equipamento("A"), "Não liga", HOJE));
    }

    private static void validarOrcamentos() throws Exception {
        ItemOrcamento peca = item("SSD", ItemOrcamento.Tipo.PECA, 2, "200.00");
        ItemOrcamento servico = item("Instalação", ItemOrcamento.Tipo.SERVICO, 1, "100.00");
        List<ComponenteOrcamento> mutavel = new ArrayList<>(List.of(peca, servico));
        PacoteServicos pacote = new PacoteServicos("Upgrade", mutavel);
        mutavel.clear();
        igual(new BigDecimal("500.00"), pacote.total());
        igual(new BigDecimal("400.00"), pacote.totalPecas());
        igual(new BigDecimal("100.00"), pacote.totalServicos());
        rejeita(UnsupportedOperationException.class, () -> pacote.componentes().clear());
        rejeita(IllegalArgumentException.class, () -> new PacoteServicos("Vazio", List.of()));
        rejeita(IllegalArgumentException.class, () -> new PacoteServicos("Inválido", Arrays.asList(peca, null)));
        Orcamento normal = builder(new PrecoNormal()).adicionar(pacote).construir();
        Orcamento campanha = builder(new DescontoMaoDeObra()).adicionar(pacote).construir();
        igual(new BigDecimal("500.00"), normal.total());
        igual(new BigDecimal("490.00"), campanha.total());
        igual(new BigDecimal("10.00"), campanha.desconto());
        igual(new BigDecimal("0.01"), new DescontoMaoDeObra().desconto(new BigDecimal("0.05")));
        rejeita(UnsupportedOperationException.class, () -> campanha.itens().clear());
        Orcamento.Builder reutilizavel = builder(new PrecoNormal()).adicionar(servico);
        Orcamento pronto = reutilizavel.construir();
        reutilizavel.adicionar(peca);
        igual(new BigDecimal("100.00"), pronto.total());
        rejeita(IllegalArgumentException.class, () -> builder(new PrecoNormal()).construir());
        rejeita(IllegalArgumentException.class, () -> builder(new PrecoNormal()).diagnostico(null).adicionar(peca).construir());
        rejeita(IllegalArgumentException.class, () -> builder(new PrecoNormal()).validade(HOJE.minusDays(1)).adicionar(peca).construir());
        PoliticaPreco indevida = new PoliticaPreco() {
            public String nome() { return "Inválida"; }
            public BigDecimal desconto(BigDecimal servicos) { return new BigDecimal("1000"); }
        };
        rejeita(IllegalArgumentException.class, () -> builder(indevida).adicionar(pacote).construir());
    }

    private static void validarFluxo() throws Exception {
        OrdemServico ordem = ordem();
        rejeita(IllegalStateException.class, () -> ordem.iniciar("Técnico", true, HOJE));
        rejeita(IllegalStateException.class, () -> ordem.aprovar("Ana", HOJE));
        rejeita(IllegalStateException.class, () -> ordem.entregar("Ana", HOJE));
        ordem.orcar(orcamento(), HOJE);
        rejeita(IllegalStateException.class, () -> ordem.iniciar("Técnico", true, HOJE));
        rejeita(IllegalArgumentException.class, () -> ordem.aprovar(" ", HOJE));
        igual(EstadoOrdem.AGUARDANDO_APROVACAO, ordem.estado());
        ordem.aprovar("Ana", HOJE);
        rejeita(IllegalStateException.class, () -> ordem.aprovar("Ana", HOJE));
        rejeita(IllegalStateException.class, () -> ordem.orcar(orcamento(), HOJE));
        rejeita(IllegalStateException.class, () -> ordem.cancelar("Desistência", HOJE));
        rejeita(IllegalStateException.class, () -> ordem.iniciar("Técnico", false, HOJE));
        igual(EstadoOrdem.APROVADA, ordem.estado());
        ordem.iniciar("Técnico", true, HOJE);
        rejeita(IllegalStateException.class, () -> ordem.iniciar("Técnico", true, HOJE));
        rejeita(IllegalStateException.class, () -> ordem.concluir("Troca", "Boot", false, HOJE));
        rejeita(IllegalArgumentException.class, () -> ordem.concluir(" ", "Boot", true, HOJE));
        igual(EstadoOrdem.EM_REPARO, ordem.estado());
        ordem.concluir("Troca do SSD", "Boot e teste de leitura aprovados", true, HOJE);
        rejeita(IllegalStateException.class, () -> ordem.entregar("Ana", HOJE));
        rejeita(IllegalArgumentException.class, () -> ordem.pagar(new Recibo("R", BigDecimal.ONE, "PIX", HOJE), HOJE));
        rejeita(IllegalArgumentException.class, () -> ordem.pagar(new Recibo("R", new BigDecimal("100"), "PIX", HOJE.minusDays(1)), HOJE));
        igual(null, ordem.recibo());
        ordem.pagar(new Recibo("R", new BigDecimal("100"), "PIX", HOJE), HOJE);
        rejeita(IllegalStateException.class, () -> ordem.pagar(new Recibo("R2", new BigDecimal("100"), "PIX", HOJE), HOJE));
        ordem.entregar("Ana", HOJE);
        igual(EstadoOrdem.ENTREGUE, ordem.estado());
        rejeita(IllegalStateException.class, () -> ordem.entregar("Ana", HOJE));
        rejeita(IllegalStateException.class, () -> ordem.devolver("Ana", HOJE));
        rejeita(UnsupportedOperationException.class, () -> ordem.historico().clear());
    }

    private static void validarAlternativas() throws Exception {
        OrdemServico recusada = ordem();
        recusada.orcar(orcamento(), HOJE);
        recusada.recusar("Ana", "Preço acima do disponível", HOJE);
        rejeita(IllegalStateException.class, () -> recusada.aprovar("Ana", HOJE));
        rejeita(IllegalStateException.class, () -> recusada.entregar("Ana", HOJE));
        recusada.devolver("Ana", HOJE);
        igual(EstadoOrdem.DEVOLVIDA, recusada.estado());
        igual(null, recusada.recibo());
        OrdemServico cancelada = ordem();
        cancelada.cancelar("Cliente desistiu antes do diagnóstico", HOJE);
        cancelada.devolver("Ana", HOJE);
        igual(EstadoOrdem.DEVOLVIDA, cancelada.estado());
        OrdemServico inviavel = ordem();
        inviavel.orcar(orcamento(), HOJE);
        inviavel.aprovar("Ana", HOJE);
        inviavel.iniciar("Técnico", true, HOJE);
        rejeita(IllegalStateException.class, () -> inviavel.interromper("Técnico", "Defeito sem solução", false, HOJE));
        inviavel.interromper("Técnico", "Defeito sem solução", true, HOJE);
        inviavel.devolver("Ana", HOJE);
        igual(EstadoOrdem.DEVOLVIDA, inviavel.estado());
        OrdemServico vencida = ordem();
        vencida.orcar(orcamento(), HOJE);
        rejeita(IllegalStateException.class, () -> vencida.aprovar("Ana", HOJE.plusDays(8)));
        igual(EstadoOrdem.AGUARDANDO_APROVACAO, vencida.estado());
        Orcamento revisao = new Orcamento.Builder(new PrecoNormal()).diagnostico("Revisado")
                .emissao(HOJE.plusDays(8)).validade(HOJE.plusDays(9))
                .adicionar(item("Reparo", ItemOrcamento.Tipo.SERVICO, 1, "120")).construir();
        vencida.orcar(revisao, HOJE.plusDays(8));
        vencida.aprovar("Ana", HOJE.plusDays(9));
        igual(new BigDecimal("120.00"), vencida.orcamento().total());
        rejeita(IllegalArgumentException.class, () -> vencida.iniciar("Técnico", true, HOJE));
    }

    private static void validarPersistencia() throws Exception {
        Path pasta = Files.createTempDirectory("assistencia-testes-");
        long numero;
        try (RepositorioArquivo repo = new RepositorioArquivo(pasta)) {
            rejeita(IOException.class, () -> new RepositorioArquivo(pasta));
            AssistenciaFacade app = facade(repo, pasta);
            numero = app.abrir(cliente(), equipamento("abc"), "Não liga <script>alert(1)</script>");
            igual(1L, numero);
            rejeita(IllegalStateException.class, () -> app.abrir(cliente(), equipamento("ABC"), "Outro defeito"));
            rejeita(IllegalArgumentException.class, () -> app.consultar(99));
            app.orcar(numero, orcamento());
            app.aprovar(numero, "Ana");
            rejeita(IllegalStateException.class, () -> app.aprovar(numero, "Ana"));
            igual(EstadoOrdem.APROVADA, app.consultar(numero).estado());
            app.iniciar(numero, "Técnico", true);
            app.concluir(numero, "Reparo", "Boot e estabilidade", true);
            app.pagar(numero, new Recibo("TRANS-1", new BigDecimal("100"), "PIX", HOJE));
            app.entregar(numero, "Ana");
            long outro = app.abrir(cliente(), equipamento("ABC"), "Nova visita");
            igual(2L, outro);
            app.orcar(outro, orcamento());
            app.aprovar(outro, "Ana");
            app.iniciar(outro, "Técnico", true);
            app.concluir(outro, "Reparo", "Teste funcional", true);
            rejeita(IllegalArgumentException.class, () -> app.pagar(outro,
                    new Recibo("trans-1", new BigDecimal("100"), "PIX", HOJE)));
            igual(null, app.consultar(outro).recibo());
            Path html = app.exportar(numero, "HTML");
            Path txt = app.exportar(numero, "TXT");
            String conteudo = Files.readString(html);
            verdadeiro(conteudo.contains("&lt;script&gt;"), "HTML deve escapar conteúdo digitado");
            verdadeiro(!conteudo.contains("<script>"), "HTML não pode executar texto do defeito");
            verdadeiro(Files.readString(txt).contains("ENTREGUE"), "TXT precisa refletir a ordem");
            rejeita(IllegalArgumentException.class, () -> app.exportar(numero, "PDF"));
            RepositorioOrdens falho = new RepositorioOrdens() {
                public List<OrdemServico> carregar() throws IOException { return repo.carregar(); }
                public void salvar(List<OrdemServico> ordens) throws IOException { throw new IOException("Disco indisponível"); }
            };
            rejeita(IOException.class, () -> facade(falho, pasta).pagar(outro,
                    new Recibo("TRANS-2", new BigDecimal("100"), "PIX", HOJE)));
            igual(null, app.consultar(outro).recibo());
            verdadeiro(Files.exists(pasta.resolve("ordens.bak")), "Backup anterior deve existir");
        }
        try (RepositorioArquivo reaberto = new RepositorioArquivo(pasta)) {
            AssistenciaFacade app = facade(reaberto, pasta);
            igual(2, app.listar().size());
            igual(EstadoOrdem.ENTREGUE, app.consultar(numero).estado());
            igual(new BigDecimal("100.00"), app.consultar(numero).recibo().valor());
        }
        Path corrompido = Files.createTempDirectory("assistencia-corrompido-");
        Files.writeString(corrompido.resolve("ordens.bin"), "arquivo inválido");
        try (RepositorioArquivo repo = new RepositorioArquivo(corrompido)) {
            rejeita(IOException.class, repo::carregar);
            rejeita(IOException.class, () -> facade(repo, corrompido).abrir(cliente(), equipamento("X"), "Defeito"));
            igual("arquivo inválido", Files.readString(corrompido.resolve("ordens.bin")));
        }
    }

    private static void validarTerminal() throws Exception {
        Path pasta = Files.createTempDirectory("assistencia-terminal-");
        String roteiro = String.join("\n", "abc", "2", "1", "Ana", "11999999999", "Notebook Dell", "NB-1", "Sem avarias", "Carregador", "Não liga", "s",
                "4", "1", "Fonte danificada", "7", "2", "1", "2", "Reparo completo", "2",
                "1", "Fonte", "1", "200", "2", "Instalação e testes", "1", "100", "s",
                "7", "1", "Técnico", "s", // Sem aprovação: deve rejeitar.
                "5", "1", "Ana", "s", "7", "1", "Técnico", "s",
                "8", "1", "Fonte substituída", "Boot e estabilidade", "s",
                "9", "1", "1", "TRANS-CLI", "290", "s", "10", "1", "Ana", "s",
                "13", "1", "1", "13", "1", "2", "3", "1", "0") + "\n";
        String saida = terminal(pasta, roteiro);
        for (String padrao : List.of("Builder", "Strategy", "Composite", "Factory Method", "Facade", "State")) {
            verdadeiro(saida.contains(padrao), "Terminal deve demonstrar " + padrao);
        }
        verdadeiro(saida.contains("Operação não realizada"), "Terminal deve tratar transição inválida");
        verdadeiro(saida.contains("290.00"), "Desconto da campanha deve preservar o valor das peças");
        verdadeiro(saida.contains("ENTREGUE"), "Fluxo interativo deve chegar à entrega");
        try (RepositorioArquivo repo = new RepositorioArquivo(pasta)) {
            igual(EstadoOrdem.ENTREGUE, facade(repo, pasta).consultar(1).estado());
        }
        String reinicio = terminal(pasta, "3\n1\n0\n");
        verdadeiro(reinicio.contains("ENTREGUE"), "Reinício precisa preservar o atendimento");
        String fim = terminal(pasta, "1\nPessoa incompleta\n");
        verdadeiro(fim.contains("incompleta descartada"), "EOF deve descartar formulário incompleto");
        try (RepositorioArquivo repo = new RepositorioArquivo(pasta)) { igual(1, repo.carregar().size()); }
    }

    private static String terminal(Path pasta, String entrada) throws Exception {
        Path java = Path.of(System.getProperty("java.home"), "bin", System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java");
        Path log = Files.createTempFile("assistencia-terminal-", ".log");
        Process processo = new ProcessBuilder(java.toString(), "-Dfile.encoding=UTF-8", "-cp",
                System.getProperty("java.class.path"), Main.class.getName(), pasta.toString())
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try (var saida = processo.getOutputStream()) { saida.write(entrada.getBytes(StandardCharsets.UTF_8)); }
        if (!processo.waitFor(20, TimeUnit.SECONDS)) {
            processo.destroyForcibly();
            throw new AssertionError("Terminal excedeu tempo limite. Log: " + log);
        }
        String texto = Files.readString(log);
        if (processo.exitValue() != 0) throw new AssertionError("Terminal falhou: " + texto);
        return texto;
    }

    private static AssistenciaFacade facade(RepositorioOrdens repo, Path pasta) {
        Clock clock = Clock.fixed(HOJE.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
        return new AssistenciaFacade(repo, clock, Map.of("TXT", new ExportadorTexto(), "HTML", new ExportadorHtml()), pasta.resolve("exportacoes"));
    }
    private static Cliente cliente() { return new Cliente("Ana", "11999999999"); }
    private static Equipamento equipamento(String serie) { return new Equipamento("Notebook", serie, "Sem avarias", "Carregador"); }
    private static OrdemServico ordem() { return new OrdemServico(1, cliente(), equipamento("X"), "Não liga", HOJE); }
    private static ItemOrcamento item(String nome, ItemOrcamento.Tipo tipo, int quantidade, String valor) {
        return new ItemOrcamento(nome, tipo, quantidade, new BigDecimal(valor));
    }
    private static Orcamento.Builder builder(PoliticaPreco politica) {
        return new Orcamento.Builder(politica).diagnostico("Defeito confirmado").emissao(HOJE).validade(HOJE.plusDays(7));
    }
    private static Orcamento orcamento() {
        return builder(new PrecoNormal()).adicionar(item("Reparo", ItemOrcamento.Tipo.SERVICO, 1, "100")).construir();
    }
    private static void igual(Object esperado, Object atual) {
        verificacoes++;
        if (!Objects.equals(esperado, atual)) throw new AssertionError("Esperado " + esperado + "; recebido " + atual);
    }
    private static void verdadeiro(boolean condicao, String mensagem) {
        verificacoes++;
        if (!condicao) throw new AssertionError(mensagem);
    }
    private static void rejeita(Class<? extends Throwable> tipo, Acao acao) throws Exception {
        verificacoes++;
        try { acao.executar(); }
        catch (Throwable e) {
            if (tipo.isInstance(e)) return;
            throw new AssertionError("Esperado " + tipo.getSimpleName() + "; recebido " + e, e);
        }
        throw new AssertionError("Era esperado rejeitar com " + tipo.getSimpleName());
    }
    @FunctionalInterface private interface Acao { void executar() throws Exception; }
}
