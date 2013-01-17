package de.zilant.mills.three;

public class ThreeMensMorrisRules implements Rules {

	@Override
	public PieceType whoIsBlocked(long position) { return PieceType.NONE; }

	@Override
	public PieceType whoHasAMill(long position) {
		PieceType result     = PieceType.NONE;
		long      lineMask   = 0x0015;
		long      columnMask = 0x1041;
		
		for(int index = 0; index < 3; index++) {
			if((position &  lineMask         ) == lineMask)   if(result == PieceType.NONE) result = PieceType.OPPONENTS; else return PieceType.BOTH;
			if((position & (lineMask   <<= 1)) == lineMask)   if(result == PieceType.NONE) result = PieceType.MINE;      else return PieceType.BOTH;
			if((position &  columnMask       ) == columnMask) if(result == PieceType.NONE) result = PieceType.OPPONENTS; else return PieceType.BOTH;
			if((position & (columnMask <<= 1)) == columnMask) if(result == PieceType.NONE) result = PieceType.MINE;      else return PieceType.BOTH;
			lineMask   <<= 5;
			columnMask <<= 1;
		}
		
		if(result != PieceType.NONE) return result;
		
		long diagonalMask = 0x10101;
		if((position &  diagonalMask       ) == diagonalMask) result = PieceType.OPPONENTS;
		if((position & (diagonalMask <<= 1)) == diagonalMask) return result == PieceType.NONE ? PieceType.MINE : PieceType.BOTH;
		if(result != PieceType.NONE) return result;
		     diagonalMask = 0x01110;
		if((position &  diagonalMask       ) == diagonalMask) result = PieceType.OPPONENTS;
		if((position & (diagonalMask <<= 1)) == diagonalMask) return result == PieceType.NONE ? PieceType.MINE : PieceType.BOTH;
		
		return result;
	}

	@Override
	public int howManyPiecesOf(long position, PieceType pieceType) {
		int result = 0;
		do {
			if((position & 3) == pieceType.VALUE) result++;
			position >>= 2;
		} while(position != 0);

		return result;
	}

	@Override
	public boolean isPositionReachableBy(long from, long to, PieceType typeOfMovingPiece) {
		long differences = from ^ to;
		int fromIndex    = -1;
		int toIndex      = -1;
		int placeIndex   =  0;
		do {
			int positionPieceType = (int) differences & 3;
			if(positionPieceType == typeOfMovingPiece.VALUE) {
				if(fromIndex < 0) fromIndex = placeIndex;
				else if(toIndex < 0) toIndex = placeIndex;
				else return false;
			} else if(positionPieceType != 0)
				return false;
			placeIndex++;
			differences >>= 2;
		} while(differences != 0);
		
		if(	fromIndex == 4
			|| toIndex == 4 
			|| fromIndex/3 == toIndex/3 && Math.abs(fromIndex%3 - toIndex%3) == 1
			|| fromIndex%3 == toIndex%3 && Math.abs(fromIndex/3 - toIndex/3) == 1)
			return true;
		
		return false;
	}

	@Override
	public long reflectHorizontally(long position) {
		return (position & 0x3F000) >> 12 | position & 0xFC0  | (position & 0x3F)   << 12;
	}

	@Override
	public long reflectVertically(long position) {
		return (position & 0x30C30) >>  4 | position & 0xC30C | (position & 0x30C3) <<  4;
	}

}
