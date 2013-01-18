package de.zilant.mills.three;
import de.zilant.mills.three.PieceType;


public interface Rules {
	int       whatsTheCode          (                                       );
	PieceType whoIsBlocked          (long position                          );
	PieceType whoHasAMill           (long position                          );
	int       howManyPiecesOf       (long position, PieceType pieceType     );
	boolean   isPositionReachableBy (long from, long to, PieceType pieceType);
	long      reflectHorizontally   (long position                          );
	long      reflectVertically     (long position                          );
}
