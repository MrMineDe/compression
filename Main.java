import java.io.*;


public class Main {

	private static final int ASCII_MAX = 127;
	private static final int MAX_LZW_POINTER_BITS = 4;
	private static final int ASCII_BIT_COUNT = 7;
	private static final int ALGORITHM_ENCODING_BIT_LEN = 3;
	private static final int ALGORITHM_ENCODING_FINISHED = 0;
	private static final int ALGORITHM_ENCODING_LZW = 1;
	private static final int ALGORITHM_ENCODING_LL2 = 2;
	private static final int ALGORITHM_ENCODING_LL3 = 3;
	private static final int ALGORITHM_ENCODING_LL4 = 4;
	private static final int ALGORITHM_ENCODING_HUFFMAN = 5;
	private static final int ALGORITHM_ENCODING_XYZ = 6;
	private static final int ALGORITHM_ENCODING_PRIMFAKTOR = 7;
	//Wie viel? Lohnt sich dynamisch?
	private static final int XYZ_X_ENCODING_BIT_LEN = 2;
	private static final int XYZ_Y_ENCODING_BIT_LEN = 2;
	private static final int XYZ_Z_ENCODING_BIT_LEN = 6;


	// returns the position of the searchterm in the dynarray and -1 if the searchterm is not found
	public static int findInDynArray(String search_term, DynArray<String> dynarray){
		for (int i = 0; i < dynarray.getLength(); i++) {
			if(search_term.equals(dynarray.getItem(i))){
				return i;
			}
		}
		return -1;
	}

	// For Context on how LZW compression works see: https://www.youtube.com/watch?v=dLvvGXwKUGw
	public static DynArray<Boolean> lzw_encode(DynArray<Integer> input){
		DynArray<String> dictionary = new DynArray<String>();
		DynArray<Integer> out = new DynArray<Integer>();
		String prev_word = new String(""),
		       cur_word = new String("");
		boolean dublicate = false;
		// This is needed for converting to booleans. This defines the amount
		// of Bits used for "pointers"
		int bitsForPointers = 0;

		prev_word = String.valueOf((char)(int)input.getItem(0));
		for(int i=1; i < input.getLength(); i++){
			dublicate = true;
			cur_word = String.valueOf((char)(int)input.getItem(i));
			if(findInDynArray(prev_word + cur_word, dictionary) == -1){
				dublicate = false;
			}
			if(dublicate){
				prev_word = (prev_word + cur_word);
			} else {
				dictionary.append(prev_word+cur_word);
				int encodingForPrev_word = ASCII_MAX + findInDynArray(prev_word, dictionary);
				if(bitsForPointers < bitLength(encodingForPrev_word+1 - (ASCII_MAX))){
					bitsForPointers = bitLength(encodingForPrev_word+1 - (ASCII_MAX));
				}
				// If the prev word is not found in the dictionary the value should 
				// be just the character
				if(encodingForPrev_word == ASCII_MAX-1){
					encodingForPrev_word = prev_word.charAt(0);
				}
				out.append(encodingForPrev_word);
				prev_word = "" + cur_word;
			}
		}
		int encodingForPrev_word = ASCII_MAX + findInDynArray(prev_word, dictionary);
		if(bitsForPointers < bitLength(encodingForPrev_word+1 - (ASCII_MAX))){
			bitsForPointers = bitLength(encodingForPrev_word+1 - (ASCII_MAX));
		}
		if(encodingForPrev_word == ASCII_MAX-1){
			encodingForPrev_word = prev_word.charAt(0);
		}
		out.append(encodingForPrev_word);

		System.out.println(bitsForPointers);
		DynArray<Boolean> out2 = new DynArray<Boolean>();
		appendToBooleanArray(out2, convertToBoolean(MAX_LZW_POINTER_BITS, bitsForPointers-1));
		for (int i = 0; i < out.getLength(); i++) {
			if(out.getItem(i) >= ASCII_MAX){
				out2.append(true);
				appendToBooleanArray(out2, convertToBoolean(bitsForPointers, out.getItem(i)-ASCII_MAX));
			} else {
				out2.append(false);
				appendToBooleanArray(out2, convertToBoolean(ASCII_BIT_COUNT, out.getItem(i)));
			}
		}
		return out2;
	}

