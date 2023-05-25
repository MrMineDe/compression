import java.io.*;  

public class Main {

	public static String lzw_encode(String input){
		DynArray<String> dictionary = new DynArray<String>();
		String output = new String(""),
		       previous_word = new String(""),
		       current_word = new String("");
		boolean dublicate = false;

		for(int i=0; i < input.length(); i++){
			current_word = String.valueOf(input.charAt(i));
			for(int j=0; j < dictionary.getLength(); j++){
				if((previous_word+current_word) == dictionary.getItem(i)){
					dublicate = true;
					break;
				}
			}
			if(!dublicate){
				dictionary.append((previous_word)+current_word);
				previous_word = "" + current_word;
				output = (output + previous_word);
			} else {
				previous_word = (previous_word + current_word);
			}
		}


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

	public static DynArray<Boolean> readFile(String filepath){
		File file = new File(filepath);
		DynArray<Boolean> data = new DynArray<Boolean>();
		if(!file.exists()){
			System.out.println("Error opening file. Does it exist? Do you have permission?");
			return null;
		}
		try{
			FileInputStream fis= new FileInputStream(file);
			int r = 0;

			while((r = fis.read()) != -1){
				appendToBooleanArray(data, convertToBoolean((char)r));
			}
			fis.close();
		} catch ( Exception IOException) {
			System.out.println("Error reading!");
		} 
		return data;
	}

	public static void main(String[] x){
		readFile("file");
	}
}
