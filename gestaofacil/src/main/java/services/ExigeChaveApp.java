package services;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.ws.rs.NameBinding;

/**
 * Marca um recurso/método JAX-RS como exigindo a chave de API do
 * appgestaofacil (ver {@link ChaveApiFilter}). Aplicada na classe
 * {@link ServicoApp}, protege todos os endpoints que o app usa, sem afetar
 * o restante do {@link ServicoHello} (usado por outros consumidores, como o
 * site institucional).
 */
@NameBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface ExigeChaveApp {
}
