//package TrueBox;

import org.apache.commons.compress.utils.Charsets;
import org.apache.pdfbox.pdmodel.PDDocument;

import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

class BoundingBox extends PDFTextStripper {

    int pageNum;
    PageStructure currentPage;
    int charNumber = 0;
    //int match = 0;
    //int unmatch = 0;
    float prevBaseLineY=0;
    float prevBaseLineX=0;
    //Line lines;
    int lineId=0;
    int wordId=0;
    int charId=0;
    int mergeId=0;
    float lineStartX=0;
    float lineStartY=0;
    float lineEndX=0;
    float lineEndY=0;

    ArrayList<Words> WordList = new ArrayList<Words>();
    ArrayList<characterInfo> CharList = new ArrayList<characterInfo>();
    HashMap<Character,String> unicodeDictionary;
    HashMap<Integer,compundCharacter> mergeMap= new HashMap<Integer,compundCharacter>();
    compundCharacter previousCharacter=null;
    ArrayList<characterInfo> radical =  new ArrayList<characterInfo>() ;

    public BoundingBox(PDDocument doc, int pagenum, PageStructure page,HashMap<Character,String> unicodeDictionary) throws IOException {
        super();
        document = doc;
        this.pageNum = pagenum;
        this.currentPage = page;
        currentPage.pageCompundCharacters=mergeMap;
        this.unicodeDictionary=unicodeDictionary;
    }

    public void getGeometricInfo(int pagenum) throws IOException {
        BarDetection barD = new BarDetection(document.getPage(pagenum));
        currentPage.bars=barD.getAllBars();
        extract(pagenum);
        getBoundingBox();
        includeBars();

        //drawBBOX(this.currentPage.pageCharacters,"Page");
    }

    public void extract(int pageNum) throws IOException {
        this.setStartPage(pageNum + 1);
        this.setEndPage(pageNum + 1);
        this.pageNum = pageNum;
        Writer dummy = new OutputStreamWriter(new ByteArrayOutputStream());
        writeText(document, dummy);
        Line prevLine = currentPage.Lines.get(currentPage.Lines.size()-1);
        prevLine.baseLine = new baseLine(lineStartX,lineStartY,prevBaseLineX,prevBaseLineY);
        //currentPage.Lines.add(new Line(lineId,prevLine.baseLine,WordList));
        dummy.close();
    }

