package base.controle;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.faces.component.UIComponent;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.servlet.http.HttpServletResponse;

import base.modelo.ColetaApp;
import base.modelo.ColetaAppAssinatura;
import dao.GenericDAO;
import util.ByteToBase64Image;

@ViewScoped
@Named("backupMB")
public class BackupMB implements Serializable {

	private static final long serialVersionUID = 1L;
	private String tabela = "coleta_app_assinatura";
	// private String tabela = "estado";
	private String arquivo = tabela;
	private Integer mes;
	private Integer ano;
	private UIComponent inputMes;
	private UIComponent inputAno;

	@Inject
	private GenericDAO<ColetaAppAssinatura> daoColetaAppAssinatura; // faz as buscas

	@PostConstruct
	public void inicializar() {

	}

	public void backup() throws IOException {
		FacesContext context = FacesContext.getCurrentInstance();
		ExternalContext externalContext = context.getExternalContext();
		HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();

//        <h:form>
//        <h:commandButton value="Fazer backup" action="#{backupTabela.backup}">
//            <f:param name="tabela" value="nome_da_tabela"/>
//            <f:param name="arquivo" value="backup.sql"/>
//        </h:commandButton>
//    </h:form>

		// String tabela =
		// context.getExternalContext().getRequestParameterMap().get("tabela");
		// String arquivo =
		// context.getExternalContext().getRequestParameterMap().get("arquivo");

		try {

			String dataInicial = ano + "-" + mes + "-01";
			String dataFinal = ano + "-" + mes + "-31";
			// ResultSet rs = stmt.executeQuery("SELECT * FROM " + tabela+" where
			// dataInclusao between '"+dataInicial+"' and '"+dataInicial+"'");
			// ResultSet rs = stmt.executeQuery("SELECT * FROM " + tabela);

			response.setContentType("application/octet-stream");
			response.setHeader("Content-disposition",
					"attachment; filename=\"" + (arquivo + "_" + mes + "-" + ano) + ".sql\"");

			PrintWriter out = response.getWriter();

			// Query query = manager.createNativeQuery("SELECT * FROM " + tabela);
			// List<Object[]> rows = daoColetaAppAssinatura.backup(tabela, " where
			// dataInclusao between '"+dataInicial+"' and '"+dataFinal+"'");
			List<Object> rows = daoColetaAppAssinatura.backup(tabela,
					" where dataInclusao between '2000-04-01' and '2023-04-10'");
			
			
			// List<Object[]> rows = daoColetaAppAssinatura.backup(tabela, "");
//			for (Object[] row : rows) {
//				String insert = "INSERT INTO " + tabela + " VALUES (";
//				for (Object value : row) {
//					if (value == null) {
//						insert += "null,";
//					} else if (value instanceof byte[]) {
//						// tratar o campo Blob
//						byte[] blobData = (byte[]) value;
//						//insert += Base64.getEncoder().encodeToString(blobData);
//						insert+=bytesToHex(blobData)+",";
//					} else if (value instanceof Number) {
//						insert += value + ",";
//					} else {
//						insert += "'" + value + "',";
//					}
//				}
//				insert = insert.substring(0, insert.length() - 1) + ");";
//				out.println(insert);
//			}
			
			

//            while (rs.next()) {
//                StringBuilder linha = new StringBuilder();
//                linha.append("INSERT INTO ");
//                linha.append(tabela);
//                linha.append(" (");
//
//                ResultSetMetaData meta = rs.getMetaData();
//                int colCount = meta.getColumnCount();
//
//                for (int i = 1; i <= colCount; i++) {
//                    String colName = meta.getColumnName(i);
//                    linha.append(colName);
//                    if (i < colCount) {
//                        linha.append(", ");
//                    }
//                }
//
//                linha.append(") VALUES (");
//
//                for (int i = 1; i <= colCount; i++) {
//                    String value = rs.getString(i);
//                    if (value == null) {
//                        linha.append("NULL");
//                    } else {
//                        linha.append("'");
//                        linha.append(value);
//                        linha.append("'");
//                    }
//                    if (i < colCount) {
//                        linha.append(", ");
//                    }
//                }
//
//                linha.append(");");
//                out.println(linha.toString());
//            }

			out.close();
			context.responseComplete();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	public static String bytesToHex(byte[] bytes) {
	    char[] hexArray = "0123456789ABCDEF".toCharArray();
	    char[] hexChars = new char[bytes.length * 2];
	    for (int i = 0; i < bytes.length; i++) {
	        int v = bytes[i] & 0xFF;
	        hexChars[i * 2] = hexArray[v >>> 4];
	        hexChars[i * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}



	public String getTabela() {
		return tabela;
	}

	public void setTabela(String tabela) {
		this.tabela = tabela;
	}

	public String getArquivo() {
		return arquivo;
	}

	public void setArquivo(String arquivo) {
		this.arquivo = arquivo;
	}

	public Integer getMes() {
		return mes;
	}

	public void setMes(Integer mes) {
		this.mes = mes;
	}

	public Integer getAno() {
		return ano;
	}

	public void setAno(Integer ano) {
		this.ano = ano;
	}

	public UIComponent getInputMes() {
		return inputMes;
	}

	public void setInputMes(UIComponent inputMes) {
		this.inputMes = inputMes;
	}

	public UIComponent getInputAno() {
		return inputAno;
	}

	public void setInputAno(UIComponent inputAno) {
		this.inputAno = inputAno;
	}

}
