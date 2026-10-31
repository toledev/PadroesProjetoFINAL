package br.com.assistencia.padroes.estado;

/**
 * @note Papel no Padrão: State (State)
 */
public enum EstadoOrdem {
    /** @note Papel no Padrão: Concrete State (State) */
    RECEBIDA {
        public EstadoOrdem orcar() { return AGUARDANDO_APROVACAO; }
        public EstadoOrdem cancelar() { return CANCELADA; }
    },
    /** @note Papel no Padrão: Concrete State (State) */
    AGUARDANDO_APROVACAO {
        public EstadoOrdem orcar() { return AGUARDANDO_APROVACAO; }
        public EstadoOrdem aprovar() { return APROVADA; }
        public EstadoOrdem recusar() { return RECUSADA; }
        public EstadoOrdem cancelar() { return CANCELADA; }
    },
    /** @note Papel no Padrão: Concrete State (State) */
    APROVADA {
        public EstadoOrdem iniciar() { return EM_REPARO; }
        public EstadoOrdem interromper() { return CANCELADA; }
    },
    /** @note Papel no Padrão: Concrete State (State) */
    EM_REPARO {
        public EstadoOrdem concluir() { return PRONTA; }
        public EstadoOrdem interromper() { return CANCELADA; }
    },
    /** @note Papel no Padrão: Concrete State (State) */
    PRONTA {
        public EstadoOrdem pagar() { return PRONTA; }
        public EstadoOrdem entregar() { return ENTREGUE; }
    },
    /** @note Papel no Padrão: Concrete State (State) */
    RECUSADA {
        public EstadoOrdem devolver() { return DEVOLVIDA; }
    },
    /** @note Papel no Padrão: Concrete State (State) */
    CANCELADA {
        public EstadoOrdem devolver() { return DEVOLVIDA; }
    },
    /** @note Papel no Padrão: Concrete State (State) */
    ENTREGUE,
    /** @note Papel no Padrão: Concrete State (State) */
    DEVOLVIDA;

    public EstadoOrdem orcar() { throw invalida("orçar/revisar"); }
    public EstadoOrdem aprovar() { throw invalida("aprovar"); }
    public EstadoOrdem recusar() { throw invalida("recusar"); }
    public EstadoOrdem cancelar() { throw invalida("cancelar antes da aprovação"); }
    public EstadoOrdem iniciar() { throw invalida("iniciar reparo"); }
    public EstadoOrdem concluir() { throw invalida("concluir reparo"); }
    public EstadoOrdem pagar() { throw invalida("registrar pagamento"); }
    public EstadoOrdem entregar() { throw invalida("entregar após reparo"); }
    public EstadoOrdem devolver() { throw invalida("devolver sem reparo"); }
    public EstadoOrdem interromper() { throw invalida("encerrar reparo inviável com isenção integral"); }

    private IllegalStateException invalida(String acao) {
        return new IllegalStateException("Não é possível " + acao + " no estado " + this + ".");
    }

    public boolean encerrada() { return this == ENTREGUE || this == DEVOLVIDA; }
}
