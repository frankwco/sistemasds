package util;

public class IntegerSingleton {

	private static IntegerSingleton uniqueInstance;
	private int controle=0;

	private IntegerSingleton() {
	}

	public static synchronized IntegerSingleton getInstance() {
		if (uniqueInstance == null)
			uniqueInstance = new IntegerSingleton();

		return uniqueInstance;
	}

	public int getControle() {
		return controle;
	}

	public void setControle(int controle) {
		this.controle = controle;
	}
	
	
}