import java.io.*;


public class Main {

	private static final int ASCII_MAX = 127;
	private static final int MAX_LZW_POINTER_BITS = 4;
	private static final int ASCII_BIT_COUNT = 7;


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
		DynArray<Integer> output = new DynArray<Integer>();
		String previous_word = new String(""),
		       current_word = new String("");
		boolean dublicate = false;
		// This is needed for converting to booleans. This defines the amount
		// of Bits used for "pointers"
		int highest_used_dictionary = 0;

		previous_word = String.valueOf((char)(int)input.getItem(0));
		for(int i=1; i < input.getLength(); i++){
			dublicate = true;
			current_word = String.valueOf((char)(int)input.getItem(i));
			if(findInDynArray(previous_word + current_word, dictionary) == -1){
				dublicate = false;
			}
			if(dublicate){
				previous_word = (previous_word + current_word);
			} else {
				dictionary.append(previous_word+current_word);
				int encoding_for_prev_word = ASCII_MAX + findInDynArray(previous_word, dictionary) + 1;
				if(highest_used_dictionary < encoding_for_prev_word - (ASCII_MAX+1)){
					highest_used_dictionary = encoding_for_prev_word - (ASCII_MAX+1);
				}
				if(encoding_for_prev_word == ASCII_MAX){
					encoding_for_prev_word = previous_word.charAt(0);
				}
				output.append(encoding_for_prev_word);
				previous_word = "" + current_word;
			}
		}
		int encoding_for_prev_word = ASCII_MAX + findInDynArray(previous_word, dictionary) + 1;
		if(highest_used_dictionary < encoding_for_prev_word - (ASCII_MAX+1)){
			highest_used_dictionary = encoding_for_prev_word - (ASCII_MAX+1);
		}
		if(encoding_for_prev_word == ASCII_MAX){
			encoding_for_prev_word = previous_word.charAt(0);
		}
		output.append(encoding_for_prev_word);

		System.out.println(highest_used_dictionary);
		highest_used_dictionary = bitLength(highest_used_dictionary);
		System.out.println(highest_used_dictionary);
		DynArray<Boolean> out2 = new DynArray<Boolean>();
		appendToBooleanArray(out2, convertToBoolean(MAX_LZW_POINTER_BITS, highest_used_dictionary-1));
		for (int i = 0; i < output.getLength(); i++) {
			if(output.getItem(i) > ASCII_MAX){
				out2.append(true);
				appendToBooleanArray(out2, convertToBoolean(highest_used_dictionary, output.getItem(i)-ASCII_MAX));
			} else {
				out2.append(false);
				appendToBooleanArray(out2, convertToBoolean(ASCII_BIT_COUNT, output.getItem(i)));
			}
		}
		return out2;
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

	public static char convertToChar(Boolean[] input){
		int output = 0;
		for (int i=0; i < 7; i++){
			if(input[i])
				output += 1 << (6-i);
		}
		return (char)output;
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

	public static void main(String[] x){
		DynArray<Integer> data = readFile("file");
		output_data(data);
		DynArray<Boolean> databol = lzw_encode(data);
		System.out.println("");
		output_data_bol(databol);
	}
}