    @Override
    protected void writeString(String string, List<TextPosition> textPositions) throws IOException {
        boolean singleWord=false;

        String word[] = string.split(getWordSeparator());
        if(word.length==1){

            singleWord=true;
        }
        mergeId++;
//        for(String w:word){
//            if(w.equals("APA")){
//                System.out.println();
//            }
//        }
//        for(int i=0;i<word.length;i++){
//            System.out.print(word[i]+" ");
//        }
//        System.out.println(string);
        TextPosition textLine = textPositions.get(0);
        if(textLine.getTextMatrix().getTranslateY()!= prevBaseLineY){
            if (prevBaseLineY==0){
                WordList =  new ArrayList<Words>();
                currentPage.Lines.add(new Line(lineId,null,WordList));
            }else{
                Line prevLine = currentPage.Lines.get(currentPage.Lines.size()-1);
                prevLine.baseLine = new baseLine(lineStartX,lineStartY,prevBaseLineX,prevBaseLineY);
                WordList =  new ArrayList<Words>();
                currentPage.Lines.add(new Line(lineId,null,WordList));

            }

            lineId+=1;
            prevBaseLineY = textLine.getTextMatrix().getTranslateY();
            lineStartY = textLine.getTextMatrix().getTranslateY();
            lineStartX = textLine.getTextMatrix().getTranslateX();

        }

        for (int i=0;i<textPositions.size();i++) {
            //System.out.println(text.getX());
            TextPosition text = textPositions.get(i);

            //String lineseparator = System.getProperty("line.separator");

            if(text.getUnicode().equals(getWordSeparator()) || text.getUnicode().equals("\uF020") ){
                WordList.add(new Words(wordId,CharList));
                wordId++;
                CharList= new ArrayList<characterInfo>();
                continue;
            }

            int merge=0;
            String value = text.getUnicode();
            //System.out.println(value.equals("√"));
            /*
            Character uniChar= (char)text.getUnicode().toCharArray()[0];

            if(unicodeDictionary.containsKey(uniChar)){
                //System.out.println("Into merge");
                value = unicodeDictionary.get(uniChar).split("-")[0];
                String comLabel = unicodeDictionary.get(uniChar).split("-")[1];
                merge=mergeId;
                if(mergeMap.containsKey(mergeId)){
                    compundCharacter comchar = mergeMap.get(mergeId);
                    ArrayList<Integer> mergeList = comchar.charList;
                    mergeList.add(charId);
                }else{
                    ArrayList<Integer> mergeList =new ArrayList<Integer>();
                    mergeList.add(charId);
                    mergeMap.put(mergeId,new compundCharacter(mergeId,comLabel,mergeList,null));
                }

                if(text.getUnicode().equals('\u221A') || text.getUnicode().equals('\u23B7') || text.getUnicode().equals("√") ) {
                    for (Bars bar : currentPage.bars) {
                        BBOX box = bar.boundingBox;
                        System.out.println("Box startY::"+box.startY +" RstartY:"+text.getTextMatrix().getTranslateY()+" wHeight:"+(text.getTextMatrix().getTranslateY()+text.getHeight()));
                        if (Math.floor(box.startY) == Math.floor(text.getTextMatrix().getTranslateY())) {

                            System.out.println();
                        }
                    }
                }
            }
            */
            characterInfo character = new characterInfo(charId,value,text,null,merge,wordId,lineId-1);
            currentPage.pageCharacters.put(charId,character);
            CharList.add(character);
            charId++;
            prevBaseLineX=text.getTextMatrix().getTranslateX()+text.getWidthDirAdj();


        }
        if(singleWord){
            WordList.add(new Words(wordId,CharList));
            wordId++;
            CharList= new ArrayList<characterInfo>();
        }


    }

