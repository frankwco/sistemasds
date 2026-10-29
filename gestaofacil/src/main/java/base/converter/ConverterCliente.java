package base.converter;


import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.inject.Inject;
import javax.inject.Named;
import dao.GenericDAO;
import base.modelo.Cliente; 
import util.Mensagem;

@Named("converterCliente")
public class ConverterCliente implements Converter{

	@Inject
	private GenericDAO<Cliente> dao;

	@Override
	public Object getAsObject(FacesContext fc, UIComponent uic, String value) {
		//System.out.println("VALUE:: "+value);
		if (value != null && value.trim().length() > 0) {
			try {
				//System.out.println("AQUJII");
			 return dao.buscarPorId(Cliente.class, Long.parseLong(value));
			 
			} catch (Exception e) {
				e.printStackTrace();
				throw new ConverterException(
						new FacesMessage(FacesMessage.SEVERITY_ERROR, Mensagem.ERRO_CONVERTER, ""));
			}
		} else {
			//System.out.println("No nullll");
			return null;
		}
	}

	@Override
	public String getAsString(FacesContext context, UIComponent component, Object value) {
		if (!(value instanceof Cliente)) {
			//System.out.println("AQUJII 22");
			return null;
		}
		//System.out.println("no returnnn");
		    return  String.valueOf(((Cliente) value).getId());
		
		
	}
}
