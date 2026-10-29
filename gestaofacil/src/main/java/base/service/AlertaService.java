package base.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import base.modelo.AlertaDTO;
import base.modelo.DocumentoMotorista;
import base.modelo.DocumentoVeiculo;
import base.modelo.LancamentoVeiculo;
import base.modelo.Lembrete;
import base.modelo.Usuario;
import base.modelo.Veiculo;
import dao.GenericDAO;

/**
 * Central de alertas unificada (RF34): agrega documentos de veículo/motorista
 * a vencer (RF18), lembretes avulsos vencidos/próximos (RF32) e manutenção
 * programada por km (kmProximaRevisao do último Serviço de cada veículo).
 * Não persiste nada — só lê e monta a lista combinada.
 */
public class AlertaService implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final int MARGEM_DIAS = 30;
	private static final long MARGEM_KM = 500;

	@Inject
	private GenericDAO<DocumentoVeiculo> daoDocumentoVeiculo;

	@Inject
	private GenericDAO<DocumentoMotorista> daoDocumentoMotorista;

	@Inject
	private GenericDAO<Lembrete> daoLembrete;

	@Inject
	private GenericDAO<LancamentoVeiculo> daoLancamento;

	public List<AlertaDTO> listarAlertas() {
		List<AlertaDTO> alertas = new ArrayList<>();
		Date hoje = new Date();
		Date limite = somarDias(hoje, MARGEM_DIAS);

		for (DocumentoVeiculo d : daoDocumentoVeiculo.listaComStatus(DocumentoVeiculo.class)) {
			if (d.getDataVencimento() != null && d.getDataVencimento().before(limite)) {
				String nomeTipo = d.getTipoDocumento() != null ? d.getTipoDocumento().getNome() : "Documento";
				AlertaDTO alerta = montar("Documento Veículo", nomeTipo, d.getVeiculo(), null, d.getDataVencimento(),
						null, null, hoje);
				alerta.setDocumentoVeiculoId(d.getId());
				alertas.add(alerta);
			}
		}

		for (DocumentoMotorista d : daoDocumentoMotorista.listaComStatus(DocumentoMotorista.class)) {
			if (d.getDataVencimento() != null && d.getDataVencimento().before(limite)) {
				String nomeTipo = d.getTipoDocumento() != null ? d.getTipoDocumento().getNome() : "Documento";
				AlertaDTO alerta = montar("Documento Motorista", nomeTipo, null, d.getMotorista(),
						d.getDataVencimento(), null, null, hoje);
				alerta.setDocumentoMotoristaId(d.getId());
				alertas.add(alerta);
			}
		}

		for (Lembrete l : daoLembrete.listar(Lembrete.class, "concluido is false")) {
			if ("Data".equals(l.getAlvoTipo()) && l.getDataAlvo() != null && l.getDataAlvo().before(limite)) {
				AlertaDTO alerta = montar("Lembrete", l.getDescricao(), l.getVeiculo(), l.getMotorista(),
						l.getDataAlvo(), null, null, hoje);
				alerta.setLembreteId(l.getId());
				alertas.add(alerta);
			} else if ("Km".equals(l.getAlvoTipo()) && l.getKmAlvo() != null && l.getVeiculo() != null
					&& l.getVeiculo().getKmAtual() != null
					&& l.getVeiculo().getKmAtual() >= l.getKmAlvo() - MARGEM_KM) {
				AlertaDTO alerta = montar("Lembrete", l.getDescricao(), l.getVeiculo(), l.getMotorista(), null,
						l.getKmAlvo(), l.getVeiculo().getKmAtual(), hoje);
				alerta.setLembreteId(l.getId());
				alertas.add(alerta);
			}
		}

		// só a manutenção programada mais recente de cada veículo — não empilha
		// alerta de serviços antigos já superados por um mais novo
		Set<Long> veiculosConsiderados = new HashSet<>();
		List<LancamentoVeiculo> servicos = daoLancamento.listar(LancamentoVeiculo.class,
				"tipo='Serviço' and kmProximaRevisao is not null order by data desc, id desc");
		for (LancamentoVeiculo servico : servicos) {
			Veiculo veiculo = servico.getVeiculo();
			if (veiculo == null || veiculosConsiderados.contains(veiculo.getId())) {
				continue;
			}
			veiculosConsiderados.add(veiculo.getId());

			if (veiculo.getKmAtual() != null && servico.getKmProximaRevisao() != null
					&& veiculo.getKmAtual() >= servico.getKmProximaRevisao() - MARGEM_KM) {
				AlertaDTO alerta = montar("Manutenção Programada", "Próxima revisão", veiculo, null, null,
						servico.getKmProximaRevisao(), veiculo.getKmAtual(), hoje);
				alerta.setVeiculoId(veiculo.getId());
				alerta.setLancamentoVeiculoId(servico.getId());
				alertas.add(alerta);
			}
		}

		// vencidos primeiro, depois próximos
		alertas.sort(Comparator.comparing(a -> "Vencido".equals(a.getUrgencia()) ? 0 : 1));

		return alertas;
	}

	private AlertaDTO montar(String tipo, String descricao, Veiculo veiculo, Usuario motorista, Date dataVencimento,
			Long kmAlvo, Long kmAtual, Date hoje) {
		AlertaDTO alerta = new AlertaDTO();
		alerta.setTipo(tipo);
		alerta.setDescricao(descricao);
		alerta.setVeiculo(veiculo != null ? veiculo.getDescricao() + " (" + veiculo.getPlaca() + ")" : null);
		alerta.setMotorista(motorista != null ? motorista.getNome() : null);
		alerta.setDataVencimento(dataVencimento);
		alerta.setKmAlvo(kmAlvo);
		alerta.setKmAtual(kmAtual);

		if (dataVencimento != null) {
			alerta.setUrgencia(dataVencimento.before(hoje) ? "Vencido" : "Próximo");
		} else if (kmAlvo != null && kmAtual != null) {
			alerta.setUrgencia(kmAtual >= kmAlvo ? "Vencido" : "Próximo");
		}

		return alerta;
	}

	private Date somarDias(Date data, int dias) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(data);
		calendar.add(Calendar.DAY_OF_MONTH, dias);
		return calendar.getTime();
	}

}