	public static DynArray<Integer> lzw_decode(DynArray<Boolean> input){
		DynArray<Integer> out = new DynArray<Integer>();
		DynArray<String> dict = new DynArray<String>();
		String prev_word,
		       cur_word;
		int bitsForPointers = 0;
		//The first 4 Bits hold the amount of bits used for each pointer -1
		//because the value cannot be 0, so the count can start at 1
		bitsForPointers = convertToInt(input, 0, 3) + 1;

		prev_word = String.valueOf((char)convertToInt(input, 5, 5+ASCII_BIT_COUNT-1));
		out.append((int)prev_word.charAt(0));

		int i = 5+ASCII_BIT_COUNT;
		while(i < input.getLength()){
			boolean isPointer = input.getItem(i);
			i++;
			if(!isPointer){
				cur_word = String.valueOf((char)convertToInt(input, i, i+ASCII_BIT_COUNT-1));
				i += ASCII_BIT_COUNT;

				out.append((int)cur_word.charAt(0));
				//Yes this is a queue for only one item
				dict = lzwResolveQueue(dict, cur_word, prev_word);
				prev_word = dict.getItem(dict.getLength()-1);
				dict.delete(dict.getLength()-1);
			}
			if(isPointer){
				int pointerValue = convertToInt(input, i, i+bitsForPointers-1);
				i += bitsForPointers;

				int prev_wordLen = 0;
				//Start queue if the pointer calculates itself
				if(pointerValue > dict.getLength()-1){
					prev_wordLen = prev_word.length()-1;
					dict = lzwResolveQueue(dict, prev_word, prev_word);
					prev_word = dict.getItem(dict.getLength()-1);
					dict.delete(dict.getLength());
				}

				dict = lzwResolveQueue(dict, dict.getItem(pointerValue).substring(prev_wordLen), prev_word);
				prev_word = dict.getItem(dict.getLength()-1);
				dict.delete(dict.getLength()-1);
				for(int j=0; j < dict.getItem(pointerValue).length(); j++){
					out.append((int)dict.getItem(pointerValue).charAt(j));
				}
			}
		}
		return out;
	}

	public static DynArray<String> lzwResolveQueue(DynArray<String> dict,
	                                               String queue,
	                                               String prev_word){
		String cur_word;
		boolean dublicate;
		for(int i=0; i < queue.length(); i++){
			dublicate = true;
			cur_word = String.valueOf(queue.charAt(i));
			if(findInDynArray(prev_word + cur_word, dict) == -1){
				dublicate = false;
			}
			if(dublicate){
				prev_word = (prev_word + cur_word);
			} else {
				dict.append(prev_word + cur_word);
				prev_word = "" + cur_word;
			}
		}
		//the prev_word has to be returned so it gets attached to the dict
		//and has to be deleted from the calling function!!
		dict.append(prev_word);
		return dict;
	}

	public static int bitLength(int num){
		int bitlength = 1;
		while(Math.pow(2 , bitlength) < num){
			bitlength++;
		}
		return bitlength;
	}

	public static DynArray<Boolean> appendToBooleanArray(DynArray<Boolean> orig, Boolean[] appends){
		for(int i=0; i < appends.length; i++){
			orig.append(appends[i]);
		}
		return orig;
	}

	public static Boolean[] convertToBoolean(int boolean_array_length, int input){
		Boolean [] output = new Boolean[boolean_array_length];
		for (int i = 0; i < boolean_array_length ; i++) {
			output[boolean_array_length-1-i] = (((input >> i) & 1) != 0);
		}
		return output;
	}

	public static int convertToInt(DynArray<Boolean> input, int index_min, int index_max){
		int output = 0;
		for (int i=index_min; i <= index_max; i++){
			if(input.getItem(i))
				output += 1 << (index_max-i); // idk ob dass richig ist, überprüfen
		}
		return output;
	}

