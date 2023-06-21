public class DynArray<ContentType> {
   
  private ContentType[] liste;  // Liste
  private int aktPos;    // Index des letzten Elements der Liste    
  
  public DynArray(){
    liste =(ContentType[]) new Object[0]; 
    aktPos=0;
  }           
  
  public void append(ContentType inhalt){
    if (isEmpty()) {
      ContentType[] hilfsarray=(ContentType[]) new Object[1];
      hilfsarray[aktPos]=inhalt;
      liste=hilfsarray;
      
    } else {
      ContentType [] hilfsarray =(ContentType[]) new Object[liste.length+1];
      for (int i=0;i<=aktPos;i++){
        hilfsarray[i]=liste[i];
      }
      hilfsarray[aktPos+1] = inhalt;
      liste = hilfsarray;
      aktPos++;
    }
  }
  
  public void insertAt(int index, ContentType inhalt){
    if ((index>=0)&&(index <=aktPos)){
        ContentType [] hilfsarray =(ContentType[]) new Object[liste.length+1];
        for (int i=0;i<index;i++){
          hilfsarray[i]=liste[i];
        }
        hilfsarray[index] = inhalt;
        for (int i=index;i<=aktPos;i++){
          hilfsarray[i+1]=liste[i];
        }
        liste = hilfsarray;
        aktPos++;
    } 
    else if (index == liste.length){
      append(inhalt);
    } // end of if-else
  }
   
  public ContentType getItem(int index){
    if (!isEmpty()){
      return liste[index];
    }
    else return null;
  }
  public void setItem(int index, ContentType inhalt){
    liste[index]=inhalt; 
  }

  public void delete(int index){
    if (!isEmpty()&& index<=aktPos){
      ContentType[] hilfsarray = (ContentType[]) new Object[liste.length-1];
      for (int i=0;i<index;i++){
        hilfsarray[i]=liste[i];
      }
      for (int i=index+1;i<liste.length;i++){
        hilfsarray[i-1]=liste[i];
      }
      aktPos--;
      liste = hilfsarray;
      if(aktPos < 0){
        aktPos = 0;
      }
    }
  }
    
  public boolean isEmpty(){
    return (liste.length==0);
  }
  
  public int getLength(){
    return liste.length;
  }
}
