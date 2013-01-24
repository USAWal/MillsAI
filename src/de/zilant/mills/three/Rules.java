package de.zilant.mills.three;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.zilant.mills.three.PieceType;


public interface Rules {
	List <Map<PositionState, Set<Long>>> getPositionsTree      (int  maxNumberOfPieces                         );
	int                                  whatsTheCode          (                                               );
	PieceType                            whoIsBlocked          (long position                                  );
	PieceType                            whoHasAMill           (long position                                  );
	int                                  howManyPiecesOf       (long position, PieceType pieceType             );
	boolean                              isPositionReachableBy (long from, long to, PieceType typeOfMovingPiece);
	long                                 reflectHorizontally   (long position                                  );
	long                                 reflectVertically     (long position                                  );
}
