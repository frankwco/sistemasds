package util;

import java.util.TimeZone;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Trava o fuso horário padrão da JVM em America/Sao_Paulo, independente de onde o servidor
 * de produção está hospedado.
 *
 * Sem isso, tudo que usa o fuso padrão implicitamente (new Date(), SimpleDateFormat,
 * Calendar.getInstance(), a conversão de java.util.Date feita pelo driver JDBC do MySQL ao
 * gravar/ler colunas DATETIME/TIMESTAMP, os componentes de data do PrimeFaces sem
 * timeZone explícito) usa o fuso do sistema operacional do host — se o host estiver em outro
 * país, toda hora exibida ou salva sai desalinhada da hora de Brasília.
 *
 * Precisa ser o PRIMEIRO listener declarado no web.xml (antes do Weld e do
 * ContextLoaderListener do Spring), pra garantir que o fuso já esteja certo antes de
 * qualquer outro código do app rodar.
 */
public class TimezoneListener implements ServletContextListener {

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		// nada a fazer
	}

}
