//package TrueBox;



import java.util.*;

class FilterText {

    HashMap<String, Integer> wordDictionary;

    FilterText(HashMap<String, Integer> wordDictionary){
        this.wordDictionary=wordDictionary;
    }
    

    public boolean matchWord(String word){
        return wordDictionary.containsKey(word.toLowerCase());
    }

    public HashMap<Integer,Words> filter(PageStructure page){

        HashMap<Integer, Words> filteredMap = new HashMap<Integer, Words>();

        ArrayList<Line> allLines = page.Lines;
        int matched=0;
        for(int lineIter=0;lineIter<allLines.size();lineIter++){
            ArrayList<Words> allWords = allLines.get(lineIter).words;
            //System.out.println("TW"+allWords.size());
            for(int wordIter=0;wordIter<allWords.size();wordIter++){
                ArrayList<characterInfo> characters = allWords.get(wordIter).characters;
                String word="";
                for(int charIter=0;charIter<characters.size();charIter++){
                    if(characters.get(charIter).value.equals(",") || characters.get(charIter).value.equals(".") )
                        continue;
                    word+=characters.get(charIter).value;
                }
                //System.out.println(word+" "+matchWord(word));
                if(matchWord(word.trim())){
                    //System.out.println(matched++);
                    matched++;
                    filteredMap.put(matched, allWords.get(wordIter));
                }
            }

        }
        //System.out.println("S:"+filteredMap.size());


        return filteredMap;

    }


    public HashMap<Integer,characterInfo> getCharacterList(HashMap<Integer,Words> filtered){
        //System.out.println("FT:"+filtered.size());
        Iterator iter = filtered.entrySet().iterator();
        HashMap<Integer,characterInfo> filteredCharacter = new HashMap<Integer, characterInfo>();
        int counter=0;
        while(iter.hasNext()){
            Map.Entry pair = (Map.Entry) iter.next();
            Words word =(Words) pair.getValue();
            for(characterInfo ch: word.characters){
                counter++;
                //System.out.println(ch.value);
                filteredCharacter.put(counter,ch);
            }
        }

        //System.out.println("FC:"+filteredCharacter.size());
        return filteredCharacter;

    }
}