	public static DynArray<Integer> readFile(String filepath){
		File file = new File(filepath);
		DynArray<Integer> data = new DynArray<Integer>();
		if(!file.exists()){
			System.out.println("Error opening file. Does it exist? Do you have permission?");
			return null;
		}
		try{
			FileInputStream fis= new FileInputStream(file);
			int r = 0;

			while((r = fis.read()) != -1){
				data.append(r);
			}
			fis.close();
		} catch ( Exception IOException) {
			System.out.println("Error reading!");
		} 
		return data;
	}

	public static void output_data(DynArray<Integer> data){
		for (int i=0; i < data.getLength(); i++) {
			System.out.print(data.getItem(i) + "; ");
		}
	}

	public static void output_data_bol(DynArray<Boolean> data){
		for (int i=0; i < data.getLength(); i++) {
			System.out.print(data.getItem(i) + "; ");
		}
	}

	public static DynArray<Boolean> dynIntToBool(DynArray<Integer> in){
		DynArray<Boolean> out = new DynArray<Boolean>();
		for(int i=0; i < in.getLength(); i++){
			appendToBooleanArray(out, convertToBoolean(ASCII_BIT_COUNT, in.getItem(i)));
		}
		return out;
	}

	public static DynArray<Integer> dynBoolToInt(DynArray<Boolean> in){
		DynArray<Integer> out = new DynArray<Integer>();
		int i=0;
		for (i=0; i < 7; i++){
			if(in.getItem(i) != false){
				break;
			}
		}
		for(i++; i < in.getLength()/7; i++){
			out.append(convertToInt(in, i*7, i*7+6));
		}
		return out;
	}

	public static DynArray<Boolean> huffman_encode(DynArray<Integer> in){
		return new DynArray<Boolean>();
	}

	//FUNKTIONIERT NUR BEDINGT:
	//IMMER 2^2 und IMMER NUR 8 BITS... GEHT DASS NICHT BESSER?
	//ACHJA UND ENCODE UND DECODE MUSS GETESTET WERDEN
	public static DynArray<Boolean> xyz_encode(DynArray<Boolean> in){
		DynArray<Boolean> out = new DynArray<Boolean>();
		appendToBooleanArray(out, convertToBoolean(XYZ_ENCODING_REST_BIT_LEN, in.getLength()%28)));
		for(int i=0; i < in.getLength()/28; i++){
			//make zahl from 7 Bits
			int zahl;
			zahl = convertToInt(in, i*28, i*28 + 6);

			int besteRest = zahl;
			int besteX = 99999999;
			int besteY = 99999999;
			int besteZ = 99999999;
			for (int x = 2; x < 5000; x++) {    //x wird jeden durchlauf um 1 denn x darf nicht größer als wurzel von zahl           
				for (int y = 2; y < 20; y++) {     //y alle durchlaufen bis y=20
					int z = zahl - (int)Math.pow(x, y);  //zugehöriges z wird errechnet
					if (bitLength(z) <= XYZ_Z_ENCODING_BIT_LEN && 
					bitLength(x) <= XYZ_X_ENCODING_BIT_LEN &&
					bitLength(y) <= XYZ_Y_ENCODING_BIT_LEN) {    //falls z>0 und 
						besteRest = z;
						besteX = x;
						besteY = y;
						besteZ = z;
					}
				}
			}

			System.out.println(zahl + " = " + besteX + " ^ " + besteY + " + " + besteZ);

			out = appendToBooleanArray(out, convertToBoolean(XYZ_X_ENCODING_BIT_LEN, besteX));
			out = appendToBooleanArray(out, convertToBoolean(XYZ_Y_ENCODING_BIT_LEN, besteY));
			out = appendToBooleanArray(out, convertToBoolean(XYZ_Z_ENCODING_BIT_LEN, besteZ));
		}

