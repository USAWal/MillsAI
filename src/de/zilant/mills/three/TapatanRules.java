package de.zilant.mills.three;

public class TapatanRules extends ThreeMensMorrisRules {
	
	@Override
	public int whatsTheCode() { return 2; }
	
	@Override
	public PieceType whoIsBlocked(long position) { return PieceType.NONE; }
	
	@Override
	public PieceType whoHasAMill(long position) {
		PieceType result = super.whoHasAMill(position);
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
	
}
