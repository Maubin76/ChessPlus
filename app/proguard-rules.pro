# Règles R8 de ChessPlus.

# chesslib lit les FEN avec Square.valueOf / File.valueOf : on garde ses énumérations intactes.
-keepclassmembers enum com.github.bhlangonijr.chesslib.** { *; }
