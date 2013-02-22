package de.zilant.mills.three;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class FiveMensMorrisRules implements Rules {
	
	@Override
	public int whatsTheMaxOfPlaces() { return 16; }
	
	@Override
	public int whatsTheMaxOfPieces() { return  5; }
	
	@Override
	public int whatsTheCode()        { return  3; }
	
	@Override
	public PieceType whoDidAMill(long from, long to) {
		PieceType whoTheMillHas = whoHasAMill(to);
		if(whoTheMillHas != PieceType.NONE) {
			PieceType reachableBy = PieceType.NONE;
			if(isPositionReachableBy(from, to, PieceType.MINE)) reachableBy = PieceType.MINE;
			else if(isPositionReachableBy(from, to, PieceType.OPPONENTS)) reachableBy = PieceType.OPPONENTS;
			else return PieceType.NONE;
			
			long diff = from ^ to;
			
			for(long lineMask = 0x0015; lineMask <= 0x1500000; lineMask <<= 8)
				for(int index = 0; index < 2; index++) {
					if(reachableBy == PieceType.OPPONENTS && (diff & lineMask) != 0) return PieceType.OPPONENTS;
					lineMask <<= 1;
					if(reachableBy == PieceType.MINE && (diff & lineMask) != 0) return PieceType.MINE;
					lineMask   <<= 5;
				}
			
			for(long columnMask : verticalMillsMasks) {
				if(reachableBy == PieceType.OPPONENTS && (diff & columnMask) != 0) return PieceType.OPPONENTS;
				if(reachableBy == PieceType.MINE && (diff & (columnMask   <<= 1)) != 0) return PieceType.MINE;			
			}
		}
		
		return PieceType.NONE;
	}

	@Override
	public List<Map<PositionState, Set<Long>>> getPositionsTree() {
		if(positionsTree != null) return positionsTree;
		positionsTree = new ArrayList<Map<PositionState,Set<Long>>>();
		for(int index = 0; index < (whatsTheMaxOfPieces()-2)*(whatsTheMaxOfPieces()-2); index++)
			positionsTree.add(createPositions());
		
		for(long mostTopLine = 0; mostTopLine <= 0x2A; mostTopLine = increaseLine(mostTopLine))
		for(long mostBottomLine = mostTopLine; mostBottomLine <= 0x2A; mostBottomLine = increaseLine(mostBottomLine))
			for(long topLine = 0; topLine <= 0x2A; topLine = increaseLine(topLine))
			for(long bottomLine = topLine; bottomLine <= 0x2A; bottomLine = increaseLine(bottomLine))
				for(long centerLine = 0; centerLine <= 0xAA; centerLine = increaseLine(centerLine)) {
					elementaryEvaluation(mostTopLine, topLine, centerLine, bottomLine, mostBottomLine);
					if(mostTopLine != mostBottomLine && topLine != bottomLine)
						elementaryEvaluation(mostTopLine, bottomLine, centerLine, topLine, mostBottomLine);
				}
		
		return positionsTree;
	}
	
	private void elementaryEvaluation(long mostTopLine, long topLine, long centerLine, long bottomLine, long mostBottomLine) {
		long position = mostTopLine << 26 | topLine << 20 | centerLine << 12 | bottomLine << 6 | mostBottomLine;
		int myPiecesNumber = howManyPiecesOf(position, PieceType.MINE);
		int opponentsPiecesNumber = howManyPiecesOf(position, PieceType.OPPONENTS);
		if(
				myPiecesNumber        >= 3 && myPiecesNumber        <= whatsTheMaxOfPieces() &&
				opponentsPiecesNumber >= 3 && opponentsPiecesNumber <= whatsTheMaxOfPieces()) {	
			PieceType blocked = whoIsBlocked(position);
			PieceType milled = whoHasAMill(position);
			Map<PositionState, Set<Long>> positions = positionsTree.get((whatsTheMaxOfPieces()-myPiecesNumber)*(whatsTheMaxOfPieces()-2)+(whatsTheMaxOfPieces()-opponentsPiecesNumber));
			long symmetricPosition = mostBottomLine << 26 | bottomLine << 20 | centerLine << 12 | topLine << 6 | mostTopLine;
			if(milled == PieceType.BOTH) {
				if(myPiecesNumber == 3) {
					positions.get(PositionState.LOSS).add(position);
					positions.get(PositionState.LOSS).add(symmetricPosition);
				}
				if(opponentsPiecesNumber == 3) {
					positions.get(PositionState.WIN).add(position);
					positions.get(PositionState.WIN).add(symmetricPosition);	
				}
				if(opponentsPiecesNumber != 3) {
					positions.get(PositionState.DRAW).add(position);
					positions.get(PositionState.DRAW).add(symmetricPosition);					
				}

			} else if(blocked == PieceType.OPPONENTS   || (milled == PieceType.MINE      && opponentsPiecesNumber == 3)) {
				positions.get(PositionState.WIN).add(position);
				positions.get(PositionState.WIN).add(symmetricPosition);
			} else if(blocked == PieceType.MINE        || (milled == PieceType.OPPONENTS && myPiecesNumber        == 3)) {
				positions.get(PositionState.LOSS).add(position);
				positions.get(PositionState.LOSS).add(symmetricPosition);
			} else {
				positions.get(PositionState.DRAW).add(position);
				positions.get(PositionState.DRAW).add(symmetricPosition);
			}
		}		
	}

	@Override
	public PieceType whoIsBlocked(long position) {
		fillConnections(position);
		
		PieceType result = PieceType.BOTH;
		for(int[] intersection : connections) {
			for(int index = 1; index < intersection.length; index++) {
				if(result == PieceType.NONE) return result;
				if       (intersection[0]                     == PieceType.NONE.VALUE) {
					if(       connections[intersection[index]][0] ==      PieceType.MINE.VALUE) {
						if     (result == PieceType.BOTH) result = PieceType.OPPONENTS;
						else if(result == PieceType.MINE) result = PieceType.NONE     ;
					} else if(connections[intersection[index]][0] == PieceType.OPPONENTS.VALUE) {
						if     (result == PieceType.BOTH     ) result = PieceType.MINE;
						else if(result == PieceType.OPPONENTS) result = PieceType.NONE;
					}
				} else if(connections[intersection[index]][0] == PieceType.NONE.VALUE) {
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
				if((position &  lineMask         ) == lineMask) { if(result == PieceType.NONE) result = PieceType.OPPONENTS; else return PieceType.BOTH; }
				if((position & (lineMask   <<= 1)) == lineMask) { if(result == PieceType.NONE) result = PieceType.MINE;      else return PieceType.BOTH; }
				lineMask   <<= 5;
			}
		
		for(long columnMask : verticalMillsMasks) {
			if((position &  columnMask         ) == columnMask) { if(result == PieceType.NONE) result = PieceType.OPPONENTS; else return PieceType.BOTH; }
			if((position & (columnMask   <<= 1)) == columnMask) { if(result == PieceType.NONE) result = PieceType.MINE;      else return PieceType.BOTH; }			
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
		
		if(toIndex < 0) return false;
		
		for(int index = 1; index < connections[fromIndex].length; index++)
			if(connections[fromIndex][index] == toIndex) return true;
		
		return false;
	}
	
	@Override
	public Collection<Long> getReachablePositionsBy(long position, PieceType pieceType) {
		Set<Long> result =  new TreeSet<Long>();
		fillConnections(position);
		for(int intersectionIndex = 0; intersectionIndex < connections.length; intersectionIndex++) {
			if(connections[intersectionIndex][0] == pieceType.VALUE) {
				long positionWithoutPiece = ~(3 << intersectionIndex*2) & position;
				for(int neighbourIndex = 1; neighbourIndex < connections[intersectionIndex].length; neighbourIndex++) {
					int connectedIntersection = connections[intersectionIndex][neighbourIndex];
					if(connections[connectedIntersection][0] == PieceType.NONE.VALUE)
						result.add(positionWithoutPiece | (long) pieceType.VALUE << 2*connectedIntersection);
				}
			}
		}

		return result;
	}

	@Override
	public long reflectHorizontally(long position) {
		return
				(position & 0xFC000000) >> 26 |
				(position & 0x03F00000) >> 14 |
				 position & 0x000FF000        |
				(position & 0x00000FC0) << 14 |
				(position & 0x0000003F) << 26;
	}

	@Override
	public long reflectVertically  (long position) {
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
		for(int raw = PositionState.LOSS.VALUE; raw <= PositionState.WIN.VALUE; raw++)
			positions.put(PositionState.getStateOf(raw), new TreeSet<Long>());
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
	
	private void fillConnections(long position) {
		for(int[] intersection : connections) {
			intersection[0] = (int) position & 3;
			position >>= 2;
		}
	}
	
	private List<Map<PositionState, Set<Long>>> positionsTree = null;
	
	private int[][] connections                               = new int[][] {
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
	
	private long[]  verticalMillsMasks                        = new long[]  {
			0x04001001     ,
			0x40040010     ,
			0x00104040     ,
			0x01010400
	};
	
}
