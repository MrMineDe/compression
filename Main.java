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
	private static final int ALGORITHM_ENCODING_komMix = 7;
	//Wie viel? Lohnt sich dynamisch?
	private static final int XYZ_X_ENCODING_BIT_LEN = 7;
	private static final int XYZ_Y_ENCODING_BIT_LEN = 3;
	private static final int XYZ_Z_ENCODING_BIT_LEN = 22;
	private static final int XYZ_ENCODING_REST_BIT_LEN = 3;


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

		//System.out.println(bitsForPointers);
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
		if(num < 0){
			num = num * -1;
		}
		int bitlength = 1;
		while(Math.pow(2 , bitlength) < num){
			bitlength++;
		}
		return bitlength;
	}

	public static int bitLength(long num){
		if(num < 0){
			num = num * -1;
		}
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

	public static Boolean[] convertToBoolean(int boolean_array_length, long input){
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

	public static long convertToLong(DynArray<Boolean> input, int index_min, int index_max){
		long output = 0;
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

	public static DynArray<Integer> dynBoolToIntNoCheck(DynArray<Boolean> in){
		DynArray<Integer> out = new DynArray<Integer>();

		for(int i=0; i < in.getLength()/7; i++){
			out.append(convertToInt(in, i*7, i*7+6));
		}
		//output_data(out);
		return out;
	}

	public static DynArray<Boolean> xyz_encode(DynArray<Boolean> in){
		DynArray<Boolean> out = new DynArray<Boolean>();
		appendToBooleanArray(out, convertToBoolean(XYZ_ENCODING_REST_BIT_LEN, in.getLength()%28));
		for(int i=0; i < in.getLength()/28;){
			long zahl;
			zahl = convertToLong(in, i*28, i*28 + 27);
			i+=28;

			int besteX = 0;
			int besteY = 0;
			long besteZ = 0;
			for (int x = 2; bitLength(x-2) <= XYZ_X_ENCODING_BIT_LEN; x++) {
				for (int y = 2; bitLength(y-2) <= XYZ_Y_ENCODING_BIT_LEN; y++) {
					long z = zahl - (long)Math.pow(x, y);  //zugehöriges z wird errechnet

					if (bitLength(z) <= XYZ_Z_ENCODING_BIT_LEN && 
					    zahl*2 > (long)Math.pow(x, y)) {    //falls z>0 und 
						besteX = x;
						besteY = y;
						besteZ = z;
					}
				}
			}

			out = appendToBooleanArray(out, convertToBoolean(XYZ_X_ENCODING_BIT_LEN, besteX-2));
			out = appendToBooleanArray(out, convertToBoolean(XYZ_Y_ENCODING_BIT_LEN, besteY-2));
			out = appendToBooleanArray(out, convertToBoolean(XYZ_Z_ENCODING_BIT_LEN, besteZ));
		}
		for(int i=28*(in.getLength()/28); i < in.getLength(); i++){
			out.append(in.getItem(i));
		}

		return out;
	}
	
	public static DynArray<Integer> xyz_decode(DynArray<Boolean> in){
		DynArray<Integer> out = new DynArray<Integer>();
		int i=0;
		for(i=0; i < in.getLength()/XYZ_X_ENCODING_BIT_LEN+XYZ_Y_ENCODING_BIT_LEN+XYZ_Z_ENCODING_BIT_LEN;){
			int x = convertToInt(in, i, i+XYZ_X_ENCODING_BIT_LEN-1);
			i += XYZ_X_ENCODING_BIT_LEN;
			int y = convertToInt(in, i, i+XYZ_Y_ENCODING_BIT_LEN-1);
			i += XYZ_Y_ENCODING_BIT_LEN;
			int z = convertToInt(in, i, i+XYZ_Z_ENCODING_BIT_LEN-1);
			i += XYZ_Z_ENCODING_BIT_LEN;
			out.append((int)Math.pow(x, y)+z);
		}
		while(i < in.getLength()){
			out.append(convertToInt(in, i, i+6));
			i+=7;
		}
		return out;
	}



	public static DynArray<Boolean> encode(DynArray<Integer> in){
		DynArray<Boolean> out = dynIntToBool(in);
		for(int i=0; i < 3; i++){
			out.insertAt(0, false);
		}	

		int outlen = out.getLength();
		out.insertAt(0, true);
		for(int i=0; i < (ASCII_BIT_COUNT-1) - outlen % ASCII_BIT_COUNT; i++){
			out.insertAt(i, false);
		}
		DynArray<Boolean> orig = new DynArray<Boolean>();

		int countMixerAfterEachOther = 0;
		// Stop encoding when you only done mixer functions 3 times in a row
		while (countMixerAfterEachOther < 3){
			DynArray<Boolean> lzw_encoded = lzw_encode(dynBoolToIntNoCheck(out));
			DynArray<Boolean> ll_encoded2 = LauflangeTwo(out);
			DynArray<Boolean> ll_encoded3 = LauflangeThree(out);
			DynArray<Boolean> ll_encoded4 = LauflangeFour(out);
			DynArray<Boolean> huffman_encoded = kompressionHuff(dynBoolToIntNoCheck(out));

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
				// Save the text before throwing it through the mixer. If no method is useful after mixing it is smarter
				// to just take the orig text before mixing, because mixing always adds a few bits at least
				if(countMixerAfterEachOther == 0){
					orig = new DynArray<Boolean>();
					for(int i=0; i < out.getLength(); i++){
						orig.append(out.getItem(i));
					}
				}

				//DynArray<Boolean> xyz_encoded = xyz_encode(out);
				DynArray<Boolean> komMix_encoded = komMix_encode(out);

				/*
				if(countMixerAfterEachOther % 2 != 0){
					out = new DynArray<Boolean>();
					appendToBooleanArray(out, convertToBoolean(ALGORITHM_ENCODING_BIT_LEN, ALGORITHM_ENCODING_XYZ));
					for(int i=0; i < xyz_encoded.getLength(); i++){
						out.append(xyz_encoded.getItem(i));
					}
				} else {
				*/
					out = new DynArray<Boolean>();
					// This is to round up so the out array is dividable through 7. This is needed for convertion from Boolean to int
					for(int i=0; i < (ASCII_BIT_COUNT-1) - (komMix_encoded.getLength()+ALGORITHM_ENCODING_BIT_LEN) % ASCII_BIT_COUNT; i++){
						out.append(false);
					}
					out.append(true);

					appendToBooleanArray(out, convertToBoolean(ALGORITHM_ENCODING_BIT_LEN, ALGORITHM_ENCODING_komMix));
					for(int i=0; i < komMix_encoded.getLength(); i++){
						out.append(komMix_encoded.getItem(i));
					}
				//}
				countMixerAfterEachOther++;
			}
		}

		return orig;
	}

	public static DynArray<Boolean> decode(DynArray<Boolean> in){
		DynArray<Boolean> out = in;
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
				out = dynIntToBool(dekompressionHuff(out));
				break;
			
			case ALGORITHM_ENCODING_XYZ:
				out = dynIntToBool(xyz_decode(out));
				break;
			case ALGORITHM_ENCODING_komMix:
				out = komMix_decode(out);
				break;
			}

		}
		return out;
	}

	public static DynArray<Boolean> komMix_encode(DynArray<Boolean> in){

		DynArray<Boolean> output = new DynArray<Boolean>();
		//die Bits, die mit einander getauscht werden sind ausgehend der Häufigkeit der Zeichen aus der Ascii-Tabelle entschieden worden
		for (int i=0; i<in.getLength()/16; i++) {
			output.append(in.getItem(i*16));
			output.append(in.getItem(i*16+4));
			output.append(in.getItem(i*16+3));
			output.append(in.getItem(i*16+6));
			output.append(in.getItem(i*16+7));
			output.append(in.getItem(i*16+5));
			output.append(in.getItem(i*16+2));
			output.append(in.getItem(i*16+1));
			output.append(in.getItem(i*16+1+8));
			output.append(in.getItem(i*16+2+8));
			output.append(in.getItem(i*16+5+8));
			output.append(in.getItem(i*16+7+8));
			output.append(in.getItem(i*16+6+8));
			output.append(in.getItem(i*16+3+8));
			output.append(in.getItem(i*16+4+8));
			output.append(in.getItem(i*16+8));
		}
		//überschüssige Bits werden nicht gemixt und einfach übertragen
		for (int i=(in.getLength()/16)*16; i<in.getLength(); i++) {
			output.append(in.getItem(i));
		}
		return output;
	}

	public static DynArray<Boolean> komMix_decode(DynArray<Boolean> in){
		DynArray<Boolean> output = new DynArray<Boolean>(); 
		//die Bits werden zurückgetauscht
		for (int i=0; i<in.getLength()/16; i++) {
			output.append(in.getItem(i*16));
			output.append(in.getItem(i*16+7));
			output.append(in.getItem(i*16+6));
			output.append(in.getItem(i*16+2));
			output.append(in.getItem(i*16+1));
			output.append(in.getItem(i*16+5));
			output.append(in.getItem(i*16+3));
			output.append(in.getItem(i*16+4));
			output.append(in.getItem(i*16+7+8));
			output.append(in.getItem(i*16+8));
			output.append(in.getItem(i*16+1+8));
			output.append(in.getItem(i*16+5+8));
			output.append(in.getItem(i*16+6+8));
			output.append(in.getItem(i*16+2+8));
			output.append(in.getItem(i*16+4+8));
			output.append(in.getItem(i*16+3+8));
		}
		//überschüssige Bits werden nicht gemixt und einfach übertragen
		for (int i=(in.getLength()/16)*16; i<in.getLength(); i++) {
			output.append(in.getItem(i));
		}
		return output;
	}

	public static void writeFile(DynArray <Integer> ein, String outputFile){
        int[] neuEin=new int[ein.getLength()];
        for(int i=0;i<ein.getLength();i++){
            neuEin[i]=ein.getItem(i);
        }
        char[] charEin = new char[neuEin.length];
        for(int i=0;i<neuEin.length;i++){
            charEin[i]=(char) neuEin[i];
        }
        String ausgabe="";
        for(int i=0;i<charEin.length;i++){
            ausgabe = ausgabe + charEin[i];
        }
        try{
        PrintWriter writer = new PrintWriter(new FileOutputStream(outputFile, false));
        writer.println(ausgabe);
        writer.close();
        }
        catch (IOException e){
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

	public static void main(String[] x){

		DynArray<Integer> data = readFile("input");
		DynArray<Boolean> encoded = encode(data);
		DynArray<Integer> encoded_i = dynBoolToIntNoCheck(encoded);

		writeFile(encoded_i, "output_encoded");
		//DynArray<Boolean> encoded_read = dynIntToBool(readFile("output"));
		System.out.println();
		System.out.println(encoded_i.getLength()*7);
		System.out.println(data.getLength()*7);
		System.out.println((double)encoded_i.getLength()/data.getLength());

		DynArray<Boolean> decoded = decode(encoded);
		DynArray<Integer> decoded_i = dynBoolToIntNoCheck(decoded);
		
		writeFile(decoded_i, "output_decoded");


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

	public static DynArray <BinTree <Integer[]>> binar = new DynArray <BinTree <Integer[]> >();
	public static DynArray <Integer> codetab = new DynArray <Integer>();
	public static BinTree <Integer[]> enc1 = new BinTree <Integer[]>();
	public static BinTree <Integer[]> enc2 = new BinTree <Integer[]>();
	public static int sTc=0;
	public static int[] vorlage = new int[128];
	public static Boolean [] [] kompZ = new Boolean [128] [128];
	public static DynArray <Boolean> kompTemp = new DynArray <Boolean>();
	public static DynArray <Boolean> out = new DynArray <Boolean>();
	public static DynArray <Integer> out2 = new DynArray <Integer>();
	public static DynArray <Boolean> tempB= new DynArray <Boolean>();

	public static DynArray <Boolean> kompressionHuff(DynArray <Integer> einH){
		// Hauptfunktion zur kompression mit aufruf aller methoden in reihenfolge zur kompression; eingabe ist ein DynArray <integer> der in ein DynArray <Boolean> komprimiert wird; aufruf in kompressionHuff zur uebersicht
		for(int i=0;i<tempB.getLength();i++){
			tempB.delete(0);
		}
		for (int i=0; i<vorlage.length;i++){
			vorlage[i]=i;
		}  
		for(int z=0;z<kompZ.length;z++){
			for(int i=0;i<kompZ.length;i++){
				kompZ[z][i]=null;
			}
		}
		enc1.deleteLeft();
		enc1.deleteRight();  
		Integer[] wahrsch = new Integer[128];
		wahrsch = erstelleHaeufigkeit(einH);
		erstelleDynArray(wahrsch);
		sTc=0;
		createTree(codetab, enc1);
		// ermitteln der tabelle in 2 arrays mit dem bintree; komprimierung; entkomprimierung(ermitteln des trees erneut mit createT, dann entschlüsseln)
		createTab(enc1);
		komp(kompZ, einH);
		verbindeKomp(codetab, out);
		return tempB;
	}

	public static DynArray <Integer> huffmanDekomp(DynArray <Boolean> eingDK){
		// Zwischenfunktion zur dekompression; fuehrt alles methoden in richtiger reihenfolge zur dekompression aus und gibt den unkomprimierten text als DynArray <Integer> aus
		enc2.deleteLeft();
		enc2.deleteRight(); 
		for(int i=0;i<tempB.getLength();i++){
			tempB.delete(0);
		}
		for (int i=0; i<vorlage.length;i++){
			vorlage[i]=i;
		} 
		for(int z=0;z<kompZ.length;z++){
			for(int i=0;i<kompZ.length;i++){
				kompZ[z][i]=null;
			}
		}
		DynArray <Boolean> tempkurz = new DynArray <Boolean> ();
		for(int i=0;i<12;i++){
			tempkurz.append(eingDK.getItem(i));    
		}
		int laenge = convertToInt(tempkurz, 0, 11 );
		DynArray <Boolean> tempUS = new DynArray <Boolean>();
		for(int i=12;i<12+laenge;i++){
			tempUS.append(eingDK.getItem(i));
		}
		DynArray <Boolean> tempTxt = new DynArray <Boolean>();
		for(int i=12+laenge;i<eingDK.getLength();i++){
			tempTxt.append(eingDK.getItem(i));
		}
		DynArray <Integer> usFinal = new DynArray <Integer>();
		for(int i=0;i<tempUS.getLength();i++){
			if(tempUS.getItem(i)==false){
				usFinal.append(0);
			}
			if(tempUS.getItem(i)==true){
				usFinal.append(1);
				DynArray <Boolean> tempUS2 = new DynArray <Boolean>();
				for(int j=1;j<8;j++){
					tempUS2.append(tempUS.getItem(i+j));
				}
				usFinal.append(convertToInt(tempUS2, 0, 6));
				i=i+7;
			}
		}
		sTc=0;
		createTree(usFinal,enc2);
		createTab(enc2);
		Boolean[] bol1 = new Boolean [tempTxt.getLength()];
		for(int i=0; i<tempTxt.getLength();i++){
			bol1[i]=tempTxt.getItem(i);
		}
		dekomp(kompZ, bol1);
		return out2;
	}

	public static Integer[] erstelleHaeufigkeit(DynArray <Integer> a){
		//es wird aus dem array a ein hauefigkeitsarray hauf erstellt. Das hauf-array hat die laenge der ascii-codierung und beschreibt wie oft die jeweiligen ascii-zeichen vorkommen
		Integer[] hauf= new Integer[128];
		for(int i=0;i<hauf.length;i++){
			hauf[i]=0;
		}
		for(int i=0;i<a.getLength();i++){
			hauf[a.getItem(i)]++ ;
		}
		return hauf;
	}

	public static void erstelleDynArray(Integer[] hauf){
		// der DynArray binar wird mit Binaerbaeumen gefuellt. Diese besitzten als Inhalt einen Array a1 der die Hauefigkeit und den Ascii-Index der Zeichen enthaelt
		for(int i=0; i<hauf.length;i++){
			BinTree <Integer[]> b1=new BinTree <Integer[]>();
			Integer[] a1=new Integer[2];
			a1[0]=hauf[i];
			a1[1]=i;
			b1.setItem(a1);
			binar.append(b1);
		} 
		//rekursivBaum fuegt die beiden Binaerbaume mit den geringsten Hauefigkeiten an einer Wurzel zusammen und fuegt diese Wurzel (mit der gesamten Hauefigkeit der Teilbauem als Inhalt) in den DynArray ein und loescht die vorherigen beiden Binaerbaeume aus dem DynArray
		rekursivBaum(binar);
//searchT schreibt den Binaerbaum in einem gewaehlten Format in den DynArray codetab. Es wird eine 0 eingefuegt wenn es sich um eine Wurzel handelt, eine 1 wenn es sich um ein Blatt handelt und nach der 1 folgt der Ascii-Wert.
		searchT(binar.getItem(binar.getLength()-1));
		sTc=0;
	}

	public static void searchT(BinTree <Integer[]> b){
		// wenn der Teilbaum ein Blatt ist, endet die Methode und fuegt eine 1 und daraufhin den ascii-wert in codetab ein 
		if(b.isLeaf()){
			codetab.append(1);
			codetab.append(b.getItem()[1]);
		}
		else {
			//codetab erhaehlt eine 0    
			codetab.append(0);
			//rekursiver Aufruf nach pre-order (w-l-r)
			if(b.hasLeft()){
				searchT(b.getLeft());
			}
			codetab.append(0);
			if(b.hasRight()){
				searchT(b.getRight());
			}
}
}

	public static void rekursivBaum(DynArray < BinTree <Integer[]> > ein){
		//min und min2 müssen auf den max-indez-wert gesetzt werden
		int max=0;
		for(int i=0;i<ein.getLength();i++){
			if(ein.getItem(max).getItem()[0]<ein.getItem(i).getItem()[0]){
				max=i;
			}
		}
		int min=max;
		int min2=max;
		for(int i=0;i<ein.getLength();i++){
			if(ein.getItem(min).getItem()[0] >= ein.getItem(i).getItem()[0] && ein.getItem(i).getItem()[0] != 0){
				min=i;
			}
		}
		for(int i=0; i<ein.getLength();i++){
			if(ein.getItem(min2).getItem()[0] >= ein.getItem(i).getItem()[0] && i != min && ein.getItem(i).getItem()[0] != 0){
				min2=i;
			}
}
//bis hierhin wird das Minimum und 2. Minimum an Hauefigkeiten gesucht und die Indizes in min und min2 gespeichert 
		BinTree <Integer[]> b2 = new BinTree <Integer[]>();
		Integer[] a2= new Integer[1];
if(ein.getItem(min).getItem()[0] == 0 || ein.getItem(min2).getItem()[0] == 0 || min==min2){
			//wenn min oder min2 0 sind endet die Rekursion (Abbruchsbedingung)
			binar=ein;
			return;
		}
		//die beiden Teilbaume werden zusammengefuegt, die neue gesamthauefigkeit wird ermittelt und die originale werden aus binar geloescht
		else {
			a2[0] = ein.getItem(min).getItem()[0] + ein.getItem(min2).getItem()[0];
			b2.setItem(a2);
			b2.setLeft(ein.getItem(min2));
			b2.setRight(ein.getItem(min));
			ein.getItem(min).getItem()[0]=0;
			ein.getItem(min2).getItem()[0]=0;
			ein.append(b2);
		}  
		//rekursiver Aufruf bis nur noch eine Wurzel in binar vorhanden ist
		rekursivBaum(ein); 
	}

	public static void createTree(DynArray <Integer> a1, BinTree <Integer[]> b3){
		// aus dem DynArray codetab wird der BinTree gebildet (bei 0 nach links und am ende der funktion ebenfalls nach rechts Teilbaeume schaffen , bei 1 beenden und ascii wert hinzufuegen)
		if(a1.getItem(sTc)==1){
			sTc++;
			Integer[] a3 = new Integer [2];
			a3[0]=0;
			a3[1]=a1.getItem(sTc);
			b3.setItem(a3);
			sTc++;
			return;
		} 
		if(a1.getItem(sTc)==0){ 
			if(!b3.hasLeft()){
				sTc++;
				BinTree <Integer[]> b4 = new BinTree <Integer[]>();
				b3.setLeft(b4); 
createTree(a1, b4);
}  
if(!b3.hasRight()){
				sTc++;
				BinTree <Integer[]> b4 = new BinTree <Integer[]>();
				b3.setRight(b4); 
				createTree(a1, b4);
			}  
		}
	}

	public static void createTab(BinTree <Integer[]> b5){
		// Hier wird die Huffman-Code-Tabelle in Form eines zweidimensionalen Boolean-Array [128] [128] erstellt; die index-werte bei [128] [k] geben das zeichen in ascii-code an
		if(b5.hasLeft()){
			kompTemp.append(false);
			createTab(b5.getLeft());
		}
		if (b5.isLeaf()){
	for(int i=0;i<kompTemp.getLength();i++){
	kompZ [b5.getItem()[1]] [i] = kompTemp.getItem(i);
}
		}
		if(b5.hasRight()){
	kompTemp.append(true);
			createTab(b5.getRight());
		}
		if(!kompTemp.isEmpty()){
			kompTemp.delete(kompTemp.getLength()-1);
		}
		return;
	}

	public static void komp(Boolean [] [] z1, DynArray <Integer> inp ){
		//aus der erstellten codetabelle(durch createTab) wird ein eingegebener dynarray mit integer-werten (inp) zu einem neuen, komprimierten dynarray out 
		for(int i=0; i < inp.getLength();i++){
			for(int j=0; j < 128;j++){
				if(z1[inp.getItem(i)] [j] != null){
					if(z1[inp.getItem(i)] [j] == true){
	out.append(true);
}
if(z1[inp.getItem(i)] [j] == false){
						out.append(false);
					}
}
			}
		}
	}

	public static void dekomp(Boolean [] [] tab1, Boolean[] ein2){
		//aus dem komprimierten input-array und der codetabelle (nach createTab) wird die dekomprimierte version erzeugt
		int index =0;
			while (ein2.length-1 > index){
			for(int i=0;i<128;i++){
				for(int j =0; j<128;j++){ 
					if(j==0 && tab1 [i] [j] == null){
						break;
					}
					if(tab1 [i] [j] == null){
						out2.append(i);
						index=index+j;
						break;
					}
					if(index+j > ein2.length-1){
						break;
}
					if(tab1 [i] [j] != ein2[index+j]){
						break;
					}
				}
			}
		}
	}

	public static void verbindeKomp(DynArray <Integer> uS, DynArray <Boolean> uKomp){
		// der output wird erstellt: zuerst 12 bits mit der laenge der huffman-codierung; dann die huffman-codierung; dann der komprimierte text
		Boolean [] tempb2 = new Boolean [7];
		Boolean [] tempb3 = new Boolean [12];
		for(int i=0; i<uS.getLength();i++){
			if(uS.getItem(i)==0){
				tempB.append(false);
			}
			if(uS.getItem(i)==1){
				tempB.append(true);
				i++;
				tempb2=convertToBoolean(7,uS.getItem(i));
				for(int j=0;j<tempb2.length;j++){
					tempB.append(tempb2[j]);
				}
			}
		}
		// 12 hier variabel (beschreibt die maximale moegliche laenge der huffman-codierung -> bei 12 bits darf die huffman-codierung maximal 4096 bits betragen
		tempb3=convertToBoolean(12, tempB.getLength());
		for(int i=0; i<tempb3.length;i++){
	tempB.insertAt(i, tempb3[i]);
		}
for(int i=0; i<uKomp.getLength();i++){
	tempB.append(uKomp.getItem(i));
		}
		//tempB jetzt fertig-komprimierter output
	}

	// mit den folgenden Hauptfunktionen kann komprimiert und dekomprimiert werden: DynArray <Boolean> = kompressionHuff(DynArray <Integer>); DynArray <Integer> = dekompressionHuff(DynArray <Boolean> ein)

	public static DynArray <Integer> dekompressionHuff(DynArray <Boolean> ein){
DynArray <Boolean> tempBcool = new DynArray <Boolean>();
for(int i=0;i<ein.getLength();i++){
	tempBcool.append(ein.getItem(i));
}
		return huffmanDekomp(tempBcool);
	}

}
