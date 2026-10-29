package base.service;

import java.io.Serializable;

import javax.inject.Inject;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import com.google.gson.JsonObject;

import base.modelo.CategoriaIndicador;
import base.modelo.MensagemSite;
import base.modelo.Tipo;
import dao.GenericDAO;
import util.ExibirMensagem;
import util.Mensagem;
import util.Transacional;
import util.validadores.ValidaCNPJ;
import util.validadores.ValidaCPF;

public class MensagemSiteService implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Inject
	private GenericDAO<MensagemSite> dao;

	@Transacional
	public void inserirAlterar(MensagemSite tipo) {
		if (tipo.getId() == null) {
			dao.inserir(tipo);
		} else {
			dao.alterar(tipo);
		}
	}

	@Transacional
	public JsonObject salvar(MensagemSite mensagemSite) {

		String cpfCnpj = mensagemSite.getCpfCnpj();
		JsonObject retorno = new JsonObject();

		cpfCnpj = cpfCnpj.replace(".", "");
		cpfCnpj = cpfCnpj.replace("-", "");
		cpfCnpj = cpfCnpj.replace("/", "");
		cpfCnpj = cpfCnpj.replace(" ", "");
		cpfCnpj = cpfCnpj.replace(" ", "");
		cpfCnpj = cpfCnpj.replace(" ", "");
		cpfCnpj = cpfCnpj.trim();

		if (ValidaCNPJ.isCNPJ(cpfCnpj) || ValidaCPF.isCPF(cpfCnpj)) {

			try {
				String erro = "";
				if (mensagemSite.getNomeRazaoSocial().trim().equals("")) {
					erro += "Nome Obrigatorio. ";
				}
				if (mensagemSite.getCpfCnpj().trim().equals("")) {
					erro += "CPF/CNPJ Obrigatorio. ";
				}
				if (mensagemSite.getEmail().trim().equals("")) {
					erro += "E-mail Obrigatorio. ";
				}

				if (!isValidEmailAddress(mensagemSite.getEmail())) {
					erro += "E-mail 1 inválido. ";
				}
				if (!isValidEmailAddress(mensagemSite.getEmail2())) {
					erro += "E-mail 2 inválido. ";
				}

				if (!erro.equals("")) {
					retorno.addProperty("mensagem", erro);

				} else {

					mensagemSite.setTipo("atualizarDados");
					mensagemSite.setStatus(true);
					dao.inserir(mensagemSite);

					// mensagemCerto = ("Recebemos os seus dados, logo faremos a atualização de
					// cadastro. Assim que isso acontecer você receberá um e-mail com as informações
					// de acesso da nossa plataforma, lá você poderá ver os pesos das coletas,
					// romaneios, certificados, etc. Aguarde o e-mail!");
					if (mensagemSite.getId() != null) {
						retorno.addProperty("mensagem", mensagemSite.getId()
								+ " - Recebemos os seus dados, logo faremos a atualização de cadastro. Assim que isso acontecer você receberá um e-mail com as informações de acesso da nossa plataforma, lá você poderá ver os pesos das coletas, romaneios, certificados, etc. Aguarde o e-mail!");
						retorno.addProperty("status", "ok");
					} else {
						retorno.addProperty("mensagem", Mensagem.ERRO);
						retorno.addProperty("status", "erro");
					}

				}

			} catch (Exception e) {
				retorno.addProperty("mensagem", Mensagem.ERRO);
				retorno.addProperty("status", "erro");
				e.printStackTrace();
			}
		} else {
			retorno.addProperty("mensagem",
					"CPF ou CNPJ está incorreto. Informe um CPF/CNPJ válido e que corresponde ao contrato!!");
			retorno.addProperty("status", "erro");

		}
		System.out.println(retorno);
		return retorno;
	}

	public static boolean isValidEmailAddress(String email) {
		boolean result = true;
		if (email.trim().equals("")) {
			return true;
		}
		try {
			InternetAddress emailAddr = new InternetAddress(email);
			emailAddr.validate();
		} catch (AddressException ex) {
			result = false;
		}
		return result;
	}

}