    public void getBoundingBox() throws IOException {
        HashMap<Integer, characterInfo> charList = currentPage.pageCharacters;
        ArrayList<DrawSymbol> expressionComponents = new ArrayList<>();
        Iterator iter = charList.entrySet().iterator();
        double widthRatio;
        while (iter.hasNext()) {
            Map.Entry pair = (Map.Entry) iter.next();

            characterInfo character = (characterInfo) pair.getValue();

            TextPosition text = character.charInfo;

            /*
            if(current_radical!=null){
                System.out.println("Enclosed "+text.getUnicode()+" "+current_radical.charInfo.contains(character.charInfo));
                //System.out.println(current_radical.charInfo.contains(character.charInfo));
            }
            if(text.getUnicode().equals("\u221A")){
                current_radical=character;
                System.out.println("unicode matched"+text.getUnicode());
            }
            */

            float startX = text.getTextMatrix().getTranslateX();
            float startY = text.getTextMatrix().getTranslateY();
         //   System.out.println(startX+":::"+startY);
            float fontSize = text.getFontSize();

            //PDType1Font font = (PDType1Font) text.getFont();

            PDFont font = text.getFont();


            drawGlyph glyph=null;


            if (font instanceof PDTrueTypeFont){
                PDTrueTypeFont TTFfont = (PDTrueTypeFont) font;

                glyph = new drawGlyph(TTFfont.getPath(text.getCharacterCodes()[0]),
                        text.getCharacterCodes()[0], text.getUnicode(), fontSize,2048);


            }
            else if (font instanceof PDType1Font) {
                PDType1Font Type1font = (PDType1Font) font;
                glyph = new drawGlyph(Type1font.getPath(Type1font.codeToName(text.getCharacterCodes()[0])),
                        text.getCharacterCodes()[0], text.getUnicode(), fontSize,1000);

            }
            else if (font instanceof PDType1CFont) {
                PDType1CFont Type1Cfont = (PDType1CFont) font;
                glyph = new drawGlyph(Type1Cfont.getPath(Type1Cfont.codeToName(text.getCharacterCodes()[0])),
                        text.getCharacterCodes()[0], text.getUnicode(), fontSize,1000);

            }

            else if (font instanceof PDType0Font) {
                PDType0Font Type0font = (PDType0Font) font;
                glyph = new drawGlyph(Type0font.getPath(text.getCharacterCodes()[0]),
                        text.getCharacterCodes()[0], text.getUnicode(), fontSize,2048);
                //glyph.draw(type.Normal);

            }
            else if (font instanceof PDType3Font) {
                PDType3Font Type3font = (PDType3Font) font;
                glyph = new drawGlyph(Type3font.getPath(Type3font.getName()),
                        text.getCharacterCodes()[0], text.getUnicode(), fontSize,1000);
            }else{
                PDType1Font font1 = (PDType1Font) font;
                System.out.println("Unknown "+" Page:"+pageNum+" Label::" +text.getUnicode());
            }

            if(glyph== null){
                System.out.println();
            }

///////////////////////////////////////////////////////////////////////////////////////
           // expressionComponents.add(new DrawSymbol(glyph, startX, startY) );
           // glyph.draw(type.Normal,null); 
//////////////////////////////////////////////////////////////////////////////////////
           
            try {
                glyph.coordinates();
                glyph.BoxCoord();
            }catch(Exception e){
                System.out.println("Error at::"+" Page:"+pageNum+" Label::" +text.getUnicode());
                e.printStackTrace();
            }
            double width = glyph.adjustResolution(glyph.maxX, fontSize) - glyph.adjustResolution(glyph.minX, fontSize);
            double height = glyph.adjustResolution(glyph.maxY, fontSize) - glyph.adjustResolution(glyph.minY, fontSize);

            
            // Update starting point (Y-axis)
            double heightRatio = height / (glyph.maxY-glyph.minY);
            if (glyph.minY < 0) {
                double decentheight = heightRatio * glyph.minY;
                startY = startY + (float) decentheight;
            } else if (glyph.minY > 0) {
                double baseAccent = heightRatio * glyph.minY;
                startY = startY - (float) baseAccent;
            }
            // Update starting point (X-axis)
            //double widthRatio1 = width / (font.getWidthFromFont(text.getCharacterCodes()[0]));
             widthRatio=0.01;
           
             if (glyph.minX < 0) {
                double leftMove = widthRatio * glyph.minX;
                startX = startX + (float) leftMove;
            }
             else if (glyph.minX > 0) {
                double rightMove = widthRatio * glyph.minX;
                startX = startX - (float) rightMove;
            }
//////////////////////////////////////////////////////////////////////////////////////////////
            System.out.println(glyph.unicode+"--"+startX+"--"+startY+"--"+widthRatio+" -- "+fontSize);
           DrawSymbol temp = new DrawSymbol(glyph,(float)( startX/widthRatio),(float) (startY/widthRatio),fontSize) ;
           temp.height = height/widthRatio;
           temp.width = width/widthRatio;
           expressionComponents.add(temp);
           // double glyphStartX = startX/ widthRatio;
           // System.out.println(startX +"--" + glyphStartX );
//////////////////////////////////////////////////////////////////////////////////////////////            
            character.boundingBox = new BBOX(startX, startY, (float) width, (float) height);
            ///
            if(character.value.equals('\u221A') || character.value.equals('\u23B7') || character.value.equals("√") ) {
                radical.add(character);
            }
            //First charac
            if(previousCharacter==null){
                BBOX tempBox = new BBOX(character.boundingBox.startX,character.boundingBox.startY,character.boundingBox.width,character.boundingBox.height);
                ArrayList<Integer> neighList = new ArrayList<Integer>();
                neighList.add(character.charId);
                previousCharacter= new compundCharacter(mergeId,character.value,neighList,tempBox);
            }else {
            	//No combination
                if (notAlphabet(character.value) && notAlphabet(previousCharacter.value) && overlap(previousCharacter.boundingBox, character.boundingBox)) {
                    if (mergeMap.containsKey(mergeId)) {
                        character.mergeId=mergeId;
                        compundCharacter comChar = mergeMap.get(mergeId);
                        ArrayList<Integer> neighList = comChar.charList;
                        neighList.add(character.charId);
                        BBOX combox = comChar.boundingBox;
                        float newStartX = findMin(combox.startX, startX);
                        float newStartY = findMin(combox.startY, startY);
                        float newEndX = findMax(combox.startX + combox.width, startX + (float) width);
                        float newEndY = findMax(combox.startY + combox.height, startY + (float) height);
                        combox.startX = newStartX;
                        combox.startY = newStartY;
                        combox.width = newEndX - newStartX;
                        combox.height = newEndY - newStartY;
                    }else {
                    	// else merge
                        character.mergeId=mergeId;
                        ArrayList<Integer> neighList = previousCharacter.charList;
                        for(Integer charid: neighList){
                            characterInfo prev = charList.get(charid);
                            prev.mergeId=mergeId;
                        }
                        neighList.add(character.charId);
                        BBOX prevbox = previousCharacter.boundingBox;
                        float newStartX = findMin(prevbox.startX, startX);
                        float newStartY = findMin(prevbox.startY, startY);
                        float newEndX = findMax(prevbox.startX + prevbox.width, startX + (float) width);
                        float newEndY = findMax(prevbox.startY + prevbox.height, startY + (float) height);

                        prevbox.startX = newStartX;
                        prevbox.startY = newStartY;
                        prevbox.width = newEndX - newStartX;
                        prevbox.height = newEndY - newStartY;

                        mergeMap.put(mergeId,previousCharacter);
                    }
                }
                else {
                    mergeId++;
                    ArrayList<Integer> neighList = new ArrayList<Integer>();
                    neighList.add(character.charId);
                    BBOX tempBox = new BBOX(character.boundingBox.startX,character.boundingBox.startY,character.boundingBox.width,character.boundingBox.height);
                    previousCharacter= new compundCharacter(mergeId,character.value,neighList,tempBox);
                }
            }


        }
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////  
      //expressionComponents
        float base = expressionComponents.get(0).startX;
        System.out.println("base "+base);
         
//        BufferedImage image = new BufferedImage(expressionComponents.size()*1000, 2500, BufferedImage.TYPE_INT_BGR); 
        
        expressionComponents.sort((o1, o2) -> ((Float)(o1.startX)).compareTo(o2.startX));
        float baseY = 25000000;
        double maxVal=0;
        DrawSymbol maxY=null;
        for(DrawSymbol a :expressionComponents ) {
        	
        	if(a.startY<baseY) {baseY = a.startY;}
        	if(a.startY+a.height>maxVal) {
        		maxVal=a.startY+a.height;
        		maxY = a;}
        }
        System.out.println(maxY.obj.unicode);
        System.out.println("Height --"+(maxY.startY-baseY+maxY.height)+" - " +maxY.height);
        BufferedImage image = new BufferedImage((int) (expressionComponents.get(expressionComponents.size()-1).startX+2000-base), (int)(maxY.startY-baseY+maxY.height+500), BufferedImage.TYPE_INT_BGR);
    //    BufferedImage image = new BufferedImage(100,100,BufferedImage.TYPE_INT_BGR);
        //maxY.startY-baseY+maxY.height
        Graphics2D graphic = image.createGraphics();
        double baseFont =expressionComponents.get(0).fontSize;
        for(DrawSymbol a :expressionComponents ) {
        	//System.out.println(a.startX);
        	a.startX -= base;
        	a.startY -= baseY;
        	System.out.println(a.obj.unicode+"--"+a.startX+"--"+a.startY);
        	//a.obj.minX-=base;
        	a.obj.offset = a.startX+500;
        	a.obj.offsetY = a.startY;
        	a.obj.fontFactor = a.fontSize/baseFont;
        	//System.out.println(a.startY);
        //	a.obj.maxX-=base;
        	a.obj.draw(type.Normal,graphic);
        	
        }
        
        //Flip the image
        AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
      //  System.out.println("Transform = "+ (int)(maxY.startY+maxY.height));
        tx.translate(0, -(int)(maxY.startY+maxY.height+500));
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        image = op.filter(image, null);


        File output = new File("Rahuloutput.png");
        ImageIO.write(image, "png", output);
      //////////////////////////////////////////////////////////////////////////////////////////////////////////

    }


