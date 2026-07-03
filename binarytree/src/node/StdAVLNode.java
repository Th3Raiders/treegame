package node;

public class StdAVLNode<T> extends AbstractValuedNode<StdAVLNode<T>, T> implements AVLNode<StdAVLNode<T>, T> {

	// ATTRIBUTS
	
	private int height;

	// CONSTRUCTEURS
	
	public StdAVLNode(T v) {
		super(v);
		this.height = 1;
	}

	// REQUETES
	
	@Override
	public int getHeight(){ 
		return height; 
	}
	
	// COMMANDES
	
	public void setHeight(int h) {
		this.height = h;
	}
}
