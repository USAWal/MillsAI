package de.zilant.mills.three;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class FiveMensMorrisRules implements Rules {

	@Override
	public List<Map<PositionState, Set<Long>>> getPositionsTree(int maxNumberOfPieces) {
		List<Map<PositionState, Set<Long>>> result = new ArrayList<Map<PositionState,Set<Long>>>();
		for(int index = 0; index < (maxNumberOfPieces-2)*(maxNumberOfPieces-2); index++)
			result.add(createPositions());
		
		for(long topLine = 0; topLine <= 0xAAA; topLine = increaseLine(topLine))
			for(long bottomLine = topLine; bottomLine <= 0xAAA; bottomLine = increaseLine(bottomLine))
				for(long centerLne = 0; centerLne <= 0xAAA; centerLne = increaseLine(centerLne)) {
					long position = topLine << 20 | centerLne << 12 | bottomLine;
					int myPiecesNumber = howManyPiecesOf(position, PieceType.MINE);
					int opponentsPiecesNumber = howManyPiecesOf(position, PieceType.OPPONENTS);
					if(
							myPiecesNumber        >= 3 && myPiecesNumber        <= maxNumberOfPieces &&
							opponentsPiecesNumber >= 3 && opponentsPiecesNumber <= maxNumberOfPieces) {					
						PieceType blocked = whoIsBlocked(position);
						PieceType milled = whoHasAMill(position);
						Map<PositionState, Set<Long>> positions = result.get((maxNumberOfPieces-myPiecesNumber)*(maxNumberOfPieces-2)+(maxNumberOfPieces-opponentsPiecesNumber));
						long symmetricPosition = bottomLine << 20 | centerLne << 12 | topLine;
						if(blocked== PieceType.OPPONENTS    || (milled == PieceType.MINE      && opponentsPiecesNumber == 3)) {
							positions.get(PositionState.WIN).add(position);
							positions.get(PositionState.WIN).add(symmetricPosition);
						} else if(blocked == PieceType.MINE || (milled == PieceType.OPPONENTS && myPiecesNumber        == 3)) {
							positions.get(PositionState.LOSS).add(position);
							positions.get(PositionState.LOSS).add(symmetricPosition);
						} else if(milled != PieceType.BOTH) {
							positions.get(PositionState.DRAW).add(position);
							positions.get(PositionState.DRAW).add(symmetricPosition);
						}
					}
				}	
		
		return result;
	}

	@Override
	public int whatsTheCode() { return 3; }

	@Override
	public PieceType whoIsBlocked(long position) {
		for(int[] intersection : connections) {
			intersection[0] = (int) position & 3;
			position >>= 2;
		}
		
		PieceType result = PieceType.BOTH;
		for(int[] intersection : connections) {
			for(int index = 1; index < intersection.length; index++) {
				if(result == PieceType.NONE) return result;
				if(intersection[0] == PieceType.NONE.VALUE) {
					if(intersection[index] == PieceType.MINE.VALUE) {
						if     (result == PieceType.BOTH) result = PieceType.OPPONENTS;
						else if(result == PieceType.MINE) result = PieceType.NONE     ;
					} else if(intersection[index] == PieceType.OPPONENTS.VALUE) {
						if     (result == PieceType.BOTH     ) result = PieceType.MINE;
						else if(result == PieceType.OPPONENTS) result = PieceType.NONE;
					}
				} else if(intersection[index] == PieceType.NONE.VALUE) {
					if(intersection[0] == PieceType.MINE.VALUE) {
						if     (result == PieceType.BOTH) result = PieceType.OPPONENTS;
						else if(result == PieceType.MINE) result = PieceType.NONE;
					} else if(intersection[0] == PieceType.OPPONENTS.VALUE) {
						if     (result == PieceType.BOTH     ) result = PieceType.MINE;
						else if(result == PieceType.OPPONENTS) result = PieceType.NONE;
					}
				}
			}
		}
		
		return result;
	}

	@Override
	public PieceType whoHasAMill(long position) {
		PieceType result     = PieceType.NONE;
		
		for(long lineMask = 0x0015; lineMask <= 0x1500000; lineMask <<= 8)
			for(int index = 0; index < 2; index++) {
				if((position &  lineMask         ) == lineMask)   if(result == PieceType.NONE) result = PieceType.OPPONENTS; else return PieceType.BOTH;
				if((position & (lineMask   <<= 1)) == lineMask)   if(result == PieceType.NONE) result = PieceType.MINE;      else return PieceType.BOTH;
				lineMask   <<= 5;
			}
		
		for(long columnMask : verticalMillsMasks) {
			if((position &  columnMask         ) == columnMask)   if(result == PieceType.NONE) result = PieceType.OPPONENTS; else return PieceType.BOTH;
			if((position & (columnMask   <<= 1)) == columnMask)   if(result == PieceType.NONE) result = PieceType.MINE;      else return PieceType.BOTH;			
		}
		
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
	public boolean isPositionReachableBy(long from, long to, PieceType pieceType) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public long reflectHorizontally(long position) {
		return (position & 0xFFF00000) >> 20 | position & 0xFF000  | (position & 0xFFF)   << 20;
	}

	@Override
	public long reflectVertically(long position) {
		return 
				(position & 0x0000C000) >>  2 |
				(position & 0x0C3000C3) >>  4 | 
				(position & 0x00003000) >>  6 | 
				 position & 0x30C0030C        | 
				(position & 0x000C0000) <<  6 | 
				(position & 0xC3000C30) <<  4 | 
				(position & 0x00030000) <<  2;
	}
	
	private Map<PositionState,Set<Long>> createPositions() {
		Map<PositionState,Set<Long>> positions = new EnumMap<PositionState, Set<Long>>(PositionState.class);
		positions.put(PositionState.WIN,          new TreeSet<Long>());
		positions.put(PositionState.ONLY_TO_WIN,  new TreeSet<Long>());
		positions.put(PositionState.TO_WIN,       new TreeSet<Long>());
		positions.put(PositionState.DRAW,         new TreeSet<Long>());
		positions.put(PositionState.TO_LOSS,      new TreeSet<Long>());
		positions.put(PositionState.ONLY_TO_LOSS, new TreeSet<Long>());
		positions.put(PositionState.LOSS,         new TreeSet<Long>());
		return positions;
	}
	
	private long increaseLine(long line) {
		long mask = 2;
		while((line & mask) != 0) {
			line -= mask;
			mask <<= 2;
		}
		line += mask >> 1;
		return line;
	}
	
	private int[][] connections = new int[][] {
			{0,  1,  6    },
			{0,  0,  2,  4},
			{0,  1,  9    },
			{0,  4,  7    },
			
			{0,  1,  3,  5},
			{0,  4,  8    },
			{0,  0,  7, 13},
			{0,  3,  6, 10},
			
			{0,  5,  9, 12},
			{0,  2,  8, 15},
			{0,  7, 11    },
			{0, 10, 12, 14},
			
			{0,  8, 11    },
			{0,  6, 14    },
			{0, 11, 13, 15},
			{0,  9, 14    },
	};
	
	private long[] verticalMillsMasks = new long[] {
			0x04001001,
			0x40040010,
			0x00104040,
			0x01010400
	};

}
