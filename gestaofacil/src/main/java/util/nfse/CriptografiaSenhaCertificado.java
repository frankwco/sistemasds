package util.nfse;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import util.nfse.NfseNacionalClient.NfseException;

/**
 * Criptografia reversível (AES-GCM, chave de 128 bits; aceita 256) da senha do certificado A1.
 *
 * A senha precisa ser recuperada para abrir o .pfx, por isso não pode ser um
 * hash. A chave fica FORA do banco de dados, para que um backup/cópia do banco
 * sozinho não revele a senha. Origem da chave, nesta ordem:
 * <ol>
 * <li>variável de ambiente {@code NFSE_CHAVE_AES} (Base64 de 16 ou 32 bytes);</li>
 * <li>propriedade de sistema {@code -Dnfse.chave.aes=...};</li>
 * <li>arquivo {@code gestaofacil-nfse.key} em {@code <catalina.base>/conf} ou em
 * {@code <home do usuário>/.gestaofacil} - criado automaticamente no primeiro
 * uso, se não existir.</li>
 * </ol>
 * Valores gravados começam com {@code AES1:}. Valores sem o prefixo são senhas
 * antigas em texto puro e são devolvidos como estão (migração automática).
 */
public final class CriptografiaSenhaCertificado {

	public static final String PREFIXO = "AES1:";
	private static final String NOME_ARQUIVO = "gestaofacil-nfse.key";
	private static final int TAM_IV = 12;
	private static final int TAM_TAG_BITS = 128;

	private static volatile byte[] chaveCache;

	private CriptografiaSenhaCertificado() {
	}

	public static boolean estaCriptografada(String valor) {
		return valor != null && valor.startsWith(PREFIXO);
	}

	/** Criptografa (se ainda não estiver). Nulo/vazio continua nulo/vazio. */
	public static String criptografar(String senha) throws NfseException {
		if (senha == null || senha.isEmpty() || estaCriptografada(senha)) {
			return senha;
		}
		try {
			byte[] iv = new byte[TAM_IV];
			new SecureRandom().nextBytes(iv);
			Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
			c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(chave(), "AES"), new GCMParameterSpec(TAM_TAG_BITS, iv));
			byte[] cifrado = c.doFinal(senha.getBytes(StandardCharsets.UTF_8));
			byte[] tudo = new byte[iv.length + cifrado.length];
			System.arraycopy(iv, 0, tudo, 0, iv.length);
			System.arraycopy(cifrado, 0, tudo, iv.length, cifrado.length);
			return PREFIXO + Base64.getEncoder().encodeToString(tudo);
		} catch (NfseException e) {
			throw e;
		} catch (Exception e) {
			throw new NfseException("Erro ao criptografar a senha do certificado: " + e.getMessage(), e);
		}
	}

	/** Descriptografa; texto sem o prefixo (senha antiga) volta como está. */
	public static String descriptografar(String valor) throws NfseException {
		if (!estaCriptografada(valor)) {
			return valor;
		}
		try {
			byte[] tudo = Base64.getDecoder().decode(valor.substring(PREFIXO.length()));
			Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
			c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(chave(), "AES"),
					new GCMParameterSpec(TAM_TAG_BITS, tudo, 0, TAM_IV));
			byte[] claro = c.doFinal(tudo, TAM_IV, tudo.length - TAM_IV);
			return new String(claro, StandardCharsets.UTF_8);
		} catch (NfseException e) {
			throw e;
		} catch (Exception e) {
			throw new NfseException("Não foi possível descriptografar a senha do certificado (a chave de "
					+ "criptografia do servidor mudou ou foi perdida). Digite a senha do certificado novamente "
					+ "na Configuração NFS-e e salve.", e);
		}
	}

	private static byte[] chave() throws NfseException {
		byte[] k = chaveCache;
		if (k != null) {
			return k;
		}
		synchronized (CriptografiaSenhaCertificado.class) {
			if (chaveCache == null) {
				chaveCache = carregarChave();
			}
			return chaveCache;
		}
	}

	private static byte[] carregarChave() throws NfseException {
		String externa = System.getenv("NFSE_CHAVE_AES");
		if (externa == null || externa.trim().isEmpty()) {
			externa = System.getProperty("nfse.chave.aes");
		}
		if (externa != null && !externa.trim().isEmpty()) {
			return decodificar(externa.trim(), "NFSE_CHAVE_AES");
		}

		List<File> pastas = new ArrayList<>();
		String catalina = System.getProperty("catalina.base");
		if (catalina != null) {
			pastas.add(new File(catalina, "conf"));
		}
		pastas.add(new File(System.getProperty("user.home"), ".gestaofacil"));

		for (File pasta : pastas) {
			File arq = new File(pasta, NOME_ARQUIVO);
			if (arq.isFile()) {
				try {
					return decodificar(new String(Files.readAllBytes(arq.toPath()), StandardCharsets.US_ASCII).trim(),
							arq.getAbsolutePath());
				} catch (NfseException e) {
					throw e;
				} catch (Exception e) {
					throw new NfseException("Não foi possível ler a chave " + arq.getAbsolutePath() + ": "
							+ e.getMessage(), e);
				}
			}
		}

		// 128 bits: funciona em qualquer Java 8 (antes do 8u161 o Java recusa 256 bits)
		byte[] nova = new byte[16];
		new SecureRandom().nextBytes(nova);
		String conteudo = Base64.getEncoder().encodeToString(nova);
		StringBuilder falhas = new StringBuilder();
		for (File pasta : pastas) {
			File arq = new File(pasta, NOME_ARQUIVO);
			try {
				if (!pasta.isDirectory() && !pasta.mkdirs()) {
					falhas.append(pasta).append(" (não foi possível criar a pasta); ");
					continue;
				}
				Files.write(arq.toPath(), conteudo.getBytes(StandardCharsets.US_ASCII));
				try {
					Files.setPosixFilePermissions(arq.toPath(), PosixFilePermissions.fromString("rw-------"));
				} catch (UnsupportedOperationException e) {
					// Windows: sem permissões POSIX
				}
				System.out.println("[NFS-e] Chave de criptografia da senha do certificado criada em "
						+ arq.getAbsolutePath() + " - guarde uma cópia em local seguro.");
				return nova;
			} catch (Exception e) {
				falhas.append(arq).append(" (").append(e.getMessage()).append("); ");
			}
		}
		throw new NfseException("Não foi possível criar a chave de criptografia da senha do certificado: " + falhas
				+ "Defina a variável de ambiente NFSE_CHAVE_AES.");
	}

	private static byte[] decodificar(String b64, String origem) throws NfseException {
		try {
			byte[] k = Base64.getDecoder().decode(b64);
			if (k.length != 16 && k.length != 32) {
				throw new IllegalArgumentException("precisa ter 16 ou 32 bytes (tem " + k.length + ")");
			}
			return k;
		} catch (IllegalArgumentException e) {
			throw new NfseException("Chave de criptografia inválida em " + origem + ": " + e.getMessage(), e);
		}
	}
}
