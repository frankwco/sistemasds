package base.controle;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import base.modelo.AlertaDTO;
import base.modelo.RelatorioCategoriaDTO;
import base.modelo.RelatorioCustoVeiculoDTO;
import base.modelo.RelatorioTendenciaMensalDTO;
import base.modelo.Veiculo;
import base.service.AlertaService;
import base.service.RelatorioVeiculoService;
import dao.GenericDAO;

/**
 * Painel dinâmico da frota: visão geral de todos os veículos (custo, km,
 * custo/km), evolução histórica, alertas de manutenção e comparação entre
 * veículos — tudo em cards/gráficos (Chart.js), sem datatable tradicional.
 * Complementa {@link RelatorioVeiculoMB} (que fica com as abas detalhadas por
 * período); aqui o padrão é "todo o histórico" e o filtro é opcional.
 */
@ViewScoped
@Named("painelVeiculosMB")
public class PainelVeiculosMB implements Serializable {

	private static final long serialVersionUID = 1L;

	private Veiculo veiculo;
	private Date dataInicio;
	private Date dataFim;
	private String presetPeriodo = "TODO";

	private List<RelatorioCustoVeiculoDTO> listCusto = new ArrayList<>();
	private List<RelatorioCategoriaDTO> listCategoria = new ArrayList<>();
	private List<RelatorioTendenciaMensalDTO> listTendencia = new ArrayList<>();
	private List<AlertaDTO> listAlertas = new ArrayList<>();

	private Double custoTotalFrota = 0d;
	private Long kmTotalFrota = 0L;
	private Double custoPorKmMedioFrota;
	private Long quantidadeVeiculos = 0L;
	private Long quantidadeAlertasVencidos = 0L;
	private Long quantidadeAlertasProximos = 0L;

	private boolean carregado = false;

	@Inject
	private GenericDAO<Veiculo> daoVeiculo;

	@Inject
	private RelatorioVeiculoService relatorioVeiculoService;

	@Inject
	private AlertaService alertaService;

	public void buscar() {
		Long veiculoId = veiculo != null ? veiculo.getId() : null;

		listCusto = relatorioVeiculoService.custoPorVeiculo(dataInicio, dataFim, veiculoId);
		listCategoria = relatorioVeiculoService.custoPorCategoria(dataInicio, dataFim, veiculoId);
		listTendencia = relatorioVeiculoService.tendenciaMensal(dataInicio, dataFim, veiculoId);

		double somaCusto = 0;
		long somaKm = 0;
		for (RelatorioCustoVeiculoDTO dto : listCusto) {
			somaCusto += dto.getCustoTotal() == null ? 0 : dto.getCustoTotal();
			if (dto.getKmRodado() != null) {
				somaKm += dto.getKmRodado();
			}
		}
		custoTotalFrota = somaCusto;
		kmTotalFrota = somaKm;
		custoPorKmMedioFrota = somaKm > 0 ? somaCusto / somaKm : null;
		quantidadeVeiculos = (long) listCusto.size();

		listAlertas = new ArrayList<>();
		long vencidos = 0;
		long proximos = 0;
		for (AlertaDTO alerta : alertaService.listarAlertas()) {
			if (alerta.getVeiculo() == null || !alertaPertenceAoFiltro(alerta)) {
				continue;
			}
			listAlertas.add(alerta);
			if ("Vencido".equals(alerta.getUrgencia())) {
				vencidos++;
			} else if ("Próximo".equals(alerta.getUrgencia())) {
				proximos++;
			}
		}
		quantidadeAlertasVencidos = vencidos;
		quantidadeAlertasProximos = proximos;

		carregado = true;
	}

	private boolean alertaPertenceAoFiltro(AlertaDTO alerta) {
		if (veiculo == null) {
			return true;
		}
		String label = veiculo.getDescricao() + " (" + veiculo.getPlaca() + ")";
		return label.equals(alerta.getVeiculo());
	}

	public void aplicarPeriodo(String preset) {
		presetPeriodo = preset;
		Date hoje = new Date();
		if ("90D".equals(preset)) {
			dataFim = hoje;
			dataInicio = new Date(hoje.getTime() - (90L * 24 * 60 * 60 * 1000));
		} else if ("ANO".equals(preset)) {
			Calendar calendar = Calendar.getInstance();
			calendar.set(Calendar.MONTH, Calendar.JANUARY);
			calendar.set(Calendar.DAY_OF_MONTH, 1);
			dataInicio = calendar.getTime();
			dataFim = hoje;
		} else {
			dataInicio = null;
			dataFim = null;
		}
		buscar();
	}

