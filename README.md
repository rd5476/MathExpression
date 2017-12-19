# MathExpression
# First version does not take XML as input it directly takes data from PDF, pdf used as input had only math expression as input

Compile : javac src/*.java
Run : java Main -n [input pdf file name] [path to tempUnicode] [Destination for output] -p

#src folder has few input expression only files. 

# I have added logic to render image for the expression in the file BoundingBox.java
# I also have changed drawGlyph.java's function adjustcoordinate to scale the glyph points
