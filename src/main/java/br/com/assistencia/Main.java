package br.com.assistencia;

import br.com.assistencia.dominio.*;
import br.com.assistencia.infra.RepositorioArquivo;
import br.com.assistencia.padroes.exportacao.*;
import br.com.assistencia.padroes.orcamento.*;
import br.com.assistencia.padroes.preco.*;
import br.com.assistencia.servicos.AssistenciaFacade;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.*;

/** Ponto de composição: constrói e injeta as dependências antes de abrir o menu. */
public final class Main {
    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
        Path dados = Path.of(args.length == 0 ? "data" : args[0]).toAbsolutePath();
        Map<Integer, PoliticaPreco> politicas = Map.of(1, new PrecoNormal(), 2, new DescontoMaoDeObra());
        Map<String, ExportadorOrdem> exportadores = Map.of("TXT", new ExportadorTexto(), "HTML", new ExportadorHtml());
        try (RepositorioArquivo repositorio = new RepositorioArquivo(dados)) {
            AssistenciaFacade assistencia = new AssistenciaFacade(repositorio, Clock.systemDefaultZone(),
                    exportadores, dados.resolve("exportacoes"));
            assistencia.listar(); // Verifica o arquivo antes de aceitar novas operações.
            System.out.println("ASSISTÊNCIA TÉCNICA | Dados locais em: " + dados);
            System.out.println("Pagamento é conferência de recebimento externo. Não há operação bancária.");
            new Terminal(assistencia, politicas, new Scanner(System.in, StandardCharsets.UTF_8)).executar();
        } catch (IOException e) {
            System.err.println("Falha no armazenamento: " + e.getMessage());
            System.err.println("Dados não foram reiniciados. Verifique a pasta, permissões e o backup.");
            System.exit(1);
        }
    }

    /** Papel nos padrões Facade e Builder: Client; lê entradas e apresenta os casos de uso. */
    private static final class Terminal {
        private final AssistenciaFacade assistencia;
        private final Map<Integer, PoliticaPreco> politicas;
        private final Scanner entrada;

        Terminal(AssistenciaFacade assistencia, Map<Integer, PoliticaPreco> politicas, Scanner entrada) {
            this.assistencia = assistencia;
            this.politicas = politicas;
            this.entrada = entrada;
        }

        void executar() {
            while (true) {
                System.out.println("""

                        1 - Receber equipamento / abrir ordem
                        2 - Listar ordens
                        3 - Consultar ficha e histórico
                        4 - Diagnosticar / emitir ou revisar orçamento
                        5 - Aprovar orçamento
                        6 - Recusar orçamento
                        7 - Iniciar reparo
                        8 - Concluir reparo e registrar testes
                        9 - Conferir e registrar pagamento
                        10 - Entregar equipamento reparado
                        11 - Cancelar antes da aprovação
                        12 - Devolver equipamento sem reparo
                        13 - Exportar ficha em TXT ou HTML
                        14 - Encerrar reparo inviável com isenção integral da loja
                        0 - Sair
                        Digite :voltar em qualquer campo para cancelar a operação.
                        """);
                try {
                    int opcao = inteiro("Opção", 0, 14);
                    if (opcao == 0) { System.out.println("Atendimentos salvos. Até logo."); return; }
                    executarOpcao(opcao);
                } catch (FimEntrada e) {
                    System.out.println("\nEntrada encerrada. Operação incompleta descartada.");
                    return;
                } catch (Voltar e) {
                    System.out.println("Operação cancelada; nenhuma alteração foi salva.");
                } catch (IllegalArgumentException | IllegalStateException e) {
                    System.out.println("Operação não realizada: " + e.getMessage());
                } catch (IOException e) {
                    System.out.println("Falha de arquivo; operação não confirmada: " + e.getMessage());
                }
            }
        }

        private void executarOpcao(int opcao) throws IOException {
            switch (opcao) {
                case 1 -> abrir();
                case 2 -> {
                    List<OrdemServico> ordens = assistencia.listar();
                    if (ordens.isEmpty()) System.out.println("Nenhuma ordem cadastrada.");
                    ordens.forEach(o -> System.out.println(o.resumo()));
                }
                case 3 -> System.out.println(assistencia.consultar(numero()).ficha());
                case 4 -> orcar();
                case 5 -> {
                    long numero = numero();
                    System.out.println(assistencia.consultar(numero).ficha());
                    String responsavel = texto("Nome de quem autorizou o orçamento");
                    confirmar("Confirmar autorização do cliente para o total apresentado");
                    assistencia.aprovar(numero, responsavel);
                    salvo("Orçamento aprovado e congelado.");
                }
                case 6 -> {
                    long numero = numero();
                    String responsavel = texto("Responsável pela recusa");
                    String motivo = texto("Motivo");
                    confirmar("Confirmar recusa sem cobrança de diagnóstico");
                    assistencia.recusar(numero, responsavel, motivo);
                    salvo("Orçamento recusado; equipamento aguarda devolução.");
                }
                case 7 -> {
                    long numero = numero();
                    String tecnico = texto("Técnico responsável");
                    boolean disponiveis = sim("Peças e recursos foram conferidos e estão disponíveis");
                    assistencia.iniciar(numero, tecnico, disponiveis);
                    salvo("Reparo iniciado.");
                }
                case 8 -> {
                    long numero = numero();
                    String relato = texto("Serviço efetivamente executado");
                    String testes = texto("Testes realizados e resultados observados");
                    boolean aprovados = sim("O equipamento passou nos testes");
                    assistencia.concluir(numero, relato, testes, aprovados);
                    salvo("Reparo concluído; aguarda pagamento e retirada.");
                }
                case 9 -> pagar();
                case 10 -> {
                    long numero = numero();
                    String recebedor = texto("Nome de quem retirou (cliente ou autorizado)");
                    confirmar("Identidade e autorização de retirada conferidas presencialmente");
                    assistencia.entregar(numero, recebedor);
                    salvo("Equipamento entregue; ordem encerrada.");
                }
                case 11 -> {
                    long numero = numero();
                    String motivo = texto("Motivo do cancelamento anterior à aprovação");
                    confirmar("Cancelar sem cobrança e sem reparo");
                    assistencia.cancelar(numero, motivo);
                    salvo("Cancelada; equipamento aguarda devolução.");
                }
                case 12 -> {
                    long numero = numero();
                    String recebedor = texto("Nome de quem retirou (cliente ou autorizado)");
                    confirmar("Identidade, autorização e acessórios conferidos presencialmente");
                    assistencia.devolver(numero, recebedor);
                    salvo("Equipamento devolvido sem reparo; ordem encerrada.");
                }
                case 13 -> {
                    long numero = numero();
                    String formato = inteiro("Formato: 1 - TXT | 2 - HTML imprimível", 1, 2) == 1 ? "TXT" : "HTML";
                    Path destino = assistencia.exportar(numero, formato);
                    System.out.println("[Facade -> Factory Method] Documento " + formato + " criado em " + destino);
                }
                case 14 -> {
                    long numero = numero();
                    String tecnico = texto("Responsável técnico pela decisão");
                    String motivo = texto("Motivo da inviabilidade do reparo");
                    boolean isencao = sim("A loja autorizou isenção integral e assumirá eventuais custos de peças/serviços");
                    assistencia.interromper(numero, tecnico, motivo, isencao);
                    salvo("Reparo encerrado sem cobrança; equipamento aguarda devolução.");
                }
                default -> throw new IllegalArgumentException("Opção inválida.");
            }
        }

        private void abrir() throws IOException {
            Cliente cliente = new Cliente(texto("Nome do cliente"), texto("Telefone com DDD"));
            Equipamento equipamento = new Equipamento(texto("Tipo, marca e modelo do computador"),
                    texto("Número de série (se ausente, fixe e informe uma etiqueta única da loja)"),
                    texto("Condição física na entrada (avarias ou sem avarias)"),
                    texto("Acessórios recebidos (ou nenhum)"));
            String defeito = texto("Defeito relatado pelo cliente");
            System.out.println("Antes de qualquer acesso a arquivos, obtenha autorização específica do cliente.");
            confirmar("Conferiu os dados e o recebimento do equipamento");
            long numero = assistencia.abrir(cliente, equipamento, defeito);
            System.out.println("[Facade] Ordem " + numero + " recebida e salva.");
        }

        private void orcar() throws IOException {
            long numero = numero();
            OrdemServico ordem = assistencia.consultar(numero);
            ordem.estado().orcar(); // Falha antes de solicitar todos os campos se a etapa não permite orçamento.
            String diagnostico = texto("Diagnóstico técnico");
            int dias = inteiro("Validade em dias a partir de hoje (1 a 90)", 1, 90);
            int politica = inteiro("Preço: 1 - Normal | 2 - Campanha 10% somente na mão de obra", 1, 2);
            Orcamento.Builder builder = new Orcamento.Builder(politicas.get(politica))
                    .diagnostico(diagnostico).emissao(assistencia.hoje()).validade(assistencia.hoje().plusDays(dias));
            int quantidade = inteiro("Quantos itens avulsos/pacotes deseja adicionar (1 a 30)", 1, 30);
            for (int i = 0; i < quantidade; i++) {
                int tipo = inteiro("Adicionar: 1 - Item avulso | 2 - Pacote com peças/serviços", 1, 2);
                if (tipo == 1) {
                    builder.adicionar(item());
                } else {
                    String nome = texto("Nome do pacote (ex.: manutenção preventiva)");
                    int total = inteiro("Quantidade de componentes do pacote (1 a 30)", 1, 30);
                    List<ComponenteOrcamento> componentes = new ArrayList<>();
                    for (int j = 0; j < total; j++) componentes.add(item());
                    builder.adicionar(new PacoteServicos(nome, componentes));
                    System.out.println("[Composite] Pacote formado; seus componentes são somados uma única vez.");
                }
            }
            Orcamento orcamento = builder.construir();
            System.out.println("[Builder + Strategy] Orçamento construído com " + orcamento.politica());
            System.out.println(orcamento.detalhar());
            confirmar("Salvar orçamento (ainda não autoriza o reparo)");
            assistencia.orcar(numero, orcamento);
            salvo("Orçamento salvo; aguarda decisão do cliente.");
        }

        private ItemOrcamento item() {
            ItemOrcamento.Tipo tipo = inteiro("Tipo: 1 - Peça | 2 - Serviço", 1, 2) == 1
                    ? ItemOrcamento.Tipo.PECA : ItemOrcamento.Tipo.SERVICO;
            String descricao = texto("Descrição do item");
            int quantidade = inteiro("Quantidade (1 a 100)", 1, 100);
            return new ItemOrcamento(descricao, tipo, quantidade, dinheiro("Valor unitário (ex.: 150,00)"));
        }

        private void pagar() throws IOException {
            long numero = numero();
            OrdemServico ordem = assistencia.consultar(numero);
            ordem.estado().pagar();
            System.out.println("Total a conferir: R$ " + ordem.orcamento().total());
            String[] formas = {"PIX", "CARTAO", "DINHEIRO"};
            String forma = formas[inteiro("Recebimento: 1 - Pix | 2 - Cartão | 3 - Dinheiro", 1, 3) - 1];
            String referencia = texto("Referência única do recebimento (transação ou controle interno de caixa)");
            BigDecimal valor = dinheiro("Valor líquido recebido (dinheiro: descontar eventual troco)");
            confirmar("O recebimento foi efetivamente verificado fora do sistema");
            assistencia.pagar(numero, new Recibo(referencia, valor, forma, assistencia.hoje()));
            salvo("Quitação registrada. Não foi efetuada cobrança bancária pelo programa.");
        }

        private void salvo(String mensagem) { System.out.println("[Facade + State] " + mensagem + " Alteração salva."); }
        private long numero() { return inteiro("Número da ordem", 1, Integer.MAX_VALUE); }

        private String texto(String campo) {
            System.out.print(campo + ": ");
            if (!entrada.hasNextLine()) throw new FimEntrada();
            String valor = entrada.nextLine().strip();
            if (valor.equalsIgnoreCase(":voltar")) throw new Voltar();
            return Validacao.texto(valor, campo);
        }
        private int inteiro(String campo, int minimo, int maximo) {
            while (true) {
                try {
                    int valor = Integer.parseInt(texto(campo));
                    if (valor >= minimo && valor <= maximo) return valor;
                } catch (NumberFormatException e) { /* A mensagem abaixo orienta uma nova tentativa. */ }
                System.out.println("Informe um inteiro entre " + minimo + " e " + maximo + ".");
            }
        }
        private BigDecimal dinheiro(String campo) {
            String valor = texto(campo);
            if (!valor.matches("[0-9]+([.,][0-9]{1,2})?")) {
                throw new IllegalArgumentException("Informe valor sem separador de milhar, com até duas casas decimais.");
            }
            return Validacao.dinheiro(new BigDecimal(valor.replace(',', '.')), campo, false);
        }
        private boolean sim(String campo) {
            while (true) {
                String valor = texto(campo + " (s/n)");
                if (valor.equalsIgnoreCase("s")) return true;
                if (valor.equalsIgnoreCase("n")) return false;
                System.out.println("Responda s ou n.");
            }
        }
        private void confirmar(String campo) { if (!sim(campo)) throw new Voltar(); }
    }

    private static final class Voltar extends RuntimeException { private static final long serialVersionUID = 1L; }
    private static final class FimEntrada extends RuntimeException { private static final long serialVersionUID = 1L; }
}
