/*
 * Modélise le noeud d'un arbre binaire valué AVL.
 *  * @cons <pre>
 *     $DESC$ Un noeud n'ayant pas de sous arbre gauche ni droit.
 *     $ARGS$ T v
 *     $PRE$
 *     		  V != null
 *     $POST$ getLeft() == null
 *     		  getRight() == null
 *     		  getValue() == v
 *     		  getHeight() == 0 </pre>
 */


package node;

public interface AVLNode<N extends AVLNode<N,T>,T> extends ValuedNode<N, T> {
	
	// REQUETES
	
	public int getHeight();
	
	// COMMANDES
	
	public void setHeight(int h);
	
}
