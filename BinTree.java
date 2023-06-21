public class BinTree<ContentType>{
	private ContentType content;
	private BinTree<ContentType> left;
	private BinTree<ContentType> right;

	public BinTree(){                      //Ein Baum wird erzeugt. Der Baum besitzt keine Teilbäume. Die Wurzel besitzt keinen Inhaltswert.
		this.content=null;
		this.left=null;
		this.right=null;
	}

	public BinTree(ContentType inhalt){     // Ein Baum wird erzeugt. Der Baum besitzt keine Teilbäume. Die Wurzel erhält den übergebenen Inhalt als Wert.
		this.content = inhalt;
		this.left=null;
		this.right=null;
	}

	public ContentType getItem(){              // Die Operation gibt den Inhaltswert der Wurzel des Baums zurück.
		return content;
	}

	public void setItem(ContentType inhalt){   // Die Wurzel des Baums erhält den übergebenen Inhalt als Wert.
		content = inhalt;
	}

	public boolean hasItem(){           // Wenn die Wurzel des Baums einen Inhaltswert besitzt, wird der Wert wahr zurückgegeben, sonst der Wert falsch.
		if (content==null){
			return false;}
		else {
			return true;
		} 
	}

	public void deleteItem() {            // Die Operation löscht den Inhaltswert der Wurzel des Baums.
		content=null;
	}

	public boolean isLeaf(){               // Wenn der Baum keine Teilbäume besitzt, die Wurzel des Baums also ein Blatt ist, wird der Wert wahr zurückgegeben, sonst der Wert falsch.
		if ((left==null) &&(right==null)) {
			return true;
		}
		else {
			return false;
		}
	}

	public boolean hasLeft(){            // Wenn der Baum einen linken Teilbaum besitzt, wird der Wert wahr zurückgegeben, sonst der Wert falsch.
		if (left != null){
			return true; 
		}
		else {
			return false;
		} 
	}

	public BinTree getLeft(){             // Die Operation gibt den linken Teilbaum zurück.
		return left;
	}

	public void setLeft(BinTree b){       // Der übergebene Baum wird als linker Teilbaum gesetzt.
		left = b;
	}

	public void deleteLeft(){             // Die Operation löscht den linken Teilbaum.
		left=null;
	}

	public boolean hasRight(){            // Wenn der Baum einen rechten Teilbaum besitzt, wird der Wert wahr zurückgegeben, sonst der Wert falsch.
		if (right != null){
			return true; 
		}
		else {
			return false;
		} // end of if-else
	}
	public BinTree getRight(){           // Die Operation gibt den rechten Teilbaum zurück.
		return right;
	} 

	public void setRight (BinTree b){     // Der übergebene Baum wird als rechter Teilbaum gesetzt.
		right = b;
	}

	public void deleteRight(){             // Die Operation löscht den rechten Teilbaum.
		right=null;
	}

}
