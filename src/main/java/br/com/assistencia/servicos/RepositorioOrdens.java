package br.com.assistencia.servicos;

import br.com.assistencia.dominio.OrdemServico;
import java.io.IOException;
import java.util.List;

public interface RepositorioOrdens {
    List<OrdemServico> carregar() throws IOException;
    void salvar(List<OrdemServico> ordens) throws IOException;
}