    public float findMax(float val1,float val2){
        return (val1>val2)?val1:val2;
    }

    public float findMin(float val1,float val2){
        return (val1<val2)?val1:val2;
    }

    public boolean overlap(BBOX ch1,BBOX ch2){

        if (ch1.startX > (ch2.startX+ch2.width) || ch2.startX > (ch1.startX+ch1.width))
            return false;
        if ((ch1.startY+ch1.height) < (ch2.startY) || (ch2.startY+ch2.height) < (ch1.startY))
            return false;

        return true;

    }

    public boolean notAlphabet(String value){
        Pattern p = Pattern.compile("[^\\p{ASCII}]");
        boolean result = p.matcher(value).find();
        return  result;
    }

    public void includeBars(){

        for(characterInfo chID: radical){
            int lastChar = currentPage.pageCharacters.size();
            if(chID.mergeId!=0){
                //System.out.println(chID.mergeId);
                compundCharacter comChar = mergeMap.get(chID.mergeId);
                BBOX combox = comChar.boundingBox;
                BBOX ownbox = chID.boundingBox;
                for(int i=0;i<currentPage.bars.size();i++) {
                    Bars bar = currentPage.bars.get(i);

                    if(overlap(combox,bar.boundingBox) || overlap(ownbox,bar.boundingBox)){
                        ArrayList<Integer> neighList = comChar.charList;
                        BBOX tempBox = new BBOX(bar.boundingBox.startX,bar.boundingBox.startY,bar.boundingBox.width,bar.boundingBox.height);
                        characterInfo  newchar = new characterInfo(lastChar, "square root bar(-)", null,tempBox, chID.mergeId,chID.wordID,chID.lineID);
                        currentPage.pageCharacters.put(lastChar,newchar);
                        neighList.add(newchar.charId);

                        float newStartX = findMin(combox.startX, bar.boundingBox.startX);
                        float newStartY = findMin(combox.startY, bar.boundingBox.startY);
                        float newEndX = findMax(combox.startX + combox.width, bar.boundingBox.startX +  bar.boundingBox.width);
                        float newEndY = findMax(combox.startY + combox.height, bar.boundingBox.startY +  bar.boundingBox.height);
                        combox.startX = newStartX;
                        combox.startY = newStartY;
                        combox.width = newEndX - newStartX;
                        combox.height = newEndY - newStartY;

                        ArrayList<Line> lineList = currentPage.Lines;
                        ArrayList<Words> wordList = lineList.get(chID.lineID).words;
                        for(Words word:wordList) {
                            if(word.wordId==chID.wordID) {
                                ArrayList<characterInfo> charList = word.characters;
                                charList.add(newchar);
                            }
                        }

                        currentPage.bars.remove(bar);
                    }
                }
                //mergeId++;
                //mergeMap.put(mergeId,comChar);

            }else{
                mergeId++;
                BBOX tempBox = new BBOX(chID.boundingBox.startX,chID.boundingBox.startY,chID.boundingBox.width,chID.boundingBox.height);
                ArrayList<Integer> neighList = new ArrayList<Integer>();
                neighList.add(chID.charId);
                compundCharacter comchar= new compundCharacter(mergeId,chID.value,neighList,tempBox);
                chID.mergeId=mergeId;
                BBOX combox = comchar.boundingBox;
                for(int i=0;i<currentPage.bars.size();i++) {
                    Bars bar = currentPage.bars.get(i);
                    if (overlap(combox, bar.boundingBox)) {
                        BBOX barBox = new BBOX(bar.boundingBox.startX, bar.boundingBox.startY, bar.boundingBox.width, bar.boundingBox.height);
                        characterInfo newchar = new characterInfo(lastChar, "square root bar(-)", null, barBox, chID.mergeId,chID.wordID,chID.lineID);
                        currentPage.pageCharacters.put(lastChar, newchar);
                        neighList.add(newchar.charId);
                        float newStartX = findMin(combox.startX, bar.boundingBox.startX);
                        float newStartY = findMin(combox.startY, bar.boundingBox.startY);
                        float newEndX = findMax(combox.startX + combox.width, bar.boundingBox.startX + bar.boundingBox.width);
                        float newEndY = findMax(combox.startY + combox.height, bar.boundingBox.startY + bar.boundingBox.height);
                        combox.startX = newStartX;
                        combox.startY = newStartY;
                        combox.width = newEndX - newStartX;
                        combox.height = newEndY - newStartY;

                        ArrayList<Line> lineList = currentPage.Lines;
                        ArrayList<Words> wordList = lineList.get(chID.lineID).words;
                        for(Words word:wordList) {
                            if(word.wordId==chID.wordID) {
                                ArrayList<characterInfo> charList = word.characters;
                                charList.add(newchar);
                            }
                        }
                        mergeMap.put(mergeId,comchar);
                        currentPage.bars.remove(bar);
                    }
                }


            }
        }


        ArrayList<Line> allLines =  currentPage.Lines;
        ArrayList<Bars> allBars = currentPage.bars;
        for(int i=0;i<allBars.size();i++) {
            int lastChar = currentPage.pageCharacters.size();
            int aboveLine = 0;
            int belowLine = 0;
            boolean flag=false;
            for (int j = 0; j < allLines.size(); j++) {
                try {
                    //System.out.println((allLines.get(j).baseLine.startY < allBars.get(i).boundingBox.startY) +" "+
                    //        (allLines.get(j).baseLine.startX <= allBars.get(i).boundingBox.startX) +" "+
                    //        (allLines.get(j).baseLine.endX >= (allBars.get(i).boundingBox.startX + allBars.get(i).boundingBox.width)));
                    if ((allLines.get(j).baseLine.startY > allBars.get(i).boundingBox.startY)){ //&&
                            //(allLines.get(j).baseLine.startX <= allBars.get(i).boundingBox.startX)) {
                            //(allLines.get(j).baseLine.endX >= (allBars.get(i).boundingBox.startX + allBars.get(i).boundingBox.width))) {

                        aboveLine = j;
                    } else if ((allLines.get(j).baseLine.startY <= allBars.get(i).boundingBox.startY)){// &&
                            //(allLines.get(j).baseLine.startX <= allBars.get(i).boundingBox.startX) ){
                            //(allLines.get(j).baseLine.endX >= (allBars.get(i).boundingBox.startX + allBars.get(i).boundingBox.width))) {

                        //belowLine = j;
                        Line line = allLines.get(aboveLine);
                        ArrayList<Words> wordlist = line.words;
                        lastChar = currentPage.pageCharacters.size();
                        characterInfo newChar = new characterInfo(lastChar , "fraction(-)", null, allBars.get(i).boundingBox, 0,wordlist.size(),line.LineId);
                        ArrayList<characterInfo> charList = new ArrayList<characterInfo>();
                        charList.add(newChar);
                        Words newWord = new Words(wordlist.size(), charList);
                        wordlist.add(newWord);
                        currentPage.pageCharacters.put(lastChar , newChar);
                        //System.out.println("Bar added into characters");
                        break;
                    }
                }catch (Exception e){

                    System.out.println(i+" "+j);
                    e.printStackTrace();
                }

            }
        }

    }

}

