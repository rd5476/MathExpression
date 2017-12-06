//package TrueBox;

import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class Main {

    static String Inpfilename;
    static String OutDir;



    public static void main(String args[]) throws IOException {
        String inpfile="";
        String option="";
        String dicPath="";
        String unicodePath="";
        boolean displayFlag=false;
        boolean printFlag=false;
        HashMap<String, Integer> wordDictionary =  new HashMap<String,Integer>();
        HashMap<Character,String> unicodeDictionary =  new HashMap<Character, String>();
        //InputFile and OutputDir
        if(args[0].equals("-n") && args.length<=5){
            option=args[0];
            inpfile = args[1];
            unicodePath = args[2];
            OutDir = args[3];
            if(args.length==5 && args[4].equals("-d")){
                displayFlag=true;
            }
            if(args.length==5 && args[4].equals("-p")){
                printFlag=true;
            }

        }else if(args[0].equals("-f") && args.length<=5) {
            option=args[0];
            inpfile = args[1];
            unicodePath =args[2];
            OutDir = args[3];
            dicPath=args[4];
            if(args.length==6 && args[5].equals("-d")){
                displayFlag=true;
            }
            if(args.length==6 && args[5].equals("-p")){
                printFlag=true;
            }
        }else{
            System.out.println("*************Usage*************");
            System.out.println("Please follow the instructions ");
            System.out.println("java Main -n <input file name> <Output Directory> [-d]");
            System.out.println("-n for all text with bounding box");
            System.out.println("           OR            ");
            System.out.println("java Main -f <input file name> <Output Directory> <word Dictionary> [-d]");
            System.out.println("-f for Non-filtered text");
            System.out.println("[-d] Optional field to display pdf content on Console");
            System.exit(0);

        }

        String [] temp = inpfile.split("\\\\");
        Inpfilename = temp[temp.length-1].split("\\.")[0];

        //Load File
        File file = new File(inpfile);
        FileInputStream inpStream = new FileInputStream(file);
        PDDocument documnet = PDDocument.load(inpStream);

        //Read unicode File

        File unicodeFile =  new File(unicodePath);
        Scanner scan =  new Scanner(unicodeFile);

        while(scan.hasNext()){
            String uni = scan.nextLine();
            String unicodeDetails []= uni.split(";");
            if(unicodeDetails[2].equals("Ps") || unicodeDetails[2].equals("Sm")){
                String unicode  = "\\u"+unicodeDetails[0];
                Character uniChar = (char)Integer.parseInt(unicode.substring(2),16);
                unicodeDictionary.put(uniChar,unicodeDetails[1]);
            }
        }

        System.out.println("Unicode Dictionary size::"+unicodeDictionary.size());

        //Readfile
        read reader = new read(documnet,unicodeDictionary);
        ArrayList<PageStructure> allPages =reader.readPdf();


        //DisplayPDF
        if(displayFlag) {
            DisplayPDF display = new DisplayPDF(allPages);
            display.displayPDF();
            System.out.println(display.xmlFormat);

        }
        //Print flag
        if(printFlag) {

            DisplayPDF display = new DisplayPDF(allPages);
            display.displayPDF();
            //System.out.println(display.xmlFormat);
            try {
                File newFile = new File("XMLTest.txt");
                PrintWriter writer = new PrintWriter(newFile);
                writer.write(display.builder.toString());
                writer.flush();
                writer.close();
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }

        //drawBoundingBox
        drawBBOX box = new drawBBOX(documnet, allPages);
        if(option.equals("-n")) {
            box.drawPDF(doctype.Normal);
            System.out.println();
            System.out.println("Output files generated...");
        }


        //Filter text
        //Load Dictionary
        if(option.equals("-f")) {
            File dicfile = new File(dicPath);
            wordDictionary = loadDictionary(dicfile);
            FilterText filterText = new FilterText(wordDictionary);
            HashMap<Integer, characterInfo> filtercharList;
            for (int pageIter = 0; pageIter < allPages.size(); pageIter++) {
                filtercharList = filterPage(filterText, pageIter, allPages);
                box.drawPageBBOX(pageIter, filtercharList,allPages.get(pageIter).pageCompundCharacters, doctype.Filtered);
            }
            System.out.println();
            System.out.println("Output files generated....");
        }
    }

    public static HashMap<String, Integer> loadDictionary(File file) throws FileNotFoundException {
        HashMap<String, Integer> wordDictionary =  new HashMap<String,Integer>();
        Scanner scan = new Scanner(new FileReader(file));
        while(scan.hasNext()){
            String word =scan.nextLine();
            word=word.trim().toLowerCase();
            wordDictionary.put(word,0);
        }

        return wordDictionary;
    }
    public static HashMap<Integer,characterInfo> filterPage(FilterText filterText,int page, ArrayList<PageStructure> allPages){
        HashMap<Integer,Words> filtered= filterText.filter(allPages.get(page));
        HashMap<Integer,characterInfo> charList =filterText.getCharacterList(filtered);
        return charList;
    }
}