	private String rotuloVeiculo(Veiculo v) {
		return v.getDescricao() + " (" + v.getPlaca() + ")";
	}

	/** JSON {labels, valores} pro ranking por veículo — custo total ou custo/km, conforme porKm. */
	private String construirRankingJson(boolean porKm) {
		JsonObject obj = new JsonObject();
		JsonArray labels = new JsonArray();
		JsonArray valores = new JsonArray();
		for (RelatorioCustoVeiculoDTO dto : listCusto) {
			if (porKm && dto.getCustoPorKm() == null) {
				continue;
			}
			labels.add(rotuloVeiculo(dto.getVeiculo()));
			valores.add(porKm ? dto.getCustoPorKm() : dto.getCustoTotal());
		}
		obj.add("labels", labels);
		obj.add("valores", valores);
		return obj.toString();
	}

	public String getCustoPorVeiculoChartJson() {
		return construirRankingJson(false);
	}

	public String getCustoPorKmChartJson() {
		return construirRankingJson(true);
	}

	public String getCategoriaChartJson() {
		JsonObject obj = new JsonObject();
		JsonArray labels = new JsonArray();
		JsonArray valores = new JsonArray();
		for (RelatorioCategoriaDTO dto : listCategoria) {
			labels.add(dto.getCategoria());
			valores.add(dto.getValorTotal());
		}
		obj.add("labels", labels);
		obj.add("valores", valores);
		return obj.toString();
	}

	public String getTendenciaChartJson() {
		JsonObject obj = new JsonObject();
		JsonArray labels = new JsonArray();
		JsonArray custos = new JsonArray();
		JsonArray custosPorKm = new JsonArray();

		SimpleDateFormat entrada = new SimpleDateFormat("yyyy-MM");
		SimpleDateFormat saida = new SimpleDateFormat("MMM/yy", new Locale("pt", "BR"));

		for (RelatorioTendenciaMensalDTO dto : listTendencia) {
			String rotulo = dto.getMesReferencia();
			try {
				rotulo = saida.format(entrada.parse(dto.getMesReferencia()));
			} catch (ParseException e) {
				// mantém "yyyy-MM" cru — não deveria acontecer, o valor é gerado por nós mesmos
			}
			labels.add(rotulo);
			custos.add(dto.getCustoTotal());
			custosPorKm.add(dto.getCustoPorKm());
		}
		obj.add("labels", labels);
		obj.add("custos", custos);
		obj.add("custosPorKm", custosPorKm);
		return obj.toString();
	}

	public List<Veiculo> completarVeiculo(String str) {
		List<Veiculo> list = daoVeiculo.listaComStatus(Veiculo.class);
		List<Veiculo> selecionados = new ArrayList<>();
		for (Veiculo cur : list) {
			if (cur.getDescricao().toLowerCase().contains(str.toLowerCase())) {
				selecionados.add(cur);
			}
		}
		return selecionados;
	}

	public Veiculo getVeiculo() {
		return veiculo;
	}

	public void setVeiculo(Veiculo veiculo) {
		this.veiculo = veiculo;
	}

	public Date getDataInicio() {
		return dataInicio;
	}

	public void setDataInicio(Date dataInicio) {
		this.dataInicio = dataInicio;
	}

	public Date getDataFim() {
		return dataFim;
	}

	public void setDataFim(Date dataFim) {
		this.dataFim = dataFim;
	}

	public String getPresetPeriodo() {
		return presetPeriodo;
	}

	public List<RelatorioCustoVeiculoDTO> getListCusto() {
		return listCusto;
	}

	public List<RelatorioCategoriaDTO> getListCategoria() {
		return listCategoria;
	}

	public List<AlertaDTO> getListAlertas() {
		return listAlertas;
	}

	public Double getCustoTotalFrota() {
		return custoTotalFrota;
	}

	public Long getKmTotalFrota() {
		return kmTotalFrota;
	}

	public Double getCustoPorKmMedioFrota() {
		return custoPorKmMedioFrota;
	}

	public Long getQuantidadeVeiculos() {
		return quantidadeVeiculos;
	}

	public Long getQuantidadeAlertasVencidos() {
		return quantidadeAlertasVencidos;
	}

	public Long getQuantidadeAlertasProximos() {
		return quantidadeAlertasProximos;
	}

	public boolean isCarregado() {
		return carregado;
	}

}
