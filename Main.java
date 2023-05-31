import java.io.*;

public class Main {

	public static int findInDynArray(String search_term, DynArray<String> dynarray){
		for (int i = 0; i < dynarray.getLength(); i++) {
			if(search_term.equals(dynarray.getItem(i))){
				return i;
			}
		}
		return -1;
	}

	// For Context on how LZW compression works see: https://www.youtube.com/watch?v=dLvvGXwKUGw
	public static DynArray<Integer> lzw_encode(DynArray<Integer> input){
		DynArray<String> dictionary = new DynArray<String>();
		DynArray<Integer> output = new DynArray<Integer>();
		String previous_word = new String(""),
		       current_word = new String("");
		boolean dublicate = false;

		previous_word = String.valueOf(input.getItem(0));
		for(int i=1; i < input.getLength(); i++){
			dublicate = false;
			current_word = String.valueOf(input.getItem(i));
			if(findInDynArray(previous_word + current_word, dictionary) != -1){
				dublicate = true;
			}
			if(dublicate){
				previous_word = (previous_word + current_word);
			} else {
				dictionary.append(previous_word+current_word);
				previous_word = "" + current_word;
				int encoding_for_prev_word = findInDynArray(previous_word, dictionary);
				if(encoding_for_prev_word == -1){
					encoding_for_prev_word = (int)previous_word.indexOf(0);
				}
				output.append(encoding_for_prev_word);
			}
		}
		int encoding_for_prev_word = findInDynArray(previous_word, dictionary);
		if(encoding_for_prev_word == -1){
			encoding_for_prev_word = (int)previous_word.indexOf(0);
		}
		output.append(encoding_for_prev_word);

		return output;
	}

	public static DynArray<Boolean> appendToBooleanArray(DynArray<Boolean> orig, Boolean[] appends){
		for(int i=0; i < appends.length; i++){
			orig.append(appends[i]);
		}
		return orig;
	}

	public static Boolean[] convertToBoolean(char input){
		Boolean [] output = new Boolean[7];
		for (int i = 0; i < 7 ; i++) {
			output[6-i] = (((input >> i) & 1) != 0);
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

	public static void main(String[] x){
		DynArray<Integer> data = readFile("file");
		output_data(data);
		data = lzw_encode(data);
		System.out.println("");
		output_data(data);
	}
}
