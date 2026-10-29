package testes;


import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import base.modelo.ColetaApp;
import dao.GenericDAO;
import util.EnviarEmailThread;
import util.email.EmailColetaMensagem;

public class TestEmail {
    public static void main(String[] args) {
    	
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("cronosPU");
        EntityManager em = emf.createEntityManager();



       
        ColetaApp coleta = em.find(ColetaApp.class, 144367L);

        EnviarEmailThread tarefa = new EnviarEmailThread(
            "frankwco@gmail.com",   
            "<NÃO RESPONDA> Coleta de Resíduos",             
            EmailColetaMensagem.getMensagemComMTR(coleta)
        );

        Thread thread = new Thread(tarefa);
        thread.start();
    }
}
