package de.zilant.mills.three;

public class Position {
	
	public Position(long value, PositionState state) {
		VALUE                      = value;
		NUMBER_OF_MY_PIECES        = getNumberOf(VALUE, PieceType.MINE);
		NUMBER_OF_OPPONENTS_PIECES = getNumberOf(VALUE, PieceType.OPPONENTS);
		this.state                 = state;
	}
	
	public Position(long value) { this(value, PositionState.DRAW); }
	
	public void setState(PositionState state) { this.state = state; }
	public PositionState getState() { return state; }
	
	public boolean hasMill(PieceType pieceType) {
		long lineMask   = pieceType == PieceType.OPPONENTS ? 0x0015 : 0x002A;
		long columnMask = pieceType == PieceType.OPPONENTS ? 0x1041 : 0x2082;
		
		for(int index = 0; index < 3; index++) {
			if((VALUE & lineMask)   == lineMask) return true;
			if((VALUE & columnMask) == columnMask) return true;
			lineMask <<= 6;
			columnMask <<= 2;
		}
		
		long diagonalMask = pieceType == PieceType.OPPONENTS ? 0x10101 : 0x20202;
		if((VALUE & diagonalMask) == diagonalMask) return true;
		diagonalMask = pieceType == PieceType.OPPONENTS ? 0x01110 : 0x02220;
		if((VALUE & diagonalMask) == diagonalMask) return true;
		
		return false;
	}
	
	public Position putTo(int placeIndex, PieceType pieceType) {
		if(pieceType == PieceType.NONE) return null;
		if((pieceType == PieceType.MINE ? NUMBER_OF_MY_PIECES : NUMBER_OF_OPPONENTS_PIECES) >= 3) return null;
		if((VALUE >> placeIndex * 2 & 3) != PieceType.NONE.VALUE) return null;
		long positionValue = VALUE & ~(3 << placeIndex*2) | (pieceType.VALUE << placeIndex*2);
		return new Position(positionValue, PositionState.DRAW);
	} 
	
	public static boolean isReachable(Position from, Position to, boolean byOpponent) {
		
		long differences = from.VALUE ^ to.VALUE;
		int fromIndex    = -1;
		int toIndex      = -1;
		int placeIndex   =  0;
		do {
			int pieceType = (int) differences & 3;
			if(pieceType == (byOpponent ? PieceType.OPPONENTS.VALUE : PieceType.MINE.VALUE)) {
				if(fromIndex < 0) fromIndex = placeIndex;
				else if(toIndex < 0) toIndex = placeIndex;
				else return false;
			} else if(pieceType != 0)
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
	public String toString() {
		return "value is [" + VALUE + "], state is [" + state + "]";
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj) return true;
		if(!(obj instanceof Position)) return false;
		Position other = (Position) obj;
		return VALUE == other.VALUE && state == other.state;
	}
	
	private static int getNumberOf(long value, PieceType type) {	
		int result = 0;
		do {
			if((value & 3) == type.VALUE) result++;
			value >>= 2;
		} while(value != 0);

		return result;
	}

	public final long     VALUE;
	public final int      NUMBER_OF_MY_PIECES;
	public final int      NUMBER_OF_OPPONENTS_PIECES;
	private PositionState state;
	
}