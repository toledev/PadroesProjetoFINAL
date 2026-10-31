package br.com.assistencia.dominio;

import br.com.assistencia.padroes.estado.EstadoOrdem;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class OrdemServico implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long numero;
    private final Cliente cliente;
    private final Equipamento equipamento;
    private final String defeito;
    private final LocalDate abertura;
    private LocalDate ultimaMovimentacao;
    private EstadoOrdem estado = EstadoOrdem.RECEBIDA;
    private Orcamento orcamento;
    private Recibo recibo;
    private final ArrayList<String> historico = new ArrayList<>();

    public OrdemServico(long numero, Cliente cliente, Equipamento equipamento, String defeito, LocalDate abertura) {
        if (numero < 1) throw new IllegalArgumentException("Número de ordem inválido.");
        this.numero = numero;
        this.cliente = Validacao.obrigatorio(cliente, "Cliente");
        this.equipamento = Validacao.obrigatorio(equipamento, "Equipamento");
        this.defeito = Validacao.texto(defeito, "Defeito relatado");
        this.abertura = Validacao.obrigatorio(abertura, "Abertura");
        ultimaMovimentacao = abertura;
        historico.add(abertura + " | Recebida. Defeito relatado: " + this.defeito);
    }

    public void orcar(Orcamento novo, LocalDate hoje) {
        EstadoOrdem destino = estado.orcar();
        validarData(hoje);
        Validacao.obrigatorio(novo, "Orçamento");
        if (!novo.emissao().equals(hoje)) throw new IllegalArgumentException("A emissão deve ser a data atual.");
        orcamento = novo;
        registrar(destino, hoje, "Orçamento emitido/revisado. Total R$ " + novo.total().toPlainString());
    }

    public void aprovar(String responsavel, LocalDate hoje) {
        EstadoOrdem destino = estado.aprovar();
        validarData(hoje);
        String nome = Validacao.texto(responsavel, "Responsável pela aprovação");
        if (hoje.isAfter(orcamento.validade())) throw new IllegalStateException("Orçamento vencido. Emita uma revisão.");
        registrar(destino, hoje, "Orçamento aprovado por " + nome + ". Total fixado: R$ " + orcamento.total());
    }

    public void recusar(String responsavel, String motivo, LocalDate hoje) {
        EstadoOrdem destino = estado.recusar();
        validarData(hoje);
        String nome = Validacao.texto(responsavel, "Responsável pela recusa");
        String razao = Validacao.texto(motivo, "Motivo da recusa");
        registrar(destino, hoje, "Recusado por " + nome + ": " + razao);
    }

    public void cancelar(String motivo, LocalDate hoje) {
        EstadoOrdem destino = estado.cancelar();
        validarData(hoje);
        registrar(destino, hoje, "Cancelamento: " + Validacao.texto(motivo, "Motivo do cancelamento"));
    }

    public void iniciar(String tecnico, boolean recursosDisponiveis, LocalDate hoje) {
        EstadoOrdem destino = estado.iniciar();
        validarData(hoje);
        String nome = Validacao.texto(tecnico, "Técnico");
        if (!recursosDisponiveis) throw new IllegalStateException("Confirme disponibilidade das peças e recursos antes do reparo.");
        registrar(destino, hoje, "Reparo iniciado por " + nome + "; disponibilidade de peças/recursos conferida.");
    }

    public void concluir(String relato, String testes, boolean aprovadoNosTestes, LocalDate hoje) {
        EstadoOrdem destino = estado.concluir();
        validarData(hoje);
        String execucao = Validacao.texto(relato, "Relato técnico");
        String verificacoes = Validacao.texto(testes, "Testes executados");
        if (!aprovadoNosTestes) throw new IllegalStateException("Reparo permanece em andamento até passar nos testes.");
        registrar(destino, hoje, "Reparo concluído: " + execucao + ". Testes aprovados: " + verificacoes);
    }

    public void interromper(String tecnico, String motivo, boolean isencaoAutorizada, LocalDate hoje) {
        EstadoOrdem destino = estado.interromper();
        validarData(hoje);
        String nome = Validacao.texto(tecnico, "Responsável técnico");
        String razao = Validacao.texto(motivo, "Motivo da inviabilidade");
        if (!isencaoAutorizada) throw new IllegalStateException("A loja precisa assumir os custos e autorizar isenção integral.");
        registrar(destino, hoje, "Reparo inviável. Responsável: " + nome + ". Motivo: " + razao
                + ". Isenção integral autorizada pela loja; devolver sem cobrança.");
    }

    public void pagar(Recibo novo, LocalDate hoje) {
        EstadoOrdem destino = estado.pagar();
        validarData(hoje);
        Validacao.obrigatorio(novo, "Recibo");
        if (recibo != null) throw new IllegalStateException("Pagamento já registrado.");
        if (!novo.data().equals(hoje)) throw new IllegalArgumentException("Pagamento deve ser registrado na data da conferência.");
        if (novo.valor().compareTo(orcamento.total()) != 0) {
            throw new IllegalArgumentException("Recebimento deve corresponder ao total do orçamento: R$ " + orcamento.total());
        }
        recibo = novo;
        registrar(destino, hoje, "Pagamento conferido: " + novo.forma() + ", R$ " + novo.valor() + ", ref. " + novo.referencia());
    }

    public void entregar(String recebedor, LocalDate hoje) {
        EstadoOrdem destino = estado.entregar();
        validarData(hoje);
        String nome = Validacao.texto(recebedor, "Responsável pela retirada");
        if (recibo == null) throw new IllegalStateException("Registre a quitação antes da entrega.");
        registrar(destino, hoje, "Entregue após reparo para " + nome + ".");
    }

    public void devolver(String recebedor, LocalDate hoje) {
        EstadoOrdem destino = estado.devolver();
        validarData(hoje);
        registrar(destino, hoje, "Devolvido sem reparo e sem cobrança para "
                + Validacao.texto(recebedor, "Responsável pela retirada") + ".");
    }

    private void validarData(LocalDate hoje) {
        Validacao.obrigatorio(hoje, "Data");
        if (hoje.isBefore(abertura) || hoje.isBefore(ultimaMovimentacao)) {
            throw new IllegalArgumentException("Data anterior ao último evento da ordem.");
        }
    }

    private void registrar(EstadoOrdem destino, LocalDate hoje, String descricao) {
        historico.add(hoje + " | " + estado + " -> " + destino + " | " + descricao);
        estado = destino;
        ultimaMovimentacao = hoje;
    }

    public long numero() { return numero; }
    public Cliente cliente() { return cliente; }
    public Equipamento equipamento() { return equipamento; }
    public EstadoOrdem estado() { return estado; }
    public Orcamento orcamento() { return orcamento; }
    public Recibo recibo() { return recibo; }
    public List<String> historico() { return List.copyOf(historico); }

    public String resumo() {
        return "OS " + numero + " | " + estado + " | " + cliente.nome() + " | " + equipamento.descricao();
    }

    public String ficha() {
        return resumo() + "\nAbertura: " + abertura + "\nTelefone: " + cliente.telefone()
                + "\nSérie/etiqueta: " + equipamento.identificador() + "\nCondição física: " + equipamento.condicaoFisica()
                + "\nAcessórios: " + equipamento.acessorios() + "\nDefeito: " + defeito + "\n\n"
                + (orcamento == null ? "Sem orçamento.\n" : orcamento.detalhar())
                + "\nCobrança: " + situacaoCobranca()
                + "\nPagamento: " + (recibo == null ? "não registrado" : recibo.forma() + " | R$ " + recibo.valor())
                + "\n\nHistórico:\n" + String.join("\n", historico) + "\n";
    }

    private String situacaoCobranca() {
        return switch (estado) {
            case RECEBIDA, AGUARDANDO_APROVACAO -> "sem cobrança; proposta ainda não autorizada";
            case RECUSADA, CANCELADA, DEVOLVIDA -> "R$ 0,00; sem reparo e com isenção integral";
            case APROVADA, EM_REPARO -> "R$ " + orcamento.total() + " autorizados; recebimento somente após conclusão";
            case PRONTA, ENTREGUE -> recibo == null ? "pendente: R$ " + orcamento.total() : "quitada; saldo R$ 0,00";
        };
    }
}
