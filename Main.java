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

	public static DynArray<Boolean> encode(DynArray<Integer> in){
		DynArray<Boolean> out = new DynArray<Boolean>();
		while (true){
			DynArray<Boolean> lzw_encoded = lzw_encode(in);
			DynArray<Boolean> ll_encoded2 = LauflangeTwo(dynIntToBool(in));
			DynArray<Boolean> ll_encoded3 = LauflangeThree(dynIntToBool(in));
			DynArray<Boolean> ll_encoded4 = LauflangeFour(dynIntToBool(in));
			DynArray<Boolean> huffman_encoded = huffman_encode(in);

			int lzw_encoded_len = lzw_encoded.getLength();
			int ll_encoded2_len = ll_encoded2.getLength();
			int ll_encoded3_len = ll_encoded3.getLength();
			int ll_encoded4_len = ll_encoded4.getLength();
			int huffman_encoded_len = huffman_encoded.getLength();

			if(lzw_encoded_len >= ll_encoded2_len &&
			   lzw_encoded_len >= ll_encoded3_len &&
			   lzw_encoded_len >= ll_encoded4_len &&
			   lzw_encoded_len >= huffman_encoded_len){
				out = new DynArray<Boolean>();
				appendToBooleanArray(out, convertToBoolean(ALGORITHM_ENCODING_BIT_LEN, ALGORITHM_ENCODING_LZW));
				

			}
			break;
		}
		return out;
	}

	public static void main(String[] x){

		DynArray<Integer> data = readFile("file");
		DynArray<Boolean> databol = lzw_encode(data);
		System.out.println(data.getLength()*7);

		//output_data_bol(lauflaenge);
		System.out.println("");
		output_data_bol(databol);
		System.out.println();

		System.out.println(databol.getLength());
		System.out.println(data.getLength() * 7);
		System.out.println((double)databol.getLength()/(data.getLength()*7));
		System.out.println();
		DynArray<Integer> data_later = lzw_decode(databol);
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
	}


	//Codierung
	public static DynArray<Boolean> LauflangeTwo(DynArray<Boolean> in) {       
		Boolean letztesW = false;
		Boolean aktuellesW = false;
		DynArray<Boolean> komp = new DynArray<Boolean>();
		int count=1;
		letztesW=in.getItem(0);
		for (int i = 1; i < in.getLength(); i++) {
			aktuellesW=in.getItem(i);
			if (aktuellesW==letztesW) {
				count++;
				if (count>4) {
					komp.append(letztesW);
					appendToBooleanArray(komp,convertToBoolean(2,3));
					count=1;
				} // end of if
			} else {
				komp.append(letztesW);
				appendToBooleanArray(komp,convertToBoolean(2,count-1));
				count=1;
				letztesW=aktuellesW;
			} // end of if-else
		}
		komp.append(letztesW);
		appendToBooleanArray(komp,convertToBoolean(2,count-1));
		return komp;
	}

	public static DynArray<Boolean> LauflangeThree(DynArray<Boolean> in) {       
		Boolean letztesW = false;
		Boolean aktuellesW = false;
		DynArray<Boolean> komp = new DynArray<Boolean>();
		int count=1;
		letztesW=in.getItem(0);
		for (int i = 1; i < in.getLength(); i++) {
			aktuellesW=in.getItem(i);
			if (aktuellesW==letztesW) {
				count++;
				if (count>8) {
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
		Boolean letztesW = false;
		Boolean aktuellesW = false;
		DynArray<Boolean> komp = new DynArray<Boolean>();
		int count=1;
		letztesW=in.getItem(0);
		for (int i = 1; i < in.getLength(); i++) {
			aktuellesW=in.getItem(i);
			if (aktuellesW==letztesW) {
				count++;
				if (count>16) {
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
		Boolean jetzt= false;
		Boolean sorte = false;
		int count=1;
		int stelle=0;
		DynArray<Boolean> out = new DynArray<Boolean>();
		for (int i = 0; i < in.getLength(); i++) {
			jetzt=in.getItem(i);
			stelle++;
			if (stelle==2) {
				if (jetzt==true) {
					count=count+2;
				} // end of if
			} else {
				if (stelle==3) {
					if (jetzt==true) {
						count++;
					} // end of if
				} else {
					if (stelle==4) {
						for (int j = 0; j < count; j++) {
							out.append(sorte);
						}
						count=1;
						stelle=1;
						sorte=jetzt;
					} else {
						sorte=jetzt;
					} // end of if-else
				} // end of if-else
			} // end of if-else    
		}  
		for (int j = 0; j < count; j++) {
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
			if (stelle==2) {
				if (jetzt==true) {
					count=count+4;
				} // end of if
			} else {
				if (stelle==3) {
					if (jetzt==true) {
						count=count+2;
					} // end of if
				} else {
					if (stelle==4) {
						if (jetzt==true) {
							count++;
						} // end of if
					} else {
						if (stelle==5) {
							for (int j = 0; j < count; j++) {
								out.append(sorte);
							}
							count=1;
							stelle=1;
							sorte=jetzt;
						} else {
							sorte=jetzt;
						} // end of if-else
					} // end of if-else
				} // end of if-else
			} // end of if-else    
		}  
		for (int j = 0; j < count; j++) {
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
			if (stelle==2) {
				if (jetzt==true) {
					count=count+8;
				} // end of if
			} else {  
				if (stelle==3) {
					if (jetzt==true) {
						count=count+4;
					} // end of if
				} else {
					if (stelle==4) {
						if (jetzt==true) {
							count=count+2;
						} // end of if
					} else {
						if (stelle==5) {
							if (jetzt==true) {
								count++;
							} // end of if
						} else {
							if (stelle==6) {
								for (int j = 0; j < count; j++) {
									out.append(sorte);
								}
								count=1;
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
		for (int j = 0; j < count; j++) {
			out.append(sorte);
		}
		return out;
	}
}
