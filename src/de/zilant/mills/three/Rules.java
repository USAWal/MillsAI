package de.zilant.mills.three;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.zilant.mills.three.PieceType;


public interface Rules {
	PieceType                            whoDidAMill             (long from    , long to                             );
	int                                  whatsTheMaxOfPlaces     (                                                   );
	int                                  whatsTheMaxOfPieces     (                                                   );
	List <Map<PositionState, Set<Long>>> getPositionsTree        (                                                   );
	int                                  whatsTheCode            (                                                   );
	PieceType                            whoIsBlocked            (long position                                      );
	PieceType                            whoHasAMill             (long position                                      );
	int                                  howManyPiecesOf         (long position, PieceType pieceType                 );
	boolean                              isPositionReachableBy   (long from    , long to, PieceType typeOfMovingPiece);
	long                                 reflectHorizontally     (long position                                      );
	long                                 reflectVertically       (long position                                      );
	Collection<Long>                     getReachablePositionsBy (long position, PieceType pieceType                 );
}
