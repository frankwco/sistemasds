package base.modelo;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Texto pronto para a descrição da NFS-e, mostrado como botão na emissão.
 * Aceita {MES_NOME}, {MES}, {ANO}, {BOLETO}, {CLIENTE} e {PREENCHER} (este
 * último bloqueia a emissão até ser trocado pelo texto certo).
 */
@Entity
@Table(name = "nfse_modelo_descricao")
public class NfseModeloDescricao implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@Column(length = 60)
	private String nome;

	@Column(length = 2000)
	private String texto;

	private Integer ordem = 1;

	private Boolean status = true;

	public NfseModeloDescricao() {
	}

	public NfseModeloDescricao(String nome, String texto, int ordem) {
		this.nome = nome;
		this.texto = texto;
		this.ordem = ordem;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getTexto() {
		return texto;
	}

	public void setTexto(String texto) {
		this.texto = texto;
	}

	public Integer getOrdem() {
		return ordem;
	}

	public void setOrdem(Integer ordem) {
		this.ordem = ordem;
	}

	public Boolean getStatus() {
		return status;
	}

	public void setStatus(Boolean status) {
		this.status = status;
	}
}
