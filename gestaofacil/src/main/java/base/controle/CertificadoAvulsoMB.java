package base.controle;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.hibernate.Session;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import com.ibm.icu.text.DateFormatSymbols;
import com.ibm.icu.text.SimpleDateFormat;

import base.modelo.Pessoa;
import base.modelo.PontoColeta;
import base.modelo.Usuario;
import base.modelo.CertificadoAvulso;
import base.modelo.Cidade;
import base.modelo.Cliente;
import base.modelo.Coleta;
import base.modelo.ContasReceber;
import base.modelo.ContasReceberParcelas;
import base.modelo.FechamentoMes;
import base.modelo.FechamentoMesItensPontoColeta;
import base.modelo.GrupoResiduo;
import base.modelo.InformacaoFinanceira;
import base.modelo.ItensColeta;
import base.modelo.ItensInformacaoFinanceiraGrupo;
import base.modelo.ItensInformacaoFinanceiraPontoColeta;
import base.service.CertificadoAvulsoService;
import base.service.ColetaService;
import base.service.ContasReceberParcelasService;
import base.service.ContasReceberService;
import base.service.FechamentoMesItensPontoColetaService;
import base.service.FechamentoMesService;
import base.service.GrupoResiduoService;
import base.service.InformacoesFinanceirasService;
import base.service.ItensColetaService;
import base.service.ItensInformacaoFinanceiraGrupoResiduoService;
import base.service.ItensInformacaoFinanceiraPontoColetaService;
import base.service.PontoColetaService;
import util.ChamarRelatorio;
import util.ExibirMensagem;
import util.FecharDialog;
import util.Mensagem;
import dao.GenericDAO;
import javax.persistence.EntityManager;
import javax.swing.JOptionPane;

@ViewScoped
@Named("certificadoAvulsoMB")
public class CertificadoAvulsoMB implements Serializable {

	private static final long serialVersionUID = 1L;

	private CertificadoAvulso certificadoAvulso;
	private List<CertificadoAvulso> listaCertificadosAvulso;

	@Inject
	private GenericDAO<CertificadoAvulso> daoCertificadoAvulso; // faz
																// as

	@Inject
	private CertificadoAvulsoService certificadoAvulsoService;
	
	@Inject
	private FechamentoMesService fechamentoMesService;

	@Inject
	private EntityManager manager;

	@PostConstruct
	public void inicializar() {
		criarNovoObjeto(null);
		buscarCertificados();
	}

	public void criarNovoObjeto(FechamentoMes f) {
		certificadoAvulso = new CertificadoAvulso();
		certificadoAvulso.setFechamentoMes(f);
		listaCertificadosAvulso = new ArrayList<>();
	}

	public void informaObjeto(CertificadoAvulso t) {
		this.certificadoAvulso = t;
	}

	public void buscarCertificadoFechamento(FechamentoMes t) {
		List<CertificadoAvulso> lc = daoCertificadoAvulso.listar(CertificadoAvulso.class,
				"fechamentoMes.id=" + t.getId());
		if (lc.size() > 0) {
			this.certificadoAvulso = lc.get(0);
		} else {
			this.certificadoAvulso = new CertificadoAvulso();
			this.certificadoAvulso.setFechamentoMes(t);
		}
	}

	public void inativar(CertificadoAvulso t) {
		t.setStatus(false);
		certificadoAvulsoService.inserirAlterar(t);
		// cidadeService.update(" Cidade set status = false where id = " +
		// t.getId());
		criarNovoObjeto(null);
		ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);
		buscarCertificados();
	}

	public void buscarCertificados() {
		listaCertificadosAvulso = daoCertificadoAvulso.listaComStatus(CertificadoAvulso.class, "dataEmissao desc");
	}
	
//	public void excluirFechamenot

	public void salvar() {

		try {
			if (certificadoAvulso.getId() == null) {
				certificadoAvulso.setStatus(true);
				certificadoAvulsoService.inserirAlterar(certificadoAvulso);
				
				certificadoAvulso.getFechamentoMes().setCertificadoAvulso(true);
				fechamentoMesService.inserirAlterar(certificadoAvulso.getFechamentoMes());

			} else {
				certificadoAvulso.setStatus(true);
				certificadoAvulsoService.inserirAlterar(certificadoAvulso);
				certificadoAvulso.getFechamentoMes().setCertificadoAvulso(true);
				fechamentoMesService.inserirAlterar(certificadoAvulso.getFechamentoMes());
			}

			criarNovoObjeto(null);
			ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);
			FecharDialog.fecharDialogCertificadoAvulso();
			buscarCertificados();

		} catch (Exception e) {
			ExibirMensagem.exibirMensagem(Mensagem.ERRO);
			e.printStackTrace();
		}

	}

	public void gerarCertificado(FechamentoMes fec) {

		List<CertificadoAvulso> lc = daoCertificadoAvulso.listar(CertificadoAvulso.class,
				"fechamentoMes.id=" + fec.getId());
		if (lc.size() > 0) {
			this.certificadoAvulso = lc.get(0);

			String dados = "<b><br/>" + fec.getInformacaoFinanceira().getCliente().getNomeRazaoSocial() + " - "
					+ fec.getInformacaoFinanceira().getCliente().getNomeFantasia() + " <br/>CPF/CNPJ: "
					+ fec.getInformacaoFinanceira().getCliente().getCpfCnpj() + " <br/>Endereço: "
					+ fec.getInformacaoFinanceira().getCliente().getEndereco() + ", "
					+ fec.getInformacaoFinanceira().getCliente().getNumero() + " -  "
					+ fec.getInformacaoFinanceira().getCliente().getCidade().getNome() + "-"
					+ fec.getInformacaoFinanceira().getCliente().getCidade().getEstado().getSigla() + "</b>";

			String periodo = "";
			if (fec.getInformacaoFinanceira().getFechamentoDataDiferente() == true) {
				periodo = "período de <b>"
						+ new SimpleDateFormat("dd/MM/yyyy", new Locale("pt", "BR"))
								.format(fec.getDataInicialFechamento())
						+ " a " + new SimpleDateFormat("dd/MM/yyyy").format(fec.getDataFinalFechamento()) + "</b>.";
			} else {
				periodo = "mês de <b>" + new DateFormatSymbols(new Locale("pt", "BR")).getMonths()[(fec.getMes() - 1)]
						+ " de " + fec.getAno() + "</b>.";
			}

			HashMap parametro = new HashMap<>();
			parametro.put("ID_FECHAMENTO_MES", fec.getId());
			parametro.put("DADOS", dados);
			parametro.put("PERIODO", periodo);
			parametro.put("TEXTO", this.certificadoAvulso.getTexto());
			ChamarRelatorio ch = new ChamarRelatorio("certificado_avulso.jasper", parametro, "certificado_" + fec.getId());
			Session sessions = manager.unwrap(Session.class);
			sessions.doWork(ch);

		} else {

		}
	}

	public CertificadoAvulso getCertificadoAvulso() {
		return certificadoAvulso;
	}

	public void setCertificadoAvulso(CertificadoAvulso certificadoAvulso) {
		this.certificadoAvulso = certificadoAvulso;
	}

	public List<CertificadoAvulso> getListaCertificadosAvulso() {
		return listaCertificadosAvulso;
	}

	public void setListaCertificadosAvulso(List<CertificadoAvulso> listaCertificadosAvulso) {
		this.listaCertificadosAvulso = listaCertificadosAvulso;
	}

}