		return out;
	}

	public static DynArray<Boolean> encode(DynArray<Integer> in){
		DynArray<Boolean> out = dynIntToBool(in);
		DynArray<Boolean> orig = new DynArray<Boolean>();

		int countMixerAfterEachOther = 0;
		while (true){
			DynArray<Boolean> lzw_encoded = lzw_encode(dynBoolToInt(out));
			DynArray<Boolean> ll_encoded2 = LauflangeTwo(out);
			DynArray<Boolean> ll_encoded3 = LauflangeThree(out);
			DynArray<Boolean> ll_encoded4 = LauflangeFour(out);
			DynArray<Boolean> huffman_encoded = huffman_encode(dynBoolToInt(out));

			int lzw_encoded_len = lzw_encoded.getLength();
			int ll_encoded2_len = ll_encoded2.getLength();
			int ll_encoded3_len = ll_encoded3.getLength();
			int ll_encoded4_len = ll_encoded4.getLength();
			int huffman_encoded_len = huffman_encoded.getLength();

			if(lzw_encoded_len <= ll_encoded2_len &&
			   lzw_encoded_len <= ll_encoded3_len &&
			   lzw_encoded_len <= ll_encoded4_len &&
			   lzw_encoded_len <= huffman_encoded_len &&
			   lzw_encoded_len+ALGORITHM_ENCODING_BIT_LEN < out.getLength()){
				out = new DynArray<Boolean>();
				// This is to round up so the out array is dividable through 7. This is needed for convertion from Boolean to int
				for(int i=0; i < (ASCII_BIT_COUNT-1) - (lzw_encoded_len+ALGORITHM_ENCODING_BIT_LEN) % ASCII_BIT_COUNT; i++){
					out.append(false);
				}
				out.append(true);

				appendToBooleanArray(out, convertToBoolean(ALGORITHM_ENCODING_BIT_LEN, ALGORITHM_ENCODING_LZW));
				for(int i=0; i < lzw_encoded.getLength(); i++){
					out.append(lzw_encoded.getItem(i));
				}
				countMixerAfterEachOther = 0;
			} else if(ll_encoded2_len <= lzw_encoded_len &&
			          ll_encoded2_len <= ll_encoded3_len &&
			          ll_encoded2_len <= ll_encoded4_len &&
			          ll_encoded2_len <= huffman_encoded_len && 
			          ll_encoded2_len+ALGORITHM_ENCODING_BIT_LEN < out.getLength()){
				out = new DynArray<Boolean>();
				// This is to round up so the out array is dividable through 7. This is needed for convertion from Boolean to int
				for(int i=0; i < (ASCII_BIT_COUNT-1) - (ll_encoded2_len+ALGORITHM_ENCODING_BIT_LEN) % ASCII_BIT_COUNT; i++){
					out.append(false);
				}
				out.append(true);
				appendToBooleanArray(out, convertToBoolean(ALGORITHM_ENCODING_BIT_LEN, ALGORITHM_ENCODING_LL2));
				for(int i=0; i < ll_encoded2.getLength(); i++){
					out.append(ll_encoded2.getItem(i));
				}
				countMixerAfterEachOther = 0;
			} else if(ll_encoded3_len <= ll_encoded2_len &&
			          ll_encoded3_len <= lzw_encoded_len &&
			          ll_encoded3_len <= ll_encoded4_len &&
			          ll_encoded3_len <= huffman_encoded_len &&
			          ll_encoded3_len+ALGORITHM_ENCODING_BIT_LEN < out.getLength()){
				out = new DynArray<Boolean>();
				// This is to round up so the out array is dividable through 7. This is needed for convertion from Boolean to int
				for(int i=0; i < (ASCII_BIT_COUNT-1) - (ll_encoded3_len+ALGORITHM_ENCODING_BIT_LEN) % ASCII_BIT_COUNT; i++){
					out.append(false);
				}
				out.append(true);
				appendToBooleanArray(out, convertToBoolean(ALGORITHM_ENCODING_BIT_LEN, ALGORITHM_ENCODING_LL3));
				for(int i=0; i < ll_encoded3.getLength(); i++){
					out.append(ll_encoded3.getItem(i));
				}
				countMixerAfterEachOther = 0;
			} else if(ll_encoded4_len <= ll_encoded2_len &&
			          ll_encoded4_len <= ll_encoded3_len &&
			          ll_encoded4_len <= lzw_encoded_len &&
			          ll_encoded4_len <= huffman_encoded_len &&
			          ll_encoded4_len+ALGORITHM_ENCODING_BIT_LEN < out.getLength()){
				out = new DynArray<Boolean>();
				// This is to round up so the out array is dividable through 7. This is needed for convertion from Boolean to int
				for(int i=0; i < (ASCII_BIT_COUNT-1) - (ll_encoded4_len+ALGORITHM_ENCODING_BIT_LEN) % ASCII_BIT_COUNT; i++){
					out.append(false);
				}
				out.append(true);
				appendToBooleanArray(out, convertToBoolean(ALGORITHM_ENCODING_BIT_LEN, ALGORITHM_ENCODING_LL4));
				for(int i=0; i < ll_encoded4.getLength(); i++){
					out.append(ll_encoded4.getItem(i));
				}
				countMixerAfterEachOther = 0;
			} else if(huffman_encoded_len <= ll_encoded2_len &&
			          huffman_encoded_len <= ll_encoded3_len &&
			          huffman_encoded_len <= ll_encoded4_len &&
			          huffman_encoded_len <= lzw_encoded_len &&
			          huffman_encoded_len+ALGORITHM_ENCODING_BIT_LEN < out.getLength()){
				out = new DynArray<Boolean>();
				// This is to round up so the out array is dividable through 7. This is needed for convertion from Boolean to int
				for(int i=0; i < (ASCII_BIT_COUNT-1) - (huffman_encoded_len+ALGORITHM_ENCODING_BIT_LEN) % ASCII_BIT_COUNT; i++){
					out.append(false);
				}
				out.append(true);
				appendToBooleanArray(out, convertToBoolean(ALGORITHM_ENCODING_BIT_LEN, ALGORITHM_ENCODING_HUFFMAN));
				for(int i=0; i < huffman_encoded.getLength(); i++){
					out.append(huffman_encoded.getItem(i));
				}
				countMixerAfterEachOther = 0;
			} else {
				// Stop encoding when you only done mixer functions 3 times in a row
				if(countMixerAfterEachOther > 3){
					break;
				}

				// Save the text before throwing it through the mixer. If no method is useful after mixing it is smarter
				// to just take the orig text before mixing, because mixing always adds a few bits at least
				if(countMixerAfterEachOther == 0){
					orig = new DynArray<Boolean>();
					for(int i=0; i < out.getLength(); i++){
						orig.append(out.getItem(i));
					}
				}

				DynArray<Boolean> xyz_encoded = xyz_encode(out);
				DynArray<Boolean> primfaktor_encoded = primfaktor_encode(out);

				if(xyz_encoded.getLength() <= primfaktor_encoded.getLength()){
					out = new DynArray<Boolean>();
					appendToBooleanArray(out, convertToBoolean(ALGORITHM_ENCODING_BIT_LEN, ALGORITHM_ENCODING_XYZ));
					for(int i=0; i < xyz_encoded.getLength(); i++){
						out.append(xyz_encoded.getItem(i));
					}
				} else {
					out = new DynArray<Boolean>();
					appendToBooleanArray(out, convertToBoolean(ALGORITHM_ENCODING_BIT_LEN, ALGORITHM_ENCODING_PRIMFAKTOR));
					for(int i=0; i < primfaktor_encoded.getLength(); i++){
						out.append(primfaktor_encoded.getItem(i));
					}
				}
				countMixerAfterEachOther++;
			}
		}
		return orig;
	}

	public static DynArray<Boolean> decode(DynArray<Boolean> in){
		DynArray<Boolean> out = new DynArray<Boolean>();
		int algorithm = -1;
		while(algorithm != ALGORITHM_ENCODING_FINISHED){

			//delete the fill zeros that got there for dividadability (ka)
			while(out.getItem(0)==false){
				out.delete(0);
			}
			out.delete(0);

			algorithm = convertToInt(out, 0, ALGORITHM_ENCODING_BIT_LEN-1);
			//delete the algorithm hint bits, they are no longer needed
			for(int i=0; i < 3; i++){
				out.delete(0);
			}

			switch(algorithm){
			case ALGORITHM_ENCODING_LZW:
				out = dynIntToBool(lzw_decode(out));
				break;
			case ALGORITHM_ENCODING_LL2:
				out = LauflangeTwoDecode(out);
				break;
			case ALGORITHM_ENCODING_LL3:
				out = LauflangeThreeDecode(out);
				break;
			case ALGORITHM_ENCODING_LL4:
				out = LauflangeFourDecode(out);
				break;
			case ALGORITHM_ENCODING_HUFFMAN:
				out = huffman_decode(out);
				break;
			case ALGORITHM_ENCODING_XYZ:
				out = xyz_decode(out);
				break;
			case ALGORITHM_ENCODING_PRIMFAKTOR:
				out = primfaktor_decode(out);
				break;
			}

		}
		return out;
	}

	public static DynArray<Boolean> primfaktor_encode(DynArray<Boolean> in){
		return new DynArray<Boolean>();
	}
	public static DynArray<Boolean> primfaktor_decode(DynArray<Boolean> in){
		return new DynArray<Boolean>();
	}
	public static DynArray<Boolean> xyz_decode(DynArray<Boolean> in){
		return new DynArray<Boolean>();
	}
	public static DynArray<Boolean> huffman_decode(DynArray<Boolean> in){
		return new DynArray<Boolean>();
	}


	public static void main(String[] x){

		DynArray<Integer> data = readFile("file");
		//DynArray<Boolean> databol = lzw_encode(data);
		System.out.println(data.getLength()*7);

		DynArray<Boolean> databol = xyz_encode(dynIntToBool(data));

		//output_data_bol(lauflaenge);
		System.out.println("");
		output_data_bol(databol);
		System.out.println();

		System.out.println(databol.getLength());
		System.out.println(data.getLength() * 7);
		System.out.println((double)databol.getLength()/(data.getLength()*7));
		System.out.println();
		/*DynArray<Integer> data_later = lzw_decode(databol);
		System.out.println(data_later.getLength()*7);
		//output_data(data_later);
		int i=0;
		for(i=0; i < data.getLength(); i++){
			if(i >= data_later.getLength()){
				System.out.println("FETT VERKACKT WIRKLIDH");
				break;
			}
			if(data.getItem(i) != data_later.getItem(i)){
				System.out.println("Verkackt bei element: " + i);
				break;
			}
		}
		if(i < data_later.getLength()-1){
			System.out.println("nicht so optimal...");
		}
		*/
	}

	//Codierung
	public static DynArray<Boolean> LauflangeTwo(DynArray<Boolean> in) {       
		Boolean letztesW = in.getItem(0);    //Erstellen der nötigen Hilfsvariablen
		Boolean aktuellesW = false;  
		DynArray<Boolean> komp = new DynArray<Boolean>(); //Variable die Ausgegeben wird
		int count=1;
		for (int i = 1; i < in.getLength(); i++) {
			aktuellesW=in.getItem(i);
			if (aktuellesW==letztesW) {         //es wird überprüf, ob der aktuelle Bit mit dem Vorherigen übereinstimmt 
				count++;
				if (count>4) {                    //wenn die Bits häufiger als 4 mal in folge kommen, wird die Sorte gefolgt von der Angabe, dass es viermal auftritt 
					komp.append(letztesW);                              
					appendToBooleanArray(komp,convertToBoolean(2,3));
					count=1;
				} // end of if
			} else {                            //wenn nicht mehr die gleichen  Bits auf einander folgen, wird die Sorte der Bits und dann die Haufigkeit angegeben
			komp.append(letztesW);
			appendToBooleanArray(komp,convertToBoolean(2,count-1));
				count=1;
				letztesW=aktuellesW;
			} // end of if-else
		}
		komp.append(letztesW);
		appendToBooleanArray(komp,convertToBoolean(2,count-1));    //innerhalb der Schleife wirden die letzte Folge gleicher Bits nicht angehangen, daher muss sie danach angehangen werden.
		return komp;
	}

	public static DynArray<Boolean> LauflangeThree(DynArray<Boolean> in) {       
		Boolean letztesW = in.getItem(0);    //Erstellen der nötigen Hilfsvariablen
		Boolean aktuellesW = false;
		DynArray<Boolean> komp = new DynArray<Boolean>();  //Variable die Ausgegeben wird
		int count=1;
		letztesW=in.getItem(0);
		for (int i = 1; i < in.getLength(); i++) {     //Theorie bleibt gleich
			aktuellesW=in.getItem(i);
			if (aktuellesW==letztesW) {
				count++;
				if (count>8) {                             //aber es können mehr Bits innerhalb einer Angabe gespeichert werden
					komp.append(letztesW);
					appendToBooleanArray(komp,convertToBoolean(3,7));
					count=1;
				} // end of if
			} else {
			komp.append(letztesW);
			appendToBooleanArray(komp,convertToBoolean(3,count-1));
				count=1;
				letztesW=aktuellesW;
			} // end of if-else
		}
		komp.append(letztesW);
		appendToBooleanArray(komp,convertToBoolean(3,count-1));
		return komp;
	}

	public static DynArray<Boolean> LauflangeFour(DynArray<Boolean> in) {       
		Boolean letztesW = in.getItem(0);    //Erstellen der nötigen Hilfsvariablen
		Boolean aktuellesW = false;
		DynArray<Boolean> komp = new DynArray<Boolean>(); //Variable die Ausgegeben wird
		int count=1;
		letztesW=in.getItem(0);
		for (int i = 1; i < in.getLength(); i++) {     //Theorie bleibt gleich
			aktuellesW=in.getItem(i);
			if (aktuellesW==letztesW) {
				count++;
				if (count>16) {                            //aber es können mehr Bits innerhalb einer Angabe gespeichert werden
					komp.append(letztesW);
					appendToBooleanArray(komp,convertToBoolean(4,15));
					count=1;
				} // end of if
			} else {
			komp.append(letztesW);
			appendToBooleanArray(komp,convertToBoolean(4,count-1));
				count=1;
				letztesW=aktuellesW;
			} // end of if-else
		}
		komp.append(letztesW);
		appendToBooleanArray(komp,convertToBoolean(4,count-1));
		return komp;
	}

	//Decodierung

	public static DynArray<Boolean> LauflangeTwoDecode(DynArray<Boolean> in) {       
		Boolean jetzt= false;                //Erstellen der nötigen Hilfsvariablen
		Boolean sorte = false;               
		int count=1;
		int stelle=0;
		DynArray<Boolean> out = new DynArray<Boolean>();
		for (int i = 0; i < in.getLength(); i++) {  //je nach Stelle im DynArray hat der Bit eine andere Bedeutung
			jetzt=in.getItem(i);
			stelle++;
		if (stelle==2) {
				if (jetzt==true) {                      //an der Stelle gibt der Bit an, dass 2 weitere Bits im Originaltext der Sorte angehören
					count=count+2;
				} // end of if
			} else {
				if (stelle==3) {                        //an der Stelle gibt der Bit an, dass ein weiterer Bit im Originaltext der Sorte angehört 
					if (jetzt==true) {
						count++;
					} // end of if
				} else {
					if (stelle==4) {                      //an der ersten Stelle gibt der Bit ob die Bits der Folge im Originaltext auf "true" oder "false" gesetzt sind
					for (int j = 0; j < count; j++) {
							out.append(sorte);
						}
						count=1; //es ist immer mindestens 1 Bit in einer Folge
						stelle=1;
						sorte=jetzt;
					} else {                             
						sorte=jetzt;
					} // end of if-else
				} // end of if-else
			} // end of if-else    
		}  
		for (int j = 0; j < count; j++) {          //die letzte Folge wird nicht in der Schleife angehangen und wird daher nach ihr angehangen
			out.append(sorte);
		}
		return out;
	}

	public static DynArray<Boolean> LauflangeThreeDecode(DynArray<Boolean> in) {       
		Boolean jetzt= false;
		Boolean sorte = false;
		int count=1;
		int stelle=0;
		DynArray<Boolean> out = new DynArray<Boolean>();
		for (int i = 0; i < in.getLength(); i++) {
			jetzt=in.getItem(i);
			stelle++;
			if (stelle==2) {                          //an der Stelle gibt der Bit an, dass 4 weitere Bits im Originaltext der Sorte angehören
				if (jetzt==true) {
					count=count+4;
				} // end of if
			} else {
				if (stelle==3) {                         //an der Stelle gibt der Bit an, dass 2 weitere Bits im Originaltext der Sorte angehören
					if (jetzt==true) {
						count=count+2;
					} // end of if
				} else {
					if (stelle==4) {                       //an der Stelle gibt der Bit an, dass ein weiterer Bit im Originaltext der Sorte angehört
						if (jetzt==true) {
							count++;
						} // end of if
					} else {
						if (stelle==5) {                     //an der ersten Stelle gibt der Bit ob die Bits der Folge im Originaltext auf "true" oder "false" gesetzt sind
							for (int j = 0; j < count; j++) {
								out.append(sorte);
							}
							count=1; //es ist immer mindestens 1 Bit in einer Folge
							stelle=1;
							sorte=jetzt;
						} else {
							sorte=jetzt;
						} // end of if-else
					} // end of if-else
				} // end of if-else
			} // end of if-else    
		}  
		for (int j = 0; j < count; j++) {            //die letzte Folge wird nicht in der Schleife angehangen und wird daher nach ihr angehangen
			out.append(sorte);
		}
		return out;
	}

	public static DynArray<Boolean> LauflangeFourDecode(DynArray<Boolean> in) {       
		Boolean jetzt= false;
		Boolean sorte = false;
		int count=1;
		int stelle=0;
		DynArray<Boolean> out = new DynArray<Boolean>();
		for (int i = 0; i < in.getLength(); i++) {
			jetzt=in.getItem(i);
			stelle++;
			if (stelle==2) {                            //an der Stelle gibt der Bit an, dass 8 weitere Bits im Originaltext der Sorte angehören
				if (jetzt==true) {
					count=count+8;
				} // end of if
			} else {  
				if (stelle==3) {                          //an der Stelle gibt der Bit an, dass 4 weitere Bits im Originaltext der Sorte angehören
					if (jetzt==true) {
						count=count+4;
					} // end of if
				} else {
					if (stelle==4) {                        //an der Stelle gibt der Bit an, dass 2 weitere Bits im Originaltext der Sorte angehören
						if (jetzt==true) {
							count=count+2;
						} // end of if
					} else {
						if (stelle==5) {                      //an der Stelle gibt der Bit an, dass ein weiterer Bit im Originaltext der Sorte angehört
							if (jetzt==true) {
								count++;
							} // end of if
						} else {
							if (stelle==6) {                    //an der ersten Stelle gibt der Bit ob die Bits der Folge im Originaltext auf "true" oder "false" gesetzt sind
								for (int j = 0; j < count; j++) {
									out.append(sorte);
								}
								count=1; //es ist immer mindestens 1 Bit in einer Folge
								stelle=1;
								sorte=jetzt;
							} else {
								sorte=jetzt;
							} // end of if-else
						} // end of if-else
					} // end of if-else
				} // end of if-else    
			}
		}    
		for (int j = 0; j < count; j++) {             //die letzte Folge wird nicht in der Schleife angehangen und wird daher nach ihr angehangen
			out.append(sorte);
		}
		return out;
	}
}
