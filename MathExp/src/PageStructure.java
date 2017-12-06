//package TrueBox;

import org.apache.pdfbox.text.TextPosition;

import java.util.ArrayList;
import java.util.HashMap;

class PageStructure{
    int pageId;
    ArrayList<Line> Lines;
    ArrayList<Bars> bars;
    HashMap<Integer,characterInfo> pageCharacters;
    HashMap<Integer,compundCharacter> pageCompundCharacters;
    PageStructure(int pgId, ArrayList<Line> sent,ArrayList<Bars> bars, HashMap<Integer,characterInfo> pageChar,HashMap<Integer,compundCharacter> comChar){
        this.pageId=pgId;
        this.Lines=sent;
        this.bars=bars;
        this.pageCharacters= pageChar;
        this.pageCompundCharacters=comChar;
    }
}

class Line{
    int LineId;
    ArrayList<Words> words;
    baseLine baseLine;
    Line(int sentid, baseLine baseline, ArrayList<Words> words){
        this.LineId=sentid;
        this.words=words;
        this.baseLine=baseline;
    }

}

class Words{
    int wordId;

    ArrayList<characterInfo> characters;
    Words(int wordid, ArrayList<characterInfo> charInfo ){
        this.wordId=wordid;
        this.characters =charInfo;

    }

}

class Bars{
    BBOX boundingBox;
    Bars(BBOX boundingBox){
        this.boundingBox=boundingBox;
    }
}

class characterInfo{

    int charId;
    String value;
    BBOX boundingBox;
    int mergeId;
    int wordID;
    int lineID;
    org.apache.pdfbox.text.TextPosition charInfo;

    characterInfo(int charId,String value,TextPosition text,BBOX bbox, int mergeId,int wordID,int lineID){
        this.charId=charId;
        this.value=value;
        this.charInfo = text;
        this.boundingBox=bbox;
        this.mergeId = mergeId;
        this.wordID=wordID;
        this.lineID=lineID;
    }

}


class compundCharacter{
    int charId;
    String value;
    BBOX boundingBox;


    ArrayList<Integer> charList;

    compundCharacter(int mergeId,String value,ArrayList<Integer> charList,BBOX bbox){
        this.charId=mergeId;
        this.value=value;
        this.boundingBox=bbox;
        this.charList=charList;
    }

}


class BBOX{
    float startX;
    float startY;
    float width;
    float height;

    BBOX(float x,float y, float w,float h){
        this.startX=x;
        this.startY=y;
        this.width=w;
        this.height=h;
    }

}

class baseLine{
    float startX;
    float startY;
    float endX;
    float endY;
    baseLine(float x,float y, float x2, float y2){
        this.startX=x;
        this.startY=y;
        this.endX=x2;
        this.endY=y2;
    }
}